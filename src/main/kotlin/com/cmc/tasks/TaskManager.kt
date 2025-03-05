package com.cmc.tasks

import com.cmc.database.services.CoinService
import com.cmc.database.services.CoinStatsService
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Manages background tasks for the application.
 */
class TaskManager(
    private val coinService: CoinService,
    private val coinStatsService: CoinStatsService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var coinDataFetchTask: CoinDataFetchTask? = null
    
    /**
     * Starts all background tasks.
     */
    fun startTasks() {
        logger.info("Starting background tasks")
        startCoinDataFetchTask()
    }
    
    /**
     * Stops all background tasks.
     */
    fun stopTasks() {
        logger.info("Stopping background tasks")
        stopCoinDataFetchTask()
    }
    
    /**
     * Starts the coin data fetch task.
     * 
     * @param fetchInterval The interval between fetches, defaults to 10 minutes.
     */
    fun startCoinDataFetchTask(fetchInterval: Duration = Duration.ofMinutes(10)) {
        logger.info("Starting coin data fetch task")
        
        coinDataFetchTask = CoinDataFetchTask(
            coinService = coinService,
            coinStatsService = coinStatsService,
            fetchInterval = fetchInterval
        ).also { it.start() }
    }
    
    /**
     * Stops the coin data fetch task.
     */
    fun stopCoinDataFetchTask() {
        logger.info("Stopping coin data fetch task")
        coinDataFetchTask?.stop()
        coinDataFetchTask = null
    }
} 