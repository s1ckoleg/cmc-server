package com.cmc.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.FileInputStream
import java.util.Properties
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun init() {
        try {
            val dbProps = loadDbProperties()
            val dbUrl = dbProps.getProperty("db.url")
            val dbUser = dbProps.getProperty("db.user")
            val dbPassword = dbProps.getProperty("db.password")
            val dbDriver = dbProps.getProperty("db.driver")
            val maxPoolSize = dbProps.getProperty("db.pool.max", "10").trim().toInt()
            
            val config = HikariConfig().apply {
                driverClassName = dbDriver
                jdbcUrl = dbUrl
                username = dbUser
                password = dbPassword
                maximumPoolSize = maxPoolSize
                isAutoCommit = true
                
                connectionTimeout = 30000
                maxLifetime = 600000
                idleTimeout = 300000
                validationTimeout = 5000
                leakDetectionThreshold = 60000
                connectionTestQuery = "SELECT 1"
                minimumIdle = 5
                
                validate()
            }
            
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            
            logger.info("Connected to database: $dbUrl")
        } catch (e: Exception) {
            logger.error("Failed to connect to database", e)
            throw e
        }
    }
    
    private fun loadDbProperties(): Properties {
        val props = Properties()
        val propFile = javaClass.classLoader.getResourceAsStream("db.properties")
            ?: throw IllegalStateException("Could not find db.properties file")
            
        propFile.use { props.load(it) }
        return props
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
} 