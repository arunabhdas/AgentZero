package app.agentzero.agentzeroapp.data.repository

import app.agentzero.agentzeroapp.data.model.RefreshToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository: MongoRepository<RefreshToken, ObjectId> {
    // Find by raw token
    fun findByUserIdAndToken(userId: ObjectId, token: String): RefreshToken?

    // Delete by raw token
    fun deleteByUserIdAndToken(userId: ObjectId, token: String)

    // Add method to delete all tokens for a user
    fun deleteAllByUserId(userId: ObjectId)
}