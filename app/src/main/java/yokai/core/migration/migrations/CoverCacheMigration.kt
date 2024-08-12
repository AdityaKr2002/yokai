package yokai.core.migration.migrations

import android.app.Application
import yokai.core.migration.Migration
import yokai.core.migration.MigrationContext
import java.io.File

/**
 * Move covers to external files dir.
 */
class CoverCacheMigration : Migration {
    override val version: Float = 19f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val oldDir = File(context.externalCacheDir, "cover_disk_cache")
        if (oldDir.exists()) {
            val destDir = context.getExternalFilesDir("covers")
            if (destDir != null) {
                oldDir.listFiles()?.forEach {
                    it.renameTo(File(destDir, it.name))
                }
            }
        }
        return true
    }
}
