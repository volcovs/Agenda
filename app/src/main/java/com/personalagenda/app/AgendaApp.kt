package com.personalagenda.app

import android.app.Application
import androidx.room.Room
import com.personalagenda.app.data.AgendaRepository
import com.personalagenda.app.data.SettingsStore
import com.personalagenda.app.data.db.AgendaDatabase

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
            )
            .build()
    }

    val repository: AgendaRepository by lazy { AgendaRepository(database.agendaDao()) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
}
