package com.personalagenda.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EventEntity::class,
        TaskEntity::class,
        FocusEntity::class,
        ProjectEntity::class,
        NoteEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AgendaDatabase : RoomDatabase() {
    abstract fun agendaDao(): AgendaDao

    companion object {
        /** v1 → v2: make tasks.epochDay nullable so undated ("Someday") tasks are possible. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER, `text` TEXT NOT NULL, `done` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `tasks_new` (`id`, `epochDay`, `text`, `done`) " +
                        "SELECT `id`, `epochDay`, `text`, `done` FROM `tasks`"
                )
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
            }
        }

        /** v2 → v3: add richer project fields (all nullable, so plain ADD COLUMN suffices). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `description` TEXT")
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `status` TEXT")
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `nextAction` TEXT")
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `deadlineEpochDay` INTEGER")
            }
        }

        /** v3 → v4: allow a note to link to a project. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `projectId` INTEGER")
            }
        }

        /** v4 → v5: allow a note to also link to an event or a task. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `eventId` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `taskId` INTEGER")
            }
        }
    }
}
