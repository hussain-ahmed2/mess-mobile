package com.hussain.messmobile

import android.app.Application
import com.hussain.messmobile.data.backup.BackupManager
import com.hussain.messmobile.data.local.MessDatabase
import com.hussain.messmobile.data.repository.MessRepository

/** Holds the database, the repository and the backup manager of the app. */
class MessApplication : Application() {

    val database: MessDatabase by lazy { MessDatabase.build(this) }

    val repository: MessRepository by lazy { MessRepository(database) }

    val backupManager: BackupManager by lazy { BackupManager(this, database) }
}
