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
    version = 8,
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

        /** v5 → v6: allow a task to link to a project (JIRA-style progress). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `projectId` INTEGER")
            }
        }

        /**
         * v6 → v7: sync groundwork. Add a stable `uid` (globally-unique id) and an
         * `updatedAt` millis timestamp to every table, and backfill existing rows.
         * Both nullable, so plain ADD COLUMN suffices (no default clause to match).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf("events", "tasks", "focus_items", "projects", "notes")
                for (t in tables) {
                    db.execSQL("ALTER TABLE `$t` ADD COLUMN `uid` TEXT")
                    db.execSQL("ALTER TABLE `$t` ADD COLUMN `updatedAt` INTEGER")
                    // Backfill: a random 16-byte hex id, and "now" in millis.
                    db.execSQL("UPDATE `$t` SET `uid` = lower(hex(randomblob(16))) WHERE `uid` IS NULL")
                    db.execSQL("UPDATE `$t` SET `updatedAt` = (strftime('%s','now') * 1000) WHERE `updatedAt` IS NULL")
                }
            }
        }

        /**
         * v7 → v8: sync tombstones + uid-based foreign keys. Add `deleted` (NOT NULL
         * DEFAULT 0 — matches the entities' @ColumnInfo(defaultValue="0")) to the four
         * synced tables, and `*Uid` link columns backfilled from the existing Long FKs.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (t in listOf("events", "tasks", "projects", "notes")) {
                    db.execSQL("ALTER TABLE `$t` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `projectUid` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `projectUid` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `eventUid` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `taskUid` TEXT")
                // Backfill link uids from the current Long foreign keys.
                db.execSQL(
                    "UPDATE `tasks` SET `projectUid` = " +
                        "(SELECT `uid` FROM `projects` WHERE `projects`.`id` = `tasks`.`projectId`) " +
                        "WHERE `projectId` IS NOT NULL"
                )
                db.execSQL(
                    "UPDATE `notes` SET `projectUid` = " +
                        "(SELECT `uid` FROM `projects` WHERE `projects`.`id` = `notes`.`projectId`) " +
                        "WHERE `projectId` IS NOT NULL"
                )
                db.execSQL(
                    "UPDATE `notes` SET `eventUid` = " +
                        "(SELECT `uid` FROM `events` WHERE `events`.`id` = `notes`.`eventId`) " +
                        "WHERE `eventId` IS NOT NULL"
                )
                db.execSQL(
                    "UPDATE `notes` SET `taskUid` = " +
                        "(SELECT `uid` FROM `tasks` WHERE `tasks`.`id` = `notes`.`taskId`) " +
                        "WHERE `taskId` IS NOT NULL"
                )
            }
        }
    }
}
