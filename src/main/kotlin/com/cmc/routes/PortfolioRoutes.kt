package com.cmc.routes

import com.cmc.database.services.PortfolioService
import com.cmc.models.PortfolioEntry
import com.cmc.models.PortfolioEntryRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal

fun Application.portfolioRoutes(portfolioService: PortfolioService) {
    val logger = LoggerFactory.getLogger("PortfolioRoutes")
    
    routing {
        // All portfolio routes are protected by JWT
        authenticate("jwt") {
            route("/api/portfolio") {
                // Get portfolio summary
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@get
                    }
                    
                    val summary = portfolioService.getPortfolioSummary(userId)
                    call.respond(summary)
                }
                
                // Get all portfolio entries
                get("/entries") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@get
                    }
                    
                    val entries = portfolioService.getAllPortfolioEntries(userId)
                    call.respond(entries)
                }
                
                // Get portfolio entry by ID
                get("/entries/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@get
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                        return@get
                    }
                    
                    val entry = portfolioService.getPortfolioEntryById(id, userId)
                    if (entry == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Portfolio entry not found"))
                        return@get
                    }
                    
                    call.respond(entry)
                }
                
                // Create new portfolio entry
                post("/entries") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        logger.error("Failed to get userId from token")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@post
                    }
                    
                    try {
                        val request = call.receive<PortfolioEntryRequest>()
                        logger.info("Received portfolio entry request: $request")
                        
                        val entry = PortfolioEntry(
                            userId = userId,
                            cryptoId = request.cryptoId,
                            quantity = request.quantity,
                            entryPrice = request.entryPrice,
                            notes = request.notes,
                            totalValue = null // Will be calculated in service
                        )
                        
                        val newEntry = portfolioService.createPortfolioEntry(entry)
                        
                        if (newEntry != null) {
                            logger.info("Created portfolio entry: $newEntry")
                            call.respond(HttpStatusCode.Created, newEntry)
                        } else {
                            logger.error("Failed to create portfolio entry for user $userId")
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create portfolio entry"))
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating portfolio entry: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                    }
                }
                
                // Update portfolio entry
                put("/entries/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        logger.error("Failed to get userId from token")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@put
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                        return@put
                    }
                    
                    try {
                        val request = call.receive<PortfolioEntryRequest>()
                        logger.info("Received portfolio entry update request: $request")
                        
                        val entry = PortfolioEntry(
                            id = id,
                            userId = userId,
                            cryptoId = request.cryptoId,
                            quantity = request.quantity,
                            entryPrice = request.entryPrice,
                            notes = request.notes,
                            totalValue = null // Will be calculated in service
                        )
                        
                        val updated = portfolioService.updatePortfolioEntry(id, userId, entry)
                        
                        if (updated) {
                            val updatedEntry = portfolioService.getPortfolioEntryById(id, userId)
                            if (updatedEntry != null) {
                                logger.info("Updated portfolio entry: $updatedEntry")
                                call.respond(updatedEntry)
                            } else {
                                logger.error("Failed to retrieve updated portfolio entry")
                                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve updated portfolio entry"))
                            }
                        } else {
                            logger.error("Portfolio entry not found: id=$id, userId=$userId")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Portfolio entry not found"))
                        }
                    } catch (e: Exception) {
                        logger.error("Error updating portfolio entry: ${e.message}", e)
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                    }
                }
                
                // Delete portfolio entry
                delete("/entries/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", Int::class) ?: run {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                        return@delete
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                        return@delete
                    }
                    
                    val deleted = portfolioService.deletePortfolioEntry(id, userId)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Portfolio entry not found"))
                    }
                }
            }
        }
    }
} 