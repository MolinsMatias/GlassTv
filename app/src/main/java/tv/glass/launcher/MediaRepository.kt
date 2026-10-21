package tv.glass.launcher

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Android binds this only after the user explicitly grants notification access. We read no notifications. */
class MediaAccessService : NotificationListenerService()

class MediaRepository(private val context: Context) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaAccessService::class.java)
    private val mutable = MutableStateFlow<MediaController?>(null)
    val session: StateFlow<MediaController?> = mutable
    val revision = MutableStateFlow(0)
    var allowed = false; private set
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { revision.value++ }
        override fun onPlaybackStateChanged(state: PlaybackState?) { revision.value++ }
        override fun onSessionDestroyed() { select(emptyList()); revision.value++ }
    }
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { select(it.orEmpty()) }
    private fun select(sessions: List<MediaController>) {
        mutable.value?.unregisterCallback(callback)
        mutable.value = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        mutable.value?.registerCallback(callback, Handler(Looper.getMainLooper()))
        revision.value++
    }
    fun start() {
        stop()
        try {
            select(manager.getActiveSessions(component)); allowed = true
            manager.addOnActiveSessionsChangedListener(listener, component, Handler(Looper.getMainLooper()))
        } catch (_: SecurityException) { allowed = false; select(emptyList()) }
    }
    fun stop() {
        manager.removeOnActiveSessionsChangedListener(listener)
        mutable.value?.unregisterCallback(callback); mutable.value = null
    }
}
