package com.cmc.database.services

import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.CoinStats
import com.cmc.models.CoinStats as CoinStatsModel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.time.LocalDateTime

class CoinStatsService {
    
    /**
     * Gets the latest stats for a specific coin.
     */
    suspend fun getLatestStatsByCoinId(coinId: Int): CoinStatsModel? = dbQuery {
        CoinStats.select { CoinStats.coinId eq coinId }
            .orderBy(CoinStats.date to SortOrder.DESC)
            .limit(1)
            .map { it.toCoinStats() }
            .singleOrNull()
    }
    
    /**
     * Gets historical stats for a specific coin for a given date.
     */
    suspend fun getStatsByCoinIdAndDate(coinId: Int, date: LocalDateTime): CoinStatsModel? = dbQuery {
        CoinStats.select { (CoinStats.coinId eq coinId) and (CoinStats.date eq date) }
            .map { it.toCoinStats() }
            .singleOrNull()
    }
    
    /**
     * Gets historical stats for a specific coin within a date range.
     */
    suspend fun getStatsByCoinIdAndDateRange(
        coinId: Int, 
        fromDate: LocalDateTime, 
        toDate: LocalDateTime
    ): List<CoinStatsModel> = dbQuery {
        CoinStats.select { 
            (CoinStats.coinId eq coinId) and 
            (CoinStats.date greaterEq fromDate) and 
            (CoinStats.date lessEq toDate) 
        }
        .orderBy(CoinStats.date to SortOrder.ASC)
        .map { it.toCoinStats() }
    }
    
    /**
     * Creates or updates stats for a specific coin on a specific date.
     * If stats already exist for the coin and date, they will be updated.
     * Otherwise, a new record will be created.
     */
    suspend fun createOrUpdateStats(stats: CoinStatsModel): CoinStatsModel? = dbQuery {
        // Check if we already have stats for this coin and date
        val existingStats = CoinStats.select { 
            (CoinStats.coinId eq stats.coinId) and 
            (CoinStats.date eq stats.date) 
        }.singleOrNull()
        
        if (existingStats != null) {
            // Update existing stats
            CoinStats.update({ CoinStats.id eq existingStats[CoinStats.id] }) {
                it[currentPrice] = stats.currentPrice
                it[marketCap] = stats.marketCap
                it[volume24h] = stats.volume24h
                // Don't update the date as it's part of the unique constraint
            }
            return@dbQuery getStatsByCoinIdAndDate(stats.coinId, stats.date)
        }
        
        // Create new stats
        val insertStatement = CoinStats.insert {
            it[coinId] = stats.coinId
            it[currentPrice] = stats.currentPrice
            it[marketCap] = stats.marketCap
            it[volume24h] = stats.volume24h
            it[date] = stats.date
        }
        
        insertStatement.resultedValues?.singleOrNull()?.toCoinStats()
    }
    
    /**
     * Gets the latest stats for all coins.
     * Returns a map of coinId to CoinStatsModel.
     */
    suspend fun getAllLatestStats(): Map<Int, CoinStatsModel> = dbQuery {
        // Get all stats ordered by date desc for each coin
        CoinStats.selectAll()
            .orderBy(CoinStats.coinId to SortOrder.ASC, CoinStats.date to SortOrder.DESC)
            .map { it.toCoinStats() }
            .groupBy { it.coinId }
            .mapValues { it.value.first() }
    }
    
    /**
     * Gets the latest stats for all coins for a specific date.
     * Returns a map of coinId to CoinStatsModel.
     */
    suspend fun getAllStatsForDate(date: LocalDateTime): Map<Int, CoinStatsModel> = dbQuery {
        CoinStats.select { CoinStats.date eq date }
            .map { it.toCoinStats() }
            .associateBy { it.coinId }
    }
    
    /**
     * Deletes old stats records that are older than the specified number of days.
     * Returns the number of records deleted.
     */
    suspend fun deleteOldStats(olderThanDays: Int): Int = dbQuery {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
        CoinStats.deleteWhere { CoinStats.date less cutoffDate }
    }
    
    private fun ResultRow.toCoinStats(): CoinStatsModel = CoinStatsModel(
        id = this[CoinStats.id],
        coinId = this[CoinStats.coinId],
        currentPrice = this[CoinStats.currentPrice],
        marketCap = this[CoinStats.marketCap],
        volume24h = this[CoinStats.volume24h],
        date = this[CoinStats.date]
    )
} 