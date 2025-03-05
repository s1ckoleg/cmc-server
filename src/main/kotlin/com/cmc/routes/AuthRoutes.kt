package com.cmc.routes

import com.cmc.auth.JwtConfig
import com.cmc.database.services.UserService
import com.cmc.models.auth.AuthRequest
import com.cmc.models.auth.AuthResponse
import com.cmc.models.auth.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRoutes(userService: UserService) {
    routing {
        route("/auth") {
            // Login
            post("/login") {
                try {
                    val request = call.receive<AuthRequest>()
                    val user = userService.validateUser(request)
                    
                    if (user != null) {
                        val token = JwtConfig.generateToken(user.id, user.username)
                        call.respond(
                            AuthResponse(
                                token = token,
                                userId = user.id,
                                username = user.username
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
            
            // Register
            post("/register") {
                try {
                    val request = call.receive<RegisterRequest>()
                    
                    // Validate request
                    if (request.username.length < 3) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Username must be at least 3 characters"))
                        return@post
                    }
                    
                    if (request.password.length < 6) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password must be at least 6 characters"))
                        return@post
                    }
                    
                    val user = userService.createUser(request)
                    
                    if (user != null) {
                        val token = JwtConfig.generateToken(user.id, user.username)
                        call.respond(
                            HttpStatusCode.Created,
                            AuthResponse(
                                token = token,
                                userId = user.id,
                                username = user.username
                            )
                        )
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create user"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "User already exists")))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }
        }
    }
} 