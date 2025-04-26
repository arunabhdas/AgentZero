package app.agentzero.agentzeroapp.security

import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant.now
import java.util.Base64
import java.util.Date
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.exp

@Service
class JwtService(
    @Value("JWT_SECRET_BASE64") private val jwtSecret: String
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

}