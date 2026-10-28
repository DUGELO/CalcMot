package br.com.calcmot.account

import br.com.calcmot.entitlement.*
import br.com.calcmot.identity.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal data class LegacyRequestView(val claimId: String, val state: String)
internal interface LegacyRequests {
    val available: Boolean
    suspend fun submit(): LegacyRequestView
    suspend fun resume(): LegacyRequestView? = null
}
internal enum class AccountNotice { NONE, CANCELED, OFFLINE, SESSION_EXPIRED, UNAUTHORIZED,
    UNAVAILABLE, CONFIGURATION, COLLISION, LOGGED_OUT, REVIEW_UNAVAILABLE, REVIEW_RECEIVED, REVIEW_CONFLICT, REVIEW_DEFERRED }
internal data class AccountState(val signedIn: Boolean = false, val linked: Boolean = false,
    val busy: Boolean = false, val notice: AccountNotice = AccountNotice.NONE,
    val access: EntitlementState = EntitlementState(), val request: LegacyRequestView? = null,
    val consultationAvailable: Boolean = false, val authAvailable: Boolean = true,
    val displayName: String? = null, val email: String? = null, val recoveryAvailable: Boolean = false,
    val profilePhoto: AccountProfilePhoto? = null, val sessionGeneration: Long = 0,
    val linkedProviders: Set<String> = emptySet())

/** Account-scoped presentation only. Never grants rights, merges UIDs, stops Core or opens billing. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class AccountViewModel(private val identity: IdentityCoordinator,
    private val signOut: suspend () -> Unit, private val repository: EntitlementRepository?,
    private val requests: LegacyRequests, private val close: () -> Unit = {},
    providedScope: CoroutineScope? = null) : ViewModel() {
    private val scope = providedScope ?: viewModelScope
    override fun onCleared() { close(); super.onCleared() }
    private val mutable = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = mutable.asStateFlow()
    private val operation = Mutex()
    private var currentUid = identity.session.value?.uid
    private var sessionGeneration = identity.revision.value
    private var request: LegacyRequestView? = null
    init {
        scope.launch {
            // Flow's switchMap: a new session cancels the previous rights observation.
            // In-flight operations still require ownership/generation guards below.
            combine(identity.session, identity.revision) { session, revision -> session to revision }.flatMapLatest {
                repository?.state ?: flowOf(EntitlementState())
            }.buffer(0).collect {
                identity.refreshSession()
                publish()
            }
        }
        publish()
    }
    private fun publish(busy: Boolean = mutable.value.busy, notice: AccountNotice = mutable.value.notice) {
        val session = identity.session.value
        val changedOwner = currentUid != session?.uid || sessionGeneration != identity.revision.value
        if (changedOwner) { currentUid = session?.uid; request = null; sessionGeneration = identity.revision.value }
        val access = repository?.current()?.takeIf { it.owner?.uid == session?.uid && session != null }
            ?: EntitlementState()
        mutable.value = AccountState(session != null, session?.identityLinked == true, busy,
            if (changedOwner) AccountNotice.NONE else notice,
            access, request.takeIf { currentUid == session?.uid && session != null }, repository != null,
            displayName = session?.displayName, email = session?.email, recoveryAvailable = requests.available,
            profilePhoto = session?.let { accountProfilePhoto(it.uid, it.googlePhotoUrl) }, sessionGeneration = sessionGeneration,
            linkedProviders = session?.linkedProviders?.toSet().orEmpty())
    }
    private suspend fun perform(block: suspend () -> Unit) {
        if (!operation.tryLock()) return
        publish(busy = true, notice = AccountNotice.NONE)
        val uid = identity.session.value?.uid
        val generation = sessionGeneration
        try { block() }
        catch (cancel: CancellationException) { throw cancel }
        catch (e: LegacyRequestException) { if (sameOwner(uid, generation)) publish(notice = e.notice) }
        catch (e: IdentityOperationException) { if (sameOwner(uid, generation)) publish(notice = e.reason.toNotice()) }
        catch (_: java.io.IOException) { if (sameOwner(uid, generation)) publish(notice = AccountNotice.OFFLINE) }
        catch (_: Exception) { if (sameOwner(uid, generation)) publish(notice = AccountNotice.UNAVAILABLE) }
        finally { publish(busy = false); operation.unlock() }
    }
    private fun sameOwner(uid: String?, generation: Long) =
        identity.session.value?.uid == uid && identity.revision.value == generation
    suspend fun open() = refresh()
    suspend fun refresh() = perform {
        identity.refreshSession()
        if (identity.session.value == null) return@perform
        if (repository == null) { publish(notice = AccountNotice.CONFIGURATION); return@perform }
        val uid = identity.session.value?.uid
        val revision = identity.revision.value
        repository.restore(); repository.refresh()
        if (requests.available && sameOwner(uid, revision)) {
            val resumed = requests.resume()
            if (sameOwner(uid, revision) && resumed != null) request = resumed
        }
        publish()
    }
    suspend fun login(signIn: suspend () -> IdentityResult) = perform {
        identity.refreshSession(); publish()
        val beforeUid = identity.session.value?.uid
        val generation = sessionGeneration
        when (val result = signIn()) {
            is IdentityResult.Failure -> if (sameOwner(beforeUid, generation)) publish(notice = result.reason.toNotice())
            is IdentityResult.Success -> {
                identity.refreshSession()
                publish() // Account for the current owner even before the Flow collector resumes.
                val stamp = result.operation
                val generationMatches = stamp != null && stamp.startedAtRevision == generation &&
                    stamp.completedAtRevision == identity.revision.value
                if (identity.session.value?.uid != result.session.uid ||
                    !generationMatches) {
                    publish(); return@perform
                }
                if (repository == null) { publish(notice = AccountNotice.CONFIGURATION); return@perform }
                repository?.restore(); repository?.refresh(); publish()
            }
        }
    }
    suspend fun logout() = perform {
        // Local Auth signOut happens before credential-provider cleanup; no benefits transfer.
        try { signOut() } finally {
            identity.refreshSession(); repository?.identityChanged()
            request = null
            publish() // Invalidate the previous session's local UI context before feedback.
            publish(notice = if (identity.session.value == null) AccountNotice.LOGGED_OUT else AccountNotice.UNAVAILABLE)
        }
    }
    suspend fun requestRecognition() = perform {
        val before = identity.session.value?.takeIf { it.identityLinked }
            ?: run { publish(notice = AccountNotice.SESSION_EXPIRED); return@perform }
        if (!requests.available) { publish(notice = AccountNotice.REVIEW_UNAVAILABLE); return@perform }
        val generation = sessionGeneration
        val view = requests.submit()
        identity.refreshSession()
        if (!sameOwner(before.uid, generation)) { publish(); return@perform }
        currentUid = before.uid; request = view
        // GRANTED on a claim is NOT shown as a recovered entitlement until S07 confirms it.
        repository?.refresh()
        if (sameOwner(before.uid, generation)) publish(notice = AccountNotice.REVIEW_RECEIVED) else publish()
    }
    private fun IdentityFailure.toNotice() = when (this) {
        IdentityFailure.CANCELED -> AccountNotice.CANCELED
        IdentityFailure.OFFLINE -> AccountNotice.OFFLINE
        IdentityFailure.SESSION_EXPIRED -> AccountNotice.SESSION_EXPIRED
        IdentityFailure.COLLISION -> AccountNotice.COLLISION
        IdentityFailure.CONFIGURATION -> AccountNotice.CONFIGURATION
        IdentityFailure.UNAVAILABLE -> AccountNotice.UNAVAILABLE
    }
}
