package com.messmate.bd.data.remote

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive v3 helper – creating the mess sheet and sharing it.
 *
 * Because the app only holds the `drive.file` scope, it may only touch files
 * that the user explicitly picked or that the app itself created. Everything in
 * this class therefore stays inside that boundary.
 */
@Singleton
class DriveGateway @Inject constructor(
    private val apiFactory: GoogleApiFactory,
) {

    private val service: Drive by lazy { apiFactory.createDriveService() }

    /**
     * Creates a new Google Spreadsheet.
     *
     * Creating the file through the **Drive** API (instead of the Sheets API)
     * guarantees that the file belongs to the app – with `drive.file` the app
     * then owns full read/write access to it.
     */
    suspend fun createSpreadsheet(name: String, description: String? = null): DriveFileInfo =
        withContext(Dispatchers.IO) {
            val file = File()
                .setName(name)
                .setMimeType(MIME_TYPE_SPREADSHEET)
                .setDescription(description ?: "Mess managed with MessMate BD")

            val created = service.files().create(file)
                .setFields("id,name,mimeType,webViewLink")
                .execute()

            DriveFileInfo(
                id = created.id,
                name = created.name,
                mimeType = created.mimeType,
                webViewLink = created.webViewLink ?: "https://docs.google.com/spreadsheets/d/${created.id}/edit",
            )
        }

    /** Metadata of a single file – `null` when the app has no access to it. */
    suspend fun getFile(fileId: String): DriveFileInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val file = service.files().get(fileId)
                .setFields("id,name,mimeType,webViewLink")
                .execute()
            DriveFileInfo(
                id = file.id,
                name = file.name,
                mimeType = file.mimeType,
                webViewLink = file.webViewLink ?: "https://docs.google.com/spreadsheets/d/${file.id}/edit",
            )
        }.getOrNull()
    }

    /** Renames a file – used when a month is closed ("… – 2026-11 (Closed)"). */
    suspend fun renameFile(fileId: String, newName: String): Unit = withContext(Dispatchers.IO) {
        val file = File().setName(newName)
        service.files().update(fileId, file).setFields("id,name").execute()
    }

    /**
     * Shares the spreadsheet with a list of Gmail addresses (write access).
     *
     * This is how a new member gets into the mess: the manager adds the Gmail
     * address, the file is shared with it and the member can then open the
     * sheet from "Shared with me" – which also grants the app the `drive.file`
     * permission for that file.
     *
     * @return the addresses that were shared successfully and the ones that
     *         failed (with the reason).
     */
    suspend fun shareWithUsers(
        fileId: String,
        emails: List<String>,
        sendNotification: Boolean = true,
    ): ShareOutcome = withContext(Dispatchers.IO) {
        val granted = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()

        emails.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { email ->
            runCatching {
                val permission = Permission()
                    .setType(TYPE_USER)
                    .setRole(ROLE_WRITER)
                    .setEmailAddress(email)

                service.permissions().create(fileId, permission)
                    .setSendNotificationEmail(sendNotification)
                    .setSupportsAllDrives(true)
                    .execute()
            }.onSuccess {
                granted += email
            }.onFailure { error ->
                failed += email to (error.message ?: "Unknown error")
            }
        }

        ShareOutcome(granted = granted, failed = failed)
    }

    /** Removes a member's access (used when a member leaves the mess). */
    suspend fun revokeAccess(fileId: String, email: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val permission = Permission().setType(TYPE_USER).setRole(ROLE_WRITER).setEmailAddress(email)
            val created = service.permissions().create(fileId, permission)
                .setTransferOwnership(false)
                .execute()
            service.permissions().delete(fileId, created.id).execute()
            true
        }.getOrDefault(false)
    }

    /**
     * Moves a file to the trash – used to clean up a sheet that could not be
     * provisioned completely.
     */
    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            service.files().delete(fileId).execute()
            true
        }.getOrDefault(false)
    }

    /**
     * Lists the spreadsheets the app is allowed to see (files it created or the
     * user picked). Used by the "join a mess" screen to offer a shortlist.
     */
    suspend fun listAccessibleSpreadsheets(): List<DriveFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            service.files().list()
                .setQ("mimeType='$MIME_TYPE_SPREADSHEET' and trashed=false")
                .setOrderBy("modifiedTime desc")
                .setPageSize(50)
                .setFields("files(id,name,mimeType,webViewLink)")
                .execute()
                .getFiles()
                ?.map {
                    DriveFileInfo(
                        id = it.id,
                        name = it.name,
                        mimeType = it.mimeType,
                        webViewLink = it.webViewLink
                            ?: "https://docs.google.com/spreadsheets/d/${it.id}/edit",
                    )
                }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        const val MIME_TYPE_SPREADSHEET = "application/vnd.google-apps.spreadsheet"
        private const val TYPE_USER = "user"
        private const val ROLE_WRITER = "writer"
    }
}

/** Metadata of a Drive file. */
data class DriveFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val webViewLink: String,
)

/** Result of a share operation. */
data class ShareOutcome(
    val granted: List<String>,
    val failed: List<Pair<String, String>>,
) {
    val isCompleteSuccess: Boolean get() = failed.isEmpty()
}
