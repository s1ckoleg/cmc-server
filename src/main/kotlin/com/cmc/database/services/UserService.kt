package com.cmc.database.services

import com.cmc.auth.PasswordManager
import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.Users
import com.cmc.models.User
import com.cmc.models.auth.AuthRequest
import com.cmc.models.auth.RegisterRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class UserService {
    
    suspend fun getAllUsers(): List<User> = dbQuery {
        Users.selectAll()
            .map { it.toUser() }
    }
    
    suspend fun getUserById(id: Int): User? = dbQuery {
        Users.select { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    suspend fun getUserByUsername(username: String): User? = dbQuery {
        Users.select { Users.username eq username }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    suspend fun createUser(request: RegisterRequest): User? = dbQuery {
        // Check if username already exists
        val existingUser = Users.select { Users.username eq request.username }.singleOrNull()
        if (existingUser != null) {
            throw IllegalArgumentException("Username already exists")
        }
        
        // Check if email already exists
        val existingEmail = Users.select { Users.email eq request.email }.singleOrNull()
        if (existingEmail != null) {
            throw IllegalArgumentException("Email already exists")
        }
        
        val hashedPassword = PasswordManager.hashPassword(request.password)
        
        val insertStatement = Users.insert {
            it[username] = request.username
            it[email] = request.email
            it[passwordHash] = hashedPassword
        }
        
        insertStatement.resultedValues?.singleOrNull()?.toUser()
    }
    
    suspend fun updateUser(id: Int, user: User): Boolean = dbQuery {
        Users.update({ Users.id eq id }) {
            it[username] = user.username
            it[email] = user.email
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }
    
    suspend fun deleteUser(id: Int): Boolean = dbQuery {
        Users.deleteWhere { Users.id eq id } > 0
    }
    
    suspend fun validateUser(request: AuthRequest): User? = dbQuery {
        val user = Users.select { Users.username eq request.username }
            .map { it.toUser() }
            .singleOrNull()
        
        if (user != null && PasswordManager.verifyPassword(request.password, user.passwordHash ?: "")) {
            user
        } else {
            null
        }
    }
    
    private fun ResultRow.toUser(): User = User(
        id = this[Users.id],
        username = this[Users.username],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash]
    )
} 