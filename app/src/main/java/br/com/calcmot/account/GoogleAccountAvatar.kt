package br.com.calcmot.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import java.net.URI

/** UI-only metadata, never an identity credential or entitlement source. Not persisted. */
internal data class AccountProfilePhoto(val owner: String, val url: String)

internal fun accountProfilePhoto(owner: String, url: String?): AccountProfilePhoto? {
    if (owner.isBlank() || url.isNullOrBlank()) return null
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null ||
        uri.fragment != null || uri.port !in listOf(-1, 443)) return null
    return AccountProfilePhoto(owner, url)
}

@Composable internal fun GoogleAccountAvatar(photo: AccountProfilePhoto?,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current)) {
    val context = LocalContext.current
    Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
        // Dispose the previous painter immediately, even when two identities share a URL.
        key(photo?.owner, photo?.url) {
            var loaded by remember { mutableStateOf(false) }
            val request = remember(photo, context) { photo?.let {
                ImageRequest.Builder(context).data(it.url)
                    .diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build()
            } }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!loaded) Icon(Icons.Outlined.Person, null,
                    Modifier.size(28.dp).testTag("account_avatar_fallback"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (request != null) AsyncImage(model = request, imageLoader = imageLoader,
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                        .then(if (loaded) Modifier.testTag("account_avatar_photo") else Modifier),
                    onLoading = { loaded = false }, onSuccess = { loaded = true }, onError = { loaded = false })
            }
        }
    }
}
