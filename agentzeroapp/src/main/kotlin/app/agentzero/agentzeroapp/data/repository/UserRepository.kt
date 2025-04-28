package app.agentzero.agentzeroapp.data.repository

import app.agentzero.agentzeroapp.data.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
}