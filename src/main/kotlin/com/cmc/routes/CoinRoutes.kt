package com.cmc.routes

import com.cmc.database.services.CoinService
import com.cmc.database.services.CoinStatsService
import com.cmc.models.Coin
import com.cmc.models.CoinWithStats
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun Application.coinRoutes(
    coinService: CoinService,
    coinStatsService: CoinStatsService
) {
    routing {
        route("/api/coins") {
            // Get all coins with their latest stats
            get {
                val coins = coinService.getAllCoins()
                val allStats = coinStatsService.getAllLatestStats()
                
                val coinsWithStats = coins.map { coin ->
                    val stats = allStats[coin.id]
                    CoinWithStats.fromCoinAndStats(coin, stats)
                }
                
                call.respond(coinsWithStats)
            }
            
            // Get all coins with stats for a specific date
            get("/date/{date}") {
                val dateStr = call.parameters["date"]
                if (dateStr.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Date is required (format: yyyy-MM-dd'T'HH:mm:ss)"))
                    return@get
                }
                
                val date = try {
                    LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid date format. Use yyyy-MM-dd'T'HH:mm:ss"))
                    return@get
                }
                
                val coins = coinService.getAllCoins()
                val allStats = coinStatsService.getAllStatsForDate(date)
                
                val coinsWithStats = coins.map { coin ->
                    val stats = allStats[coin.id]
                    CoinWithStats.fromCoinAndStats(coin, stats)
                }
                
                call.respond(coinsWithStats)
            }
            
            // Get coin by ID with its latest stats
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@get
                }
                
                val coin = coinService.getCoinById(id)
                if (coin == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                    return@get
                }
                
                val stats = coinStatsService.getLatestStatsByCoinId(id)
                val coinWithStats = CoinWithStats.fromCoinAndStats(coin, stats)
                
                call.respond(coinWithStats)
            }
            
            // Get coin by ID with stats for a specific date
            get("/{id}/date/{date}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@get
                }
                
                val dateStr = call.parameters["date"]
                if (dateStr.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Date is required (format: yyyy-MM-dd'T'HH:mm:ss)"))
                    return@get
                }
                
                val date = try {
                    LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid date format. Use yyyy-MM-dd'T'HH:mm:ss"))
                    return@get
                }
                
                val coin = coinService.getCoinById(id)
                if (coin == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                    return@get
                }
                
                val stats = coinStatsService.getStatsByCoinIdAndDate(id, date)
                val coinWithStats = CoinWithStats.fromCoinAndStats(coin, stats)
                
                call.respond(coinWithStats)
            }
            
            // Get historical data for a coin within a date range
            get("/{id}/history") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                    return@get
                }
                
                val fromDateStr = call.parameters["from"]
                val toDateStr = call.parameters["to"]
                
                if (fromDateStr.isNullOrBlank() || toDateStr.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, 
                        mapOf("error" to "Both 'from' and 'to' date parameters are required (format: yyyy-MM-dd'T'HH:mm:ss)"))
                    return@get
                }
                
                val fromDate = try {
                    LocalDateTime.parse(fromDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'from' date format. Use yyyy-MM-dd'T'HH:mm:ss"))
                    return@get
                }
                
                val toDate = try {
                    LocalDateTime.parse(toDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid 'to' date format. Use yyyy-MM-dd'T'HH:mm:ss"))
                    return@get
                }
                
                if (toDate.isBefore(fromDate)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "'to' date must be after 'from' date"))
                    return@get
                }
                
                val coin = coinService.getCoinById(id)
                if (coin == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                    return@get
                }
                
                val statsHistory = coinStatsService.getStatsByCoinIdAndDateRange(id, fromDate, toDate)
                
                // Map the stats to a simplified format for the response
                val historyResponse = statsHistory.map { stats ->
                    mapOf(
                        "date" to stats.date.toString(),
                        "price" to stats.currentPrice,
                        "marketCap" to stats.marketCap,
                        "volume" to stats.volume24h
                    )
                }
                
                call.respond(mapOf(
                    "coin" to coin,
                    "history" to historyResponse
                ))
            }
            
            // Get coin by ticker with its latest stats
            get("/ticker/{ticker}") {
                val ticker = call.parameters["ticker"]
                if (ticker.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Ticker is required"))
                    return@get
                }
                
                val coin = coinService.getCoinByTicker(ticker.uppercase())
                if (coin == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                    return@get
                }
                
                val stats = coinStatsService.getLatestStatsByCoinId(coin.id)
                val coinWithStats = CoinWithStats.fromCoinAndStats(coin, stats)
                
                call.respond(coinWithStats)
            }
            
            // Admin routes - protected by JWT
            authenticate("jwt") {
                // Add a new coin
                post {
                    try {
                        val coin = call.receive<Coin>()
                        val newCoin = coinService.createCoin(coin)
                        
                        if (newCoin != null) {
                            call.respond(HttpStatusCode.Created, newCoin)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create coin"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                    }
                }
                
                // Update a coin
                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                        return@put
                    }
                    
                    try {
                        val coin = call.receive<Coin>()
                        val updated = coinService.updateCoin(id, coin)
                        
                        if (updated) {
                            val updatedCoin = coinService.getCoinById(id)
                            if (updatedCoin != null) {
                                call.respond(updatedCoin)
                            } else {
                                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve updated coin"))
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                    }
                }
                
                // Delete a coin
                delete("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                        return@delete
                    }
                    
                    val deleted = coinService.deleteCoin(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Coin not found"))
                    }
                }
            }
        }
    }
} 