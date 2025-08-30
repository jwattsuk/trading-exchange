package com.jwattsuk.tradingexchange;

import com.jwattsuk.tradingexchange.config.ExchangeConfig;
import com.jwattsuk.tradingexchange.fix.FixAcceptor;
import com.jwattsuk.tradingexchange.matching.MatchingEngine;
import com.jwattsuk.tradingexchange.marketdata.MarketDataPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the Mock Trading Exchange Gateway.
 * Initializes FIX acceptor, matching engine, and market data publisher.
 */
public class MockTradingExchange {
    private static final Logger logger = LoggerFactory.getLogger(MockTradingExchange.class);
    
    private final ExchangeConfig config;
    private final FixAcceptor fixAcceptor;
    private final MatchingEngine matchingEngine;
    private final MarketDataPublisher marketDataPublisher;
    private final ScheduledExecutorService scheduler;
    
    public MockTradingExchange(ExchangeConfig config) {
        this.config = config;
        this.matchingEngine = new MatchingEngine(config);
        this.marketDataPublisher = new MarketDataPublisher(config, matchingEngine);
        this.fixAcceptor = new FixAcceptor(config, matchingEngine, marketDataPublisher);
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void start() throws Exception {
        logger.info("Starting Mock Trading Exchange Gateway...");
        
        // Start market data publisher
        marketDataPublisher.start();
        
        // Start FIX acceptor
        fixAcceptor.start();
        
        // Schedule periodic market data updates
        scheduler.scheduleAtFixedRate(
            () -> marketDataPublisher.publishMarketData(),
            0, 
            config.getMarketDataIntervalMs(), 
            TimeUnit.MILLISECONDS
        );
        
        logger.info("Mock Trading Exchange Gateway started successfully");
        logger.info("FIX Acceptor listening on port: {}", config.getFixPort());
        logger.info("Market Data Publisher listening on port: {}", config.getMarketDataPort());
    }
    
    public void stop() {
        logger.info("Stopping Mock Trading Exchange Gateway...");
        
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            fixAcceptor.stop();
            marketDataPublisher.stop();
            
            logger.info("Mock Trading Exchange Gateway stopped successfully");
        } catch (InterruptedException e) {
            logger.error("Error during shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        try {
            ExchangeConfig config = ExchangeConfig.loadFromProperties();
            MockTradingExchange exchange = new MockTradingExchange(config);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(exchange::stop));
            
            exchange.start();
            
            // Keep the application running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start Mock Trading Exchange", e);
            System.exit(1);
        }
    }
}
