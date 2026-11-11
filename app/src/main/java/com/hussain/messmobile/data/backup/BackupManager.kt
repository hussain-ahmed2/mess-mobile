package com.hussain.messmobile.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.hussain.messmobile.data.local.MessDatabase
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Writes and reads backup files.
 *
 * A backup is a single JSON file that the user can store anywhere: on the device, on a memory
 * stick, in the cloud or in a chat. Restoring it replaces everything that is currently stored,
 * so the app is exactly where the user left it after an uninstall.
 */
class BackupManager(
    private val context: Context,
    private val database: MessDatabase,
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Reads a backup file without changing anything. */
    fun read(uri: Uri): BackupFile {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().decodeToString()
        } ?: throw IOException("The backup file could not be opened.")

        return json.decodeFromString(BackupFile.serializer(), text)
    }

    /** Writes the current content of the database to the given file. */
    suspend fun write(uri: Uri, appVersion: String): BackupFile {
        val backup = BackupFile(
            appVersion = appVersion,
            exportedAt = System.currentTimeMillis(),
            members = database.memberDao().getAll(),
            meals = database.mealDao().getAll(),
            mealEntries = database.mealEntryDao().getAll(),
            dues = database.duesDao().getAll(),
            payments = database.paymentDao().getAll(),
            expenses = database.expenseDao().getAll(),
            shoppingItems = database.shoppingDao().getAll(),
            announcements = database.announcementDao().getAll(),
        )

        val text = json.encodeToString(BackupFile.serializer(), backup)

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray())
            stream.flush()
        } ?: throw IOException("The backup file could not be written.")

        return backup
    }

    /**
     * Replaces the content of the database with the content of the backup.
     *
     * The whole restore runs in a single transaction, either everything is restored or nothing
     * is.
     */
    suspend fun restore(backup: BackupFile) {
        require(backup.formatVersion <= BackupFile.CURRENT_FORMAT_VERSION) {
            "The backup was created by a newer version of the app."
        }

        database.withTransaction {
            // Children first, the foreign keys of the parents point at them.
            database.mealEntryDao().deleteAll()
            database.paymentDao().deleteAll()
            database.duesDao().deleteAll()
            database.mealDao().deleteAll()
            database.expenseDao().deleteAll()
            database.shoppingDao().deleteAll()
            database.announcementDao().deleteAll()
            database.memberDao().deleteAll()

            database.memberDao().insertAll(backup.members)
            database.mealDao().insertAll(backup.meals)
            database.mealEntryDao().insertAll(backup.mealEntries)
            database.duesDao().insertAll(backup.dues)
            database.paymentDao().insertAll(backup.payments)
            database.expenseDao().insertAll(backup.expenses)
            database.shoppingDao().insertAll(backup.shoppingItems)
            database.announcementDao().insertAll(backup.announcements)
        }
    }
}
