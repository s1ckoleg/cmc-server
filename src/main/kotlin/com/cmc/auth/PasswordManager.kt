package com.cmc.auth

import org.mindrot.jbcrypt.BCrypt

object PasswordManager {
    /**
     * Hashes a password using BCrypt
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    /**
     * Verifies a password against a hash
     */
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }
} 