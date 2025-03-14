package com.cmc

import com.cmc.auth.JwtConfig
import com.cmc.database.DatabaseFactory
import com.cmc.database.services.CoinService
import com.cmc.database.services.CoinStatsService
import com.cmc.database.services.PortfolioService
import com.cmc.database.services.UserService
import com.cmc.database.tables.Coins
import com.cmc.database.tables.CoinStats
import com.cmc.database.tables.PortfolioEntries
import com.cmc.database.tables.Users
import com.cmc.routes.authRoutes
import com.cmc.routes.coinRoutes
import com.cmc.routes.portfolioRoutes
import com.cmc.tasks.TaskManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

fun main() {
    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
    
    Runtime.getRuntime().addShutdownHook(Thread {
        server.application.environment.log.info("Shutting down server...")
        server.stop(1, 5, TimeUnit.SECONDS)
    })
    
    server.start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    
    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users)
        SchemaUtils.createMissingTablesAndColumns(Coins)
        SchemaUtils.createMissingTablesAndColumns(CoinStats)
        SchemaUtils.createMissingTablesAndColumns(PortfolioEntries)
    }
    
    transaction {
        if (Coins.selectAll().count().toInt() == 0) {
            Coins.insert {
                it[ticker] = "BTC"
                it[name] = "Bitcoin"
            }
            Coins.insert {
                it[ticker] = "ALPH"
                it[name] = "Alephium"
            }
        }
    }
    
    val userService = UserService()
    val coinService = CoinService()
    val coinStatsService = CoinStatsService()
    val portfolioService = PortfolioService(coinService, coinStatsService)
    
    val taskManager = TaskManager(coinService, coinStatsService)
    environment.monitor.subscribe(ApplicationStarted) {
        taskManager.startTasks()
    }
    environment.monitor.subscribe(ApplicationStopping) {
        taskManager.stopTasks()
    }
    
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Unknown error"))
            )
        }
    }
    
    install(Authentication) {
        jwt("jwt") {
            JwtConfig.configureKtorFeature(this)
        }
    }
    
    configureRouting()
    
    authRoutes(userService)
    coinRoutes(coinService, coinStatsService)
    portfolioRoutes(portfolioService)
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to Crypto Portfolio API!", ContentType.Text.Plain)
        }
        
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }
    }
} 