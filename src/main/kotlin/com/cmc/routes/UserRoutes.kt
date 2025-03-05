package com.cmc.routes

import com.cmc.database.services.UserService
import com.cmc.models.User
import com.cmc.models.auth.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.userRoutes() {
    val userService = UserService()
    
    routing {
        route("/users") {
            // Get all users
            get {
                call.respond(userService.getAllUsers())
            }
            
            // Get user by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@get
                }
                
                val user = userService.getUserById(id)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }
                
                call.respond(user)
            }
            
            // Create new user
            post {
                try {
                    val user = call.receive<RegisterRequest>()
                    val newUser = userService.createUser(user)
                    
                    if (newUser != null) {
                        call.respond(HttpStatusCode.Created, newUser)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create user"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
            
            // Update user
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@put
                }
                
                try {
                    val user = call.receive<User>()
                    val updated = userService.updateUser(id, user)
                    
                    if (updated) {
                        val updatedUser = userService.getUserById(id)
                        if (updatedUser != null) {
                            call.respond(updatedUser)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve updated user"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
            
            // Delete user
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@delete
                }
                
                val deleted = userService.deleteUser(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
        }
    }
} 