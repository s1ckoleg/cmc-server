package com.cmc.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.jwt.*
import java.util.*
import java.io.FileInputStream
import java.util.Properties

object JwtConfig {
    private val SECRET = loadSecret()
    private const val ISSUER = "crypto-portfolio-api"
    private const val AUDIENCE = "crypto-portfolio-users"
    private const val VALIDITY_IN_MS = 36_000_00 * 10 // 10 hours
    
    private val algorithm = Algorithm.HMAC256(SECRET)
    
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()
    
    private fun loadSecret(): String {
        return try {
            // First try to load from .env file
            val props = Properties()
            val envFile = java.io.File(".env")
            if (envFile.exists()) {
                FileInputStream(envFile).use { props.load(it) }
                props.getProperty("JWT_SECRET") ?: throw IllegalStateException("JWT_SECRET not found in .env file")
            } else {
                // Fallback to environment variable
                System.getenv("JWT_SECRET") ?: throw IllegalStateException("JWT_SECRET environment variable is not set")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load JWT_SECRET: ${e.message}")
        }
    }
    
    /**
     * Generates a JWT token for a specific user
     */
    fun generateToken(userId: Int, username: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
        .sign(algorithm)
    
    /**
     * Configures the JWT authentication feature
     */
    fun configureKtorFeature(config: JWTAuthenticationProvider.Config) = with(config) {
        verifier(verifier)
        realm = "crypto-portfolio-api"
        validate { credential ->
            val userId = credential.payload.getClaim("userId").asInt()
            val username = credential.payload.getClaim("username").asString()
            
            if (userId != null && username != null) {
                JWTPrincipal(credential.payload)
            } else {
                null
            }
        }
    }
} 