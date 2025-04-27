package app.agentzero.agentzeroapp.security

import app.agentzero.agentzeroapp.data.model.User
import app.agentzero.agentzeroapp.data.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder
) {
    fun register(email: String, password: String): User {
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }
}