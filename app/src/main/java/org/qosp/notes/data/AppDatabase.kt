package org.qosp.notes.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.qosp.notes.data.dao.*
import org.qosp.notes.data.model.*

@Database(
    entities = [
        NoteEntity::class,
        NoteTagJoin::class,
        Notebook::class,
        Tag::class,
        Reminder::class,
        IdMapping::class,
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract val noteDao: NoteDao
    abstract val notebookDao: NotebookDao
    abstract val noteTagDao: NoteTagDao
    abstract val tagDao: TagDao
    abstract val reminderDao: ReminderDao
    abstract val idMappingDao: IdMappingDao

    companion object {
        const val DB_NAME = "notes_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE notes ADD COLUMN isCompactPreview INTEGER NOT NULL DEFAULT (0)")
                }
            }
        }
    }
}
