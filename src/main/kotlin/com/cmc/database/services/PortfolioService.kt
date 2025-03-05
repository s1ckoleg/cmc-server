package com.cmc.database.services

import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.Coins
import com.cmc.database.tables.CoinStats
import com.cmc.database.tables.PortfolioEntries
import com.cmc.models.PortfolioEntry
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class PortfolioService(
    private val coinService: CoinService,
    private val coinStatsService: CoinStatsService
) {
    
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
        val entry = (PortfolioEntries innerJoin Coins)
            .select { (PortfolioEntries.id eq id) and (PortfolioEntries.userId eq userId) }
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
            .singleOrNull() ?: return@dbQuery null
        
        // Get latest stats for the coin
        val stats = coinStatsService.getLatestStatsByCoinId(entry.cryptoId)
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
    
    suspend fun createPortfolioEntry(entry: PortfolioEntry): PortfolioEntry? = dbQuery {
        val insertStatement = PortfolioEntries.insert {
            it[userId] = entry.userId
            it[cryptoId] = entry.cryptoId
            it[quantity] = entry.quantity
            it[entryPrice] = entry.entryPrice
            it[entryDate] = entry.entryDate
            it[notes] = entry.notes
        }
        
        val insertedId = insertStatement.resultedValues?.singleOrNull()?.get(PortfolioEntries.id)
        if (insertedId != null) {
            getPortfolioEntryById(insertedId, entry.userId)
        } else {
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
    
    suspend fun getPortfolioSummary(userId: Int): Map<String, Any> = dbQuery {
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
        
        mapOf(
            "totalInvestment" to totalInvestment,
            "totalCurrentValue" to totalCurrentValue,
            "totalProfitLoss" to totalProfitLoss,
            "totalProfitLossPercentage" to totalProfitLossPercentage,
            "entries" to entries
        )
    }
} 