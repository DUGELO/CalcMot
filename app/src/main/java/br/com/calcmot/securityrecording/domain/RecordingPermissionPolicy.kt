package br.com.calcmot.securityrecording.domain
object RecordingPermissionPolicy {
    enum class Resource { CAMERA, MICROPHONE, NOTIFICATIONS }
    fun next(camera: Boolean, microphone: Boolean, notifications: Boolean): Resource? = when {
        !camera -> Resource.CAMERA
        !microphone -> Resource.MICROPHONE
        !notifications -> Resource.NOTIFICATIONS
        else -> null
    }
    fun permanentlyDenied(requestedBefore: Boolean, showsRationale: Boolean) = requestedBefore && !showsRationale
}
