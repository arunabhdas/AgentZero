package app.agentzero.agentzeroapp.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.print.Book
import java.time.Instant.now
import java.util.Base64
import java.util.Date
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.exp

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {

    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    private val accessTokenValidityMs = 15L * 60L * 1000L // 15 minutes * 60 seconds * 1000 milliseconds

    val refreshTokenValidityMs = 30L * 24 * 60 * 60 * 1000L // 30 days * 24 hours * 60 minutes * 60 seconds * 1000 milliseconds

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "access",
            expiry = accessTokenValidityMs
        )
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(
            userId = userId,
            type = "refresh",
            expiry = refreshTokenValidityMs
        )
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    // Authorization: Bearer <token>
    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw IllegalArgumentException("Invalid token.")
        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken =  if (token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else token
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

}