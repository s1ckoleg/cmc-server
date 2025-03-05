package com.cmc.tasks

import com.cmc.database.services.CoinService
import com.cmc.database.services.CoinStatsService
import com.cmc.models.CoinStats
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Task that periodically fetches cryptocurrency data from an external API
 * and updates the CoinStats table.
 */
class CoinDataFetchTask(
    private val coinService: CoinService,
    private val coinStatsService: CoinStatsService,
    private val fetchInterval: Duration = Duration.ofMinutes(10)
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Starts the periodic task to fetch coin data.
     */
    fun start() {
        logger.info("Starting coin data fetch task with interval: ${fetchInterval.toMinutes()} minutes")
        
        scheduler.scheduleAtFixedRate(
            { runFetchTask() },
            0,
            fetchInterval.toMinutes(),
            TimeUnit.MINUTES
        )
    }
    
    /**
     * Stops the periodic task.
     */
    fun stop() {
        logger.info("Stopping coin data fetch task")
        scheduler.shutdown()
        coroutineScope.cancel()
        runBlocking {
            httpClient.close()
        }
    }
    
    private fun runFetchTask() {
        coroutineScope.launch {
            try {
                logger.info("Fetching coin data from external API")
                fetchAndUpdateCoinData()
                logger.info("Coin data fetch completed successfully")
            } catch (e: Exception) {
                logger.error("Error fetching coin data: ${e.message}", e)
            }
        }
    }
    
    /**
     * Fetches coin data from external API and updates the CoinStats table.
     */
    private suspend fun fetchAndUpdateCoinData() {
        try {
            logger.info("Fetching coin data from external API")
            val coins = coinService.getAllCoins()
            
            for (coin in coins) {
                try {
                    val apiData = fetchCoinDataFromApi(coin.ticker)
                    if (apiData != null) {
                        coinStatsService.createOrUpdateStats(
                            stats = CoinStats(
                                coinId = coin.id,
                                currentPrice = apiData.price,
                                marketCap = apiData.marketCap,
                                volume24h = apiData.volume,
                                date = LocalDateTime.now()
                            )
                        )
                        logger.info("Successfully updated stats for ${coin.ticker}")
                    } else {
                        logger.warn("Skipping ${coin.ticker} - no data available")
                    }
                } catch (e: Exception) {
                    logger.error("Error updating stats for ${coin.ticker}: ${e.message}", e)
                }
            }
            
            logger.info("Coin data fetch completed successfully")
        } catch (e: Exception) {
            logger.error("Error in coin data fetch task: ${e.message}", e)
        }
    }
    
    /**
     * Fetches coin data from the Gate.io API.
     */
    private suspend fun fetchCoinDataFromApi(ticker: String): CoinApiResponse? {
        try {
            val gateioResponse = httpClient.get("https://api.gateio.ws/api/v4/spot/tickers") {
                url {
                    parameters.append("currency_pair", "${ticker}_USDT")
                }
            }
            val kryptexResponse = httpClient.get("https://api.kryptex.network/api/v1/coin/${ticker.lowercase()}/info") {}
            
            if (gateioResponse.status.isSuccess() && kryptexResponse.status.isSuccess()) {
                val gateioJson = json.parseToJsonElement(gateioResponse.bodyAsText()).jsonArray
                val kryptexJson = json.parseToJsonElement(kryptexResponse.bodyAsText())
                
                if (gateioJson.isNotEmpty()) {
                    val gateioTickerData = gateioJson[0].jsonObject
                    
                    return CoinApiResponse(
                        price = BigDecimal(gateioTickerData["last"]?.jsonPrimitive?.content ?: "0"),
                        marketCap = BigDecimal(kryptexJson.jsonObject["market_cap"]?.jsonPrimitive?.content ?: "0"),
                        volume = BigDecimal(gateioTickerData["quote_volume"]?.jsonPrimitive?.content ?: "0"),
                    )
                }
            }
            
            logger.warn("Failed to fetch data for $ticker: ${gateioResponse.status}")
            return null
            
        } catch (e: Exception) {
            logger.error("Error fetching data for $ticker: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Data class to hold the response from the Gate.io API.
     */
    data class CoinApiResponse(
        val price: BigDecimal,
        val marketCap: BigDecimal,
        val volume: BigDecimal,
    )
} 