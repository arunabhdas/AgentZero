package app.agentzero.agentzeroapp.security

import app.agentzero.agentzeroapp.data.model.RefreshToken
import app.agentzero.agentzeroapp.data.model.User
import app.agentzero.agentzeroapp.data.repository.RefreshTokenRepository
import app.agentzero.agentzeroapp.data.repository.UserRepository
import org.bson.types.ObjectId
import java.security.MessageDigest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Ref
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String): User {
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException("Invalid credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials")
        }

        // Delete all previous refresh tokens for this user
        refreshTokenRepository.deleteAllByUserId(user.id)

        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(
            userId =user.id,
            rawRefreshToken = newRefreshToken
        )

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token.")
        }

        // Assumes getUserIdFromToken also validates expiry
        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            IllegalArgumentException("Invalid refresh token.")
        }

        // Find the raw token in the repository
        refreshTokenRepository.findByUserIdAndToken(user.id, refreshToken)
            ?: throw IllegalArgumentException("Refresh token not recognized (maybe used or expired")

        // Delete the used raw token
        refreshTokenRepository.deleteByUserIdAndToken(user.id, refreshToken)

        // Generate new tokens
        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        // Store the new raw refresh token
        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )

    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                token = rawRefreshToken
            )
        )
    }

}