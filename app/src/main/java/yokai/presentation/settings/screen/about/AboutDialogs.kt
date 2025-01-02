package yokai.presentation.settings.screen.about

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.textview.MaterialTextView
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.ui.more.parseReleaseNotes
import java.io.Serializable
import yokai.domain.DialogHostState
import yokai.i18n.MR

data class NewUpdateData(
    val body: String,
    val url: String,
    val isBeta: Boolean?,
) : Serializable

suspend fun DialogHostState.awaitNewUpdateDialog(
    data: NewUpdateData,
    onDismiss: () -> Unit = {},
): Unit = dialog { cont ->
    val context = LocalContext.current
    val appContext = context.applicationContext

    val isOnA12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            cont.cancel()
        },
        title = {
            Text(
                text = stringResource(
                    if (data.isBeta == true) {
                        MR.strings.new_beta_version_available
                    } else {
                        MR.strings.new_version_available
                    }
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                AppDownloadInstallJob.start(appContext, data.url, true)
                onDismiss()
                cont.cancel()
            }) {
                Text(text = stringResource(if (isOnA12) MR.strings.update else MR.strings.download))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    cont.cancel()
                }
            ) {
                Text(text = stringResource(MR.strings.ignore))
            }
        },
        text = { MarkdownText(data.body) }
    )
}

@Composable
private fun MarkdownText(text: String) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            MaterialTextView(it)
        },
        update = {
            it.text = context.parseReleaseNotes(text)
        },
    )
}
