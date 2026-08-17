package com.personalagenda.app

import android.app.Application
import androidx.room.Room
import com.personalagenda.app.data.AgendaRepository
import com.personalagenda.app.data.SettingsStore
import com.personalagenda.app.data.db.AgendaDatabase
import com.personalagenda.app.data.sync.AuthManager
import com.personalagenda.app.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Owns the singleton database and repository. A ViewModel factory reads the
 * repository from here (see [com.personalagenda.app.ui.dashboard.DashboardViewModel]).
 */
class AgendaApp : Application() {

    private val database: AgendaDatabase by lazy {
        Room.databaseBuilder(this, AgendaDatabase::class.java, "agenda.db")
            .addMigrations(
                AgendaDatabase.MIGRATION_1_2,
                AgendaDatabase.MIGRATION_2_3,
                AgendaDatabase.MIGRATION_3_4,
                AgendaDatabase.MIGRATION_4_5,
                AgendaDatabase.MIGRATION_5_6,
                AgendaDatabase.MIGRATION_6_7,
                AgendaDatabase.MIGRATION_7_8,
            )
            .build()
    }

    /** App-lifetime scope for background sync. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: AgendaRepository by lazy { AgendaRepository(database.agendaDao()) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val authManager: AuthManager by lazy { AuthManager(this) }

    val syncManager: SyncManager by lazy { SyncManager(database.agendaDao(), authManager) }

    override fun onCreate() {
        super.onCreate()
        // Start/stop live Firestore sync as the signed-in user changes.
        syncManager.bindTo(appScope)
    }
}
