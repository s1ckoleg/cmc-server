package com.cmc.database.services

import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.Coins
import com.cmc.database.tables.CoinStats
import com.cmc.database.tables.PortfolioEntries
import com.cmc.models.PortfolioEntry
import com.cmc.models.PortfolioSummary
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class PortfolioService(
    private val coinService: CoinService,
    private val coinStatsService: CoinStatsService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    suspend fun getAllPortfolioEntries(userId: Int): List<PortfolioEntry> = dbQuery {
        // First get all portfolio entries with coin info
        val entries = (PortfolioEntries innerJoin Coins)
            .select { PortfolioEntries.userId eq userId }
            .map { row ->
                val coinId = row[PortfolioEntries.cryptoId]
                val quantity = row[PortfolioEntries.quantity]
                val entryPrice = row[PortfolioEntries.entryPrice]
                
                PortfolioEntry(
                    id = row[PortfolioEntries.id],
                    userId = row[PortfolioEntries.userId],
                    cryptoId = coinId,
                    quantity = quantity,
                    entryPrice = entryPrice,
                    entryDate = row[PortfolioEntries.entryDate],
                    notes = row[PortfolioEntries.notes],
                    
                    // Basic coin info
                    cryptoSymbol = null, // Will be set below
                    cryptoName = row[Coins.name],
                    currentPrice = null, // Will be set below
                    currentValue = null, // Will be calculated below
                    profitLoss = null, // Will be calculated below
                    profitLossPercentage = null, // Will be calculated below
                    totalValue = BigDecimal.ZERO
                )
            }
        
        // Get all latest stats for the coins
        val coinIds = entries.map { it.cryptoId }.distinct()
        val statsMap = coinIds.mapNotNull { coinId ->
            val stats = coinStatsService.getLatestStatsByCoinId(coinId)
            if (stats != null) {
                coinId to stats
            } else {
                null
            }
        }.toMap()
        
        // Enhance entries with stats
        entries.map { entry ->
            val stats = statsMap[entry.cryptoId]
            val currentPrice = stats?.currentPrice ?: BigDecimal.ZERO
            val currentValue = currentPrice.multiply(entry.quantity)
            val investment = entry.entryPrice.multiply(entry.quantity)
            val profitLoss = currentValue.subtract(investment)
            val profitLossPercentage = if (investment > BigDecimal.ZERO) {
                profitLoss.divide(investment, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }
            
            val coin = coinService.getCoinById(entry.cryptoId)
            
            entry.copy(
                cryptoSymbol = coin?.ticker,
                currentPrice = currentPrice,
                currentValue = currentValue,
                profitLoss = profitLoss,
                profitLossPercentage = profitLossPercentage,
                totalValue = currentValue
            )
        }
    }
    
    suspend fun getPortfolioEntryById(id: Int, userId: Int): PortfolioEntry? = dbQuery {
        logger.info("Getting portfolio entry for id=$id and userId=$userId")
        // First get the basic portfolio entry
        val entry = PortfolioEntries
            .select { (PortfolioEntries.id eq id) and (PortfolioEntries.userId eq userId) }
            .map { row ->
                val coinId = row[PortfolioEntries.cryptoId]
                val quantity = row[PortfolioEntries.quantity]
                val entryPrice = row[PortfolioEntries.entryPrice]
                
                logger.info("Found portfolio entry: coinId=$coinId, quantity=$quantity, entryPrice=$entryPrice")
                
                PortfolioEntry(
                    id = row[PortfolioEntries.id],
                    userId = row[PortfolioEntries.userId],
                    cryptoId = coinId,
                    quantity = quantity,
                    entryPrice = entryPrice,
                    entryDate = row[PortfolioEntries.entryDate],
                    notes = row[PortfolioEntries.notes],
                    
                    // These will be set below
                    cryptoSymbol = null,
                    cryptoName = null,
                    currentPrice = null,
                    currentValue = null,
                    profitLoss = null,
                    profitLossPercentage = null,
                    totalValue = BigDecimal.ZERO
                )
            }
            .singleOrNull() ?: run {
                logger.error("Portfolio entry not found for id=$id and userId=$userId")
                return@dbQuery null
            }
        
        // Get the coin data
        val coin = coinService.getCoinById(entry.cryptoId)
        if (coin == null) {
            logger.error("Coin not found for cryptoId=${entry.cryptoId}")
            return@dbQuery null
        }
        logger.info("Found coin: ${coin.ticker} (${coin.name})")
        
        // Get latest stats for the coin
        val stats = coinStatsService.getLatestStatsByCoinId(entry.cryptoId)
        if (stats == null) {
            logger.warn("No stats found for coin ${coin.ticker}")
        }
        
        val currentPrice = stats?.currentPrice ?: BigDecimal.ZERO
        val currentValue = currentPrice.multiply(entry.quantity)
        val investment = entry.entryPrice.multiply(entry.quantity)
        val profitLoss = currentValue.subtract(investment)
        val profitLossPercentage = if (investment > BigDecimal.ZERO) {
            profitLoss.divide(investment, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        logger.info("Calculated values: currentPrice=$currentPrice, currentValue=$currentValue, investment=$investment, profitLoss=$profitLoss, profitLossPercentage=$profitLossPercentage")
        
        entry.copy(
            cryptoSymbol = coin.ticker,
            cryptoName = coin.name,
            currentPrice = currentPrice,
            currentValue = currentValue,
            profitLoss = profitLoss,
            profitLossPercentage = profitLossPercentage,
            totalValue = currentValue
        ).also {
            logger.info("Returning portfolio entry: $it")
        }
    }
    
    suspend fun createPortfolioEntry(entry: PortfolioEntry): PortfolioEntry? = dbQuery {
        try {
            logger.info("Creating portfolio entry: $entry")
            
            // First check if the coin exists
            val coin = coinService.getCoinById(entry.cryptoId)
            if (coin == null) {
                logger.error("Failed to create portfolio entry: Coin with ID ${entry.cryptoId} not found")
                return@dbQuery null
            }
            
            var insertedId: Int? = null
            
            // Create the entry and get its ID in the same transaction
            PortfolioEntries.insert {
                it[userId] = entry.userId
                it[cryptoId] = entry.cryptoId
                it[quantity] = entry.quantity
                it[entryPrice] = entry.entryPrice
                it[entryDate] = entry.entryDate
                it[notes] = entry.notes
            }.resultedValues?.singleOrNull()?.let { row ->
                insertedId = row[PortfolioEntries.id]
            }
            
            if (insertedId == null) {
                logger.error("Failed to create portfolio entry: No ID returned from insert")
                return@dbQuery null
            }
            
            logger.info("Successfully created portfolio entry with ID: $insertedId")
            
            // Create the response directly instead of querying again
            PortfolioEntry(
                id = insertedId!!,
                userId = entry.userId,
                cryptoId = entry.cryptoId,
                quantity = entry.quantity,
                entryPrice = entry.entryPrice,
                entryDate = entry.entryDate,
                notes = entry.notes,
                cryptoSymbol = coin.ticker,
                cryptoName = coin.name,
                currentPrice = BigDecimal.ZERO,  // We'll set this to 0 initially
                currentValue = BigDecimal.ZERO,  // We'll set this to 0 initially
                profitLoss = BigDecimal.ZERO,    // We'll set this to 0 initially
                profitLossPercentage = BigDecimal.ZERO,  // We'll set this to 0 initially
                totalValue = entry.quantity.multiply(entry.entryPrice)  // Use entry price as initial value
            ).also {
                logger.info("Returning newly created portfolio entry: $it")
            }
        } catch (e: Exception) {
            logger.error("Error creating portfolio entry: ${e.message}", e)
            null
        }
    }
    
    suspend fun updatePortfolioEntry(id: Int, userId: Int, entry: PortfolioEntry): Boolean = dbQuery {
        PortfolioEntries.update({ (PortfolioEntries.id eq id) and (PortfolioEntries.userId eq userId) }) {
            it[cryptoId] = entry.cryptoId
            it[quantity] = entry.quantity
            it[entryPrice] = entry.entryPrice
            it[entryDate] = entry.entryDate
            it[notes] = entry.notes
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }
    
    suspend fun deletePortfolioEntry(id: Int, userId: Int): Boolean = dbQuery {
        PortfolioEntries.deleteWhere { (PortfolioEntries.id eq id) and (PortfolioEntries.userId eq userId) } > 0
    }
    
    suspend fun getPortfolioSummary(userId: Int): PortfolioSummary = dbQuery {
        val entries = getAllPortfolioEntries(userId)
        
        var totalInvestment = BigDecimal.ZERO
        var totalCurrentValue = BigDecimal.ZERO
        
        entries.forEach { entry ->
            val investment = entry.entryPrice.multiply(entry.quantity)
            totalInvestment = totalInvestment.add(investment)
            
            val currentValue = entry.currentValue ?: BigDecimal.ZERO
            totalCurrentValue = totalCurrentValue.add(currentValue)
        }
        
        val totalProfitLoss = totalCurrentValue.subtract(totalInvestment)
        val totalProfitLossPercentage = if (totalInvestment > BigDecimal.ZERO) {
            totalProfitLoss.divide(totalInvestment, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        PortfolioSummary(
            totalInvestment = totalInvestment,
            totalCurrentValue = totalCurrentValue,
            totalProfitLoss = totalProfitLoss,
            totalProfitLossPercentage = totalProfitLossPercentage,
            entries = entries
        )
    }
} 