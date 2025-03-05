package com.cmc.database.services

import com.cmc.database.DatabaseFactory.dbQuery
import com.cmc.database.tables.Coins
import com.cmc.models.Coin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class CoinService {
    
    suspend fun getAllCoins(): List<Coin> = dbQuery {
        Coins.selectAll()
            .map { it.toCoin() }
    }
    
    suspend fun getCoinById(id: Int): Coin? = dbQuery {
        Coins.select { Coins.id eq id }
            .map { it.toCoin() }
            .singleOrNull()
    }
    
    suspend fun getCoinByTicker(ticker: String): Coin? = dbQuery {
        Coins.select { Coins.ticker eq ticker.uppercase() }
            .map { it.toCoin() }
            .singleOrNull()
    }
    
    suspend fun createCoin(coin: Coin): Coin? = dbQuery {
        // Check if ticker already exists
        val existingCoin = Coins.select { Coins.ticker eq coin.ticker.uppercase() }.singleOrNull()
        if (existingCoin != null) {
            // Update existing coin
            Coins.update({ Coins.ticker eq coin.ticker.uppercase() }) {
                it[name] = coin.name
            }
            return@dbQuery getCoinByTicker(coin.ticker)
        }
        
        val insertStatement = Coins.insert {
            it[ticker] = coin.ticker.uppercase()
            it[name] = coin.name
        }
        
        insertStatement.resultedValues?.singleOrNull()?.toCoin()
    }
    
    suspend fun updateCoin(id: Int, coin: Coin): Boolean = dbQuery {
        Coins.update({ Coins.id eq id }) {
            it[ticker] = coin.ticker.uppercase()
            it[name] = coin.name
        } > 0
    }
    
    suspend fun deleteCoin(id: Int): Boolean = dbQuery {
        Coins.deleteWhere { Coins.id eq id } > 0
    }
    
    private fun ResultRow.toCoin(): Coin = Coin(
        id = this[Coins.id],
        ticker = this[Coins.ticker],
        name = this[Coins.name]
    )
} 