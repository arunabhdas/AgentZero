package app.agentzero.agentzeroapp.model

import java.time.Instant

@Document("notes")
data class Note(
    val title: String,
    val content: String,
    val color: Long,
    val createdAt: Instant,
    @Id val id: ObjectId = ObjectId.get()
)
