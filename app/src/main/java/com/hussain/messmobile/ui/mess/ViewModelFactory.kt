package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hussain.messmobile.MessApplication

/** Creates the view models of the app and hands them the repository. */
object ViewModelFactory {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().repository) }
        initializer { MembersViewModel(app().repository) }
        initializer { MealsViewModel(app().repository) }
        initializer { DuesViewModel(app().repository) }
        initializer { ExpensesViewModel(app().repository) }
        initializer { AnnouncementsViewModel(app().repository) }
        initializer { BackupViewModel(app().backupManager) }
    }

    private fun CreationExtras.app(): MessApplication =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MessApplication
}
