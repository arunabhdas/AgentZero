package app.agentzero.agentzeroapp.controllers

import app.agentzero.agentzeroapp.data.model.Note
import app.agentzero.agentzeroapp.data.repository.NoteRepository
import app.agentzero.agentzeroapp.data.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant


@RestController
@RequestMapping("/notes")
class NoteController(
    private val repository: NoteRepository,
    private val userRepository: UserRepository,
    private val noteRepository: NoteRepository
) {
    data class NoteRequest(
        val id: String?,
        val title: String,
        val content: String,
        val color: String
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: String,
        val createdAt: Instant

    )
    // Upsert - Updates if id is provided, else insert and generate new id
    @PostMapping
    fun save(
        @RequestBody
        body: NoteRequest
    ): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String

        // Only handle noteId conversion, leave ownerId as string
        val noteId = body.id?.let {
            try { ObjectId(it) } catch (e: IllegalArgumentException) { ObjectId.get() }
        } ?: ObjectId.get()

        val note = repository.save(
            Note(
                id = noteId,
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId)
            )
        )
        return note.toResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String

        return repository.findByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        // Try to convert authentication principal to ObjectId for comparison
        try {
            if (note.ownerId.toHexString() == ownerId) {
                repository.deleteById(ObjectId(id))
            }
        } catch (e: IllegalArgumentException) {
            // If we can't convert the string to ObjectId, we can't match the owner
            throw IllegalArgumentException("Note not found or not owned by you")
        }
    }


    private fun Note.toResponse(): NoteController.NoteResponse {
        return NoteResponse(
            id = id.toHexString(),
            title = title,
            content = content,
            color = color,
            createdAt = createdAt
        )
    }
}
