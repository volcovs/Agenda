package com.personalagenda.app.data.sync

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.personalagenda.app.data.db.AgendaDao
import com.personalagenda.app.data.db.EventEntity
import com.personalagenda.app.data.db.NoteEntity
import com.personalagenda.app.data.db.ProjectEntity
import com.personalagenda.app.data.db.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Automatic, live two-way sync between Room and Cloud Firestore, scoped per
 * signed-in user (`users/{uid}/{events|tasks|projects|notes}/{rowUid}`).
 *
 * - **Push**: watches the local tables (including tombstones) and uploads every
 *   row whose `updatedAt` is newer than the per-collection watermark.
 * - **Pull**: a snapshot listener per collection applies remote rows with
 *   last-write-wins on `updatedAt`, keyed by the stable `uid`, then re-resolves
 *   the local Long foreign keys from the link uids.
 *
 * Echoes are absorbed by the watermark + LWW: applying a pulled row advances the
 * push watermark past it, and equal/older `updatedAt` values are skipped, so a
 * change can't ping-pong. Foreign keys travel as the target's `uid`, so links
 * survive the local ids differing between devices.
 */
class SyncManager(
    private val dao: AgendaDao,
    private val auth: AuthManager,
) {
    private val db = FirebaseFirestore.getInstance()
    private var scope: CoroutineScope? = null
    private val registrations = mutableListOf<ListenerRegistration>()
    private val mutex = Mutex()
    private val pushWatermark = mutableMapOf<String, Long>()

    /** Start/stop syncing as the signed-in user changes. Call once at app start. */
    fun bindTo(appScope: CoroutineScope) {
        appScope.launch {
            auth.user.collect { user ->
                val uid = user?.uid
                if (uid != null) start(uid) else stop()
            }
        }
    }

    private fun col(uid: String, name: String): CollectionReference =
        db.collection("users").document(uid).collection(name)

    private fun start(uid: String) {
        stop()
        pushWatermark.clear()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s

        // PUSH — react to any local change.
        s.launch { dao.eventsSyncFlow().collect { pushEvents(uid, it) } }
        s.launch { dao.tasksSyncFlow().collect { pushTasks(uid, it) } }
        s.launch { dao.projectsSyncFlow().collect { pushProjects(uid, it) } }
        s.launch { dao.notesSyncFlow().collect { pushNotes(uid, it) } }

        // PULL — react to remote changes.
        registrations += col(uid, EVENTS).addSnapshotListener { snap, e ->
            if (e != null || snap == null) { logErr("events", e); return@addSnapshotListener }
            s.launch { pullEvents(snap.documents); relink() }
        }
        registrations += col(uid, PROJECTS).addSnapshotListener { snap, e ->
            if (e != null || snap == null) { logErr("projects", e); return@addSnapshotListener }
            s.launch { pullProjects(snap.documents); relink() }
        }
        registrations += col(uid, TASKS).addSnapshotListener { snap, e ->
            if (e != null || snap == null) { logErr("tasks", e); return@addSnapshotListener }
            s.launch { pullTasks(snap.documents); relink() }
        }
        registrations += col(uid, NOTES).addSnapshotListener { snap, e ->
            if (e != null || snap == null) { logErr("notes", e); return@addSnapshotListener }
            s.launch { pullNotes(snap.documents); relink() }
        }
    }

    private fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
        scope?.cancel()
        scope = null
    }

    private fun logErr(what: String, e: Exception?) { if (e != null) Log.w(TAG, "listener $what failed", e) }

    // --- Push (local -> Firestore) ---

    private suspend fun pushEvents(uid: String, rows: List<EventEntity>) =
        pushAll(EVENTS, col(uid, EVENTS), rows.mapNotNull { r -> r.uid?.let { SyncDoc(it, r.updatedAt ?: 0L, eventData(r)) } })

    private suspend fun pushTasks(uid: String, rows: List<TaskEntity>) =
        pushAll(TASKS, col(uid, TASKS), rows.mapNotNull { r -> r.uid?.let { SyncDoc(it, r.updatedAt ?: 0L, taskData(r)) } })

    private suspend fun pushProjects(uid: String, rows: List<ProjectEntity>) =
        pushAll(PROJECTS, col(uid, PROJECTS), rows.mapNotNull { r -> r.uid?.let { SyncDoc(it, r.updatedAt ?: 0L, projectData(r)) } })

    private suspend fun pushNotes(uid: String, rows: List<NoteEntity>) =
        pushAll(NOTES, col(uid, NOTES), rows.mapNotNull { r -> r.uid?.let { SyncDoc(it, r.updatedAt ?: 0L, noteData(r)) } })

    private class SyncDoc(val uid: String, val updatedAt: Long, val data: Map<String, Any?>)

    private suspend fun pushAll(name: String, col: CollectionReference, docs: List<SyncDoc>) = mutex.withLock {
        val wm = pushWatermark[name] ?: 0L
        val fresh = docs.filter { it.updatedAt > wm }
        if (fresh.isEmpty()) return@withLock
        try {
            // Commit in batches (Firestore caps a batch at 500 writes).
            fresh.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.set(col.document(it.uid), it.data, SetOptions.merge()) }
                batch.commit().await()
            }
            pushWatermark[name] = maxOf(wm, fresh.maxOf { it.updatedAt })
        } catch (e: Exception) {
            Log.w(TAG, "push $name failed", e) // keep watermark; retry on next change
        }
    }

    // --- Pull (Firestore -> local), last-write-wins ---

    private suspend fun pullEvents(docs: List<DocumentSnapshot>) = mutex.withLock {
        var maxAt = pushWatermark[EVENTS] ?: 0L
        for (d in docs) {
            val remoteAt = d.getLong("updatedAt") ?: 0L
            val existing = dao.eventByUid(d.id)
            if (existing != null && (existing.updatedAt ?: 0L) >= remoteAt) continue
            val row = EventEntity(
                id = existing?.id ?: 0L,
                epochDay = d.getLong("epochDay") ?: 0L,
                startMinute = (d.getLong("startMinute") ?: 0L).toInt(),
                endMinute = (d.getLong("endMinute") ?: 0L).toInt(),
                title = d.getString("title") ?: "",
                subtitle = d.getString("subtitle"),
                category = d.getString("category") ?: "PERSONAL",
                location = d.getString("location"),
                uid = d.id,
                updatedAt = remoteAt,
                deleted = d.getBoolean("deleted") ?: false,
            )
            if (existing != null) dao.updateEventRow(row) else dao.insertEvent(row)
            if (remoteAt > maxAt) maxAt = remoteAt
        }
        pushWatermark[EVENTS] = maxAt
    }

    private suspend fun pullProjects(docs: List<DocumentSnapshot>) = mutex.withLock {
        var maxAt = pushWatermark[PROJECTS] ?: 0L
        for (d in docs) {
            val remoteAt = d.getLong("updatedAt") ?: 0L
            val existing = dao.projectByUid(d.id)
            if (existing != null && (existing.updatedAt ?: 0L) >= remoteAt) continue
            val row = ProjectEntity(
                id = existing?.id ?: 0L,
                name = d.getString("name") ?: "",
                progress = (d.getDouble("progress") ?: 0.0).toFloat(),
                category = d.getString("category") ?: "PROJECTS",
                description = d.getString("description"),
                status = d.getString("status"),
                nextAction = d.getString("nextAction"),
                deadlineEpochDay = d.getLong("deadlineEpochDay"),
                uid = d.id,
                updatedAt = remoteAt,
                deleted = d.getBoolean("deleted") ?: false,
            )
            if (existing != null) dao.updateProjectRow(row) else dao.insertProject(row)
            if (remoteAt > maxAt) maxAt = remoteAt
        }
        pushWatermark[PROJECTS] = maxAt
    }

    private suspend fun pullTasks(docs: List<DocumentSnapshot>) = mutex.withLock {
        var maxAt = pushWatermark[TASKS] ?: 0L
        for (d in docs) {
            val remoteAt = d.getLong("updatedAt") ?: 0L
            val existing = dao.taskByUid(d.id)
            if (existing != null && (existing.updatedAt ?: 0L) >= remoteAt) continue
            val row = TaskEntity(
                id = existing?.id ?: 0L,
                epochDay = d.getLong("epochDay"),
                text = d.getString("text") ?: "",
                done = d.getBoolean("done") ?: false,
                projectId = existing?.projectId, // resolved by relink() after the batch
                uid = d.id,
                updatedAt = remoteAt,
                deleted = d.getBoolean("deleted") ?: false,
                projectUid = d.getString("projectUid"),
            )
            if (existing != null) dao.updateTaskRow(row) else dao.insertTask(row)
            if (remoteAt > maxAt) maxAt = remoteAt
        }
        pushWatermark[TASKS] = maxAt
    }

    private suspend fun pullNotes(docs: List<DocumentSnapshot>) = mutex.withLock {
        var maxAt = pushWatermark[NOTES] ?: 0L
        for (d in docs) {
            val remoteAt = d.getLong("updatedAt") ?: 0L
            val existing = dao.noteByUid(d.id)
            if (existing != null && (existing.updatedAt ?: 0L) >= remoteAt) continue
            val row = NoteEntity(
                id = existing?.id ?: 0L,
                epochDay = d.getLong("epochDay") ?: 0L,
                text = d.getString("text") ?: "",
                minute = (d.getLong("minute") ?: 0L).toInt(),
                projectId = existing?.projectId,
                eventId = existing?.eventId,
                taskId = existing?.taskId,
                uid = d.id,
                updatedAt = remoteAt,
                deleted = d.getBoolean("deleted") ?: false,
                projectUid = d.getString("projectUid"),
                eventUid = d.getString("eventUid"),
                taskUid = d.getString("taskUid"),
            )
            if (existing != null) dao.updateNoteRow(row) else dao.insertNote(row)
            if (remoteAt > maxAt) maxAt = remoteAt
        }
        pushWatermark[NOTES] = maxAt
    }

    /** Re-resolve local Long foreign keys from the synced link uids. */
    private suspend fun relink() = mutex.withLock {
        dao.relinkTaskProjects()
        dao.relinkNotes()
    }

    // --- Entity -> Firestore document ---

    private fun eventData(r: EventEntity) = mapOf(
        "epochDay" to r.epochDay,
        "startMinute" to r.startMinute,
        "endMinute" to r.endMinute,
        "title" to r.title,
        "subtitle" to r.subtitle,
        "category" to r.category,
        "location" to r.location,
        "updatedAt" to (r.updatedAt ?: 0L),
        "deleted" to r.deleted,
    )

    private fun taskData(r: TaskEntity) = mapOf(
        "epochDay" to r.epochDay,
        "text" to r.text,
        "done" to r.done,
        "projectUid" to r.projectUid,
        "updatedAt" to (r.updatedAt ?: 0L),
        "deleted" to r.deleted,
    )

    private fun projectData(r: ProjectEntity) = mapOf(
        "name" to r.name,
        "progress" to r.progress,
        "category" to r.category,
        "description" to r.description,
        "status" to r.status,
        "nextAction" to r.nextAction,
        "deadlineEpochDay" to r.deadlineEpochDay,
        "updatedAt" to (r.updatedAt ?: 0L),
        "deleted" to r.deleted,
    )

    private fun noteData(r: NoteEntity) = mapOf(
        "epochDay" to r.epochDay,
        "text" to r.text,
        "minute" to r.minute,
        "projectUid" to r.projectUid,
        "eventUid" to r.eventUid,
        "taskUid" to r.taskUid,
        "updatedAt" to (r.updatedAt ?: 0L),
        "deleted" to r.deleted,
    )

    private companion object {
        const val TAG = "SyncManager"
        const val EVENTS = "events"
        const val TASKS = "tasks"
        const val PROJECTS = "projects"
        const val NOTES = "notes"
    }
}
