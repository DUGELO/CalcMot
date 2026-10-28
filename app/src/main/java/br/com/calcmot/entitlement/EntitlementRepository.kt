package br.com.calcmot.entitlement

import br.com.calcmot.identity.IdentityAuthPort
import br.com.calcmot.identity.IdentityFailure
import br.com.calcmot.identity.IdentityOperationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** Explicit app entry/return operations only. Construction and StateFlow reads do no I/O.
 * No reference to Core/runtime/services. Call identityChanged on auth changes BEFORE UI consumption.
 * Production wiring/restrictive safe-entry policy intentionally waits for OQ-03/OQ-10.
 */
internal class EntitlementRepository(private val project: String, private val auth: IdentityAuthPort,
    private val remote: EntitlementRemote, private val store: EntitlementStore,
    private val clock: EntitlementClock, private val policy: OfflinePolicy? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val promoVerifier: PromoVerifier? = null) {
    private val lock = Mutex()
    private val mutable = MutableStateFlow(EntitlementState())
    val state: StateFlow<EntitlementState> = mutable.asStateFlow()
    private var cache: EntitlementCache? = null
    private var owner: EntitlementOwner? = null
    private var epoch = 0L
    private val timerScope = CoroutineScope(SupervisorJob() + io)
    private var expiryJob: Job? = null

    @Synchronized fun close() { timerScope.cancel() }

    /** Synchronous RAM clear prevents account B observing A while a request/disk load is in flight. */
    @Synchronized fun identityChanged() {
        epoch++
        owner = currentOwner()
        cache = null
        expiryJob?.cancel()
        mutable.value = EntitlementState(owner, EntitlementStatus.IDENTITY_CHANGED)
    }

    suspend fun restore() = withContext(io) { lock.withLock {
        val ticket = synchronizeIdentity()
        val expected = ticket.first ?: return@withLock
        try {
            val saved = store.read(expected)
            synchronized(this@EntitlementRepository) {
                if (!isCurrent(ticket)) return@synchronized
                if (saved != null) {
                    require(saved.owner == expected && saved.elapsed >= 0 && saved.policyVersion.isNotBlank())
                    require(saved.snapshot == EntitlementCodec.decode(saved.snapshotJson))
                    require(saved.promoVerifiedAt == null || (saved.snapshot.promo.state == "ACTIVE" &&
                        saved.promoVerifiedAt in 0..saved.snapshot.serverNow))
                }
                if (saved == null && cache != null) { publish(EntitlementStatus.STORAGE_UNAVAILABLE); return@synchronized }
                if (saved != null && cache != null) {
                    require(saved.snapshot.revision >= cache!!.snapshot.revision)
                    if (cache!!.snapshot.legacyCore) require(saved.snapshot.legacy == cache!!.snapshot.legacy)
                }
                cache = saved
                publish(if (saved == null) EntitlementStatus.EMPTY else EntitlementStatus.READY)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failure(ticket, EntitlementStatus.STORAGE_UNAVAILABLE) }
    } }

    suspend fun refresh() = withContext(io) { lock.withLock {
        val ticket = synchronizeIdentity()
        val expected = ticket.first ?: return@withLock
        if (cache == null) {
            try {
                val stored = store.read(expected)
                synchronized(this@EntitlementRepository) {
                    if (isCurrent(ticket) && stored != null) {
                        require(stored.owner == expected && stored.elapsed >= 0 && stored.policyVersion.isNotBlank())
                        require(stored.snapshot == EntitlementCodec.decode(stored.snapshotJson))
                        require(stored.promoVerifiedAt == null || (stored.snapshot.promo.state == "ACTIVE" &&
                            stored.promoVerifiedAt in 0..stored.snapshot.serverNow))
                        cache = stored
                    }
                }
            } catch (cancel: CancellationException) { throw cancel }
            // Corruption/key loss does not authorize access, but must not make authenticated
            // server recovery impossible. Only a valid own 200 can replace the unreadable file.
            catch (_: Exception) { failure(ticket, EntitlementStatus.STORAGE_UNAVAILABLE) }
        }
        val start = clock.elapsed()
        val boot = clock.boot()
        failure(ticket, EntitlementStatus.REFRESHING)
        try {
            val token = auth.firebaseIdToken(false)
            if (!isCurrent(ticket)) return@withLock
            val response = try { remote.get(token) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: IllegalArgumentException) { failure(ticket, EntitlementStatus.INCONSISTENT); return@withLock }
            if (!isCurrent(ticket)) return@withLock
            if (response.code != 200) {
                failure(ticket, when (response.code) {
                    401 -> EntitlementStatus.UNAUTHORIZED
                    503 -> EntitlementStatus.BACKEND_UNAVAILABLE
                    else -> EntitlementStatus.INCONSISTENT
                }); return@withLock
            }
            val snapshot = try { EntitlementCodec.decode(response.body) }
                catch (_: Exception) { failure(ticket, EntitlementStatus.INCONSISTENT); return@withLock }
            val previous = cache?.takeIf { it.owner == expected }?.snapshot
            if (previous != null && (snapshot.revision < previous.revision || snapshot.serverNow < previous.serverNow ||
                (previous.legacyCore && snapshot.legacy != previous.legacy) ||
                (snapshot.revision == previous.revision && !sameSources(previous, snapshot)))) {
                failure(ticket, EntitlementStatus.INCONSISTENT); return@withLock
            }
            if (clock.boot() != boot || clock.elapsed() < start) {
                failure(ticket, EntitlementStatus.INCONSISTENT); return@withLock
            }
            // Source-specific recency. Failure preserves only prior verification of the EXACT
            // same owner's promo; a fresh projection/GET never refreshes this timestamp.
            val priorPromoCheck = cache?.takeIf { it.owner == expected && it.snapshot.promo == snapshot.promo }?.promoVerifiedAt
            val receipt = if (snapshot.promo.state == "ACTIVE" && promoVerifier != null) {
                try { promoVerifier.verify(token, expected, snapshot.promo) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { null }
            } else null
            if (!isCurrent(ticket)) return@withLock
            val checkedPromo = receipt?.takeIf { it.owner == expected && it.source == snapshot.promo &&
                it.verifiedAt in 0..snapshot.serverNow && (priorPromoCheck == null || it.verifiedAt >= priorPromoCheck) }?.verifiedAt
                ?: priorPromoCheck
            val saved = EntitlementCache(expected, response.body, snapshot, start, boot,
                policy?.version ?: "OQ-03-PENDING", checkedPromo)
            // Persistence failure must not replace an older durable record in RAM with an uncommitted result.
            try { store.write(saved) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { failure(ticket, EntitlementStatus.STORAGE_UNAVAILABLE); return@withLock }
            synchronized(this@EntitlementRepository) {
                if (isCurrent(ticket)) { cache = saved; publish(EntitlementStatus.READY) }
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: IdentityOperationException) {
            failure(ticket, if (error.reason == IdentityFailure.OFFLINE) EntitlementStatus.OFFLINE
                else EntitlementStatus.SESSION_EXPIRED)
        } catch (_: IOException) { failure(ticket, EntitlementStatus.OFFLINE) }
        catch (_: Exception) { failure(ticket, EntitlementStatus.STORAGE_UNAVAILABLE) }
    } }

    /** Pure RAM evaluation: explicit timers/foreground owners can call this; no token/disk/network. */
    @Synchronized fun reevaluate() {
        if (currentOwner() != owner) { identityChanged(); return }
        publish(mutable.value.status)
    }

    /** Authorization consumers take a fresh RAM view, not a previously collected UI value. */
    @Synchronized fun current(): EntitlementState { reevaluate(); return mutable.value }

    @Synchronized private fun synchronizeIdentity(): Pair<EntitlementOwner?, Long> {
        if (currentOwner() != owner) identityChanged()
        if (owner == null) mutable.value = EntitlementState(status = EntitlementStatus.SESSION_EXPIRED)
        return owner to epoch
    }
    private fun currentOwner() = auth.currentSession()?.let { EntitlementOwner(project, it.uid) }
    @Synchronized private fun isCurrent(ticket: Pair<EntitlementOwner?, Long>): Boolean {
        if (currentOwner() != owner) { identityChanged(); return false }
        return ticket.first == owner && ticket.second == epoch
    }
    @Synchronized private fun failure(ticket: Pair<EntitlementOwner?, Long>, status: EntitlementStatus) {
        if (isCurrent(ticket)) publish(status)
        else if (currentOwner() != owner) identityChanged()
    }
    private fun sameSources(a: EntitlementSnapshot, b: EntitlementSnapshot) = a.checkedAt == b.checkedAt &&
        a.subscriptionRequired == b.subscriptionRequired && a.legacy == b.legacy && a.play == b.play &&
        a.promo == b.promo && a.review == b.review &&
        (!a.proAllowed || !b.proAllowed || a.features == b.features)

    private fun publish(status: EntitlementStatus) {
        expiryJob?.cancel()
        val saved = cache?.takeIf { it.owner == owner }
        val snapshot = saved?.snapshot
        if (snapshot == null) { mutable.value = EntitlementState(owner, status); return }
        val elapsed = clock.elapsed()
        val delta = elapsed - saved.elapsed
        val trusted = saved.boot != null && saved.boot == clock.boot() && delta >= 0 &&
            snapshot.serverNow <= Long.MAX_VALUE - delta
        val now = if (trusted) snapshot.serverNow + delta else null
        fun temporal(present: Boolean, start: Long?, until: Long?, verifiedAt: Long?, maxAge: Long?): TemporalStatus {
            if (!present) return TemporalStatus.NO_SOURCE
            if (now == null) return TemporalStatus.REVALIDATION_REQUIRED
            if (until == null || now >= until) return TemporalStatus.EXPIRED
            if (start != null && now < start) return TemporalStatus.REVALIDATION_REQUIRED
            if (policy == null) return TemporalStatus.POLICY_REQUIRED
            if (saved.policyVersion != policy.version || verifiedAt == null || now < verifiedAt ||
                maxAge == null || now - verifiedAt >= maxAge) return TemporalStatus.REVALIDATION_REQUIRED
            return TemporalStatus.VALID
        }
        val play = temporal(snapshot.play.state in EntitlementCodec.activePlay && snapshot.play.phase != "UNKNOWN",
            null, snapshot.play.until, snapshot.play.checkedAt, policy?.playAge(snapshot.play.phase))
        val promo = temporal(snapshot.promo.state == "ACTIVE", snapshot.promo.start, snapshot.promo.until,
            saved.promoVerifiedAt, policy?.promoMaxAgeMs)
        // Explicit owner decision: no provisional review access, even with a server date.
        val review = if (snapshot.legacy.state == "UNDER_REVIEW") TemporalStatus.DISABLED else TemporalStatus.NO_SOURCE
        val pro = play == TemporalStatus.VALID || promo == TemporalStatus.VALID
        mutable.value = EntitlementState(owner, status, snapshot.legacyCore, pro,
            if (pro) snapshot.features else emptySet(), play, promo, review, snapshot.play.phase, snapshot.revision,
            snapshot.play.state, snapshot.legacy.state, snapshot.nextAction, snapshot.review.claim)
        // Wake only at known source/policy boundaries. This job does RAM evaluation only;
        // no polling, network, token refresh or commercial UI during monitoring.
        if (now != null) {
            val deadlines = listOfNotNull(snapshot.play.until, snapshot.promo.start, snapshot.promo.until, snapshot.review.until,
                deadline(snapshot.play.checkedAt, policy?.playAge(snapshot.play.phase)),
                deadline(saved.promoVerifiedAt, policy?.promoMaxAgeMs))
            deadlines.filter { it > now }.minOrNull()?.let { next ->
                expiryJob = timerScope.launch { delay(next - now); reevaluate() }
            }
        }
    }

    private fun deadline(base: Long?, age: Long?): Long? =
        if (base != null && age != null && base <= Long.MAX_VALUE - age) base + age else null
}
