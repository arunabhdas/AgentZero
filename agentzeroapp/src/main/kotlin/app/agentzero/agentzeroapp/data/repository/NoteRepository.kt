package app.agentzero.agentzeroapp.data.repository

import app.agentzero.agentzeroapp.data.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository:MongoRepository<Note, ObjectId> {

    fun findByOwnerId(ownerId: ObjectId): List<Note>


}
