package com.jwattsuk.tradingexchange.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Configuration for the Mock Trading Exchange.
 * Loads settings from exchange.properties file.
 */
public class ExchangeConfig {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeConfig.class);
    
    // FIX Configuration
    private final int fixPort;
    private final String fixConfigFile;
    private final String senderCompId;
    private final String targetCompId;
    
    // Market Data Configuration
    private final int marketDataPort;
    private final long marketDataIntervalMs;
    
    // Trading Configuration
    private final List<String> symbols;
    private final int maxOrderBookLevels;
    private final boolean enableLogging;
    
    // Performance Configuration
    private final int threadPoolSize;
    private final long heartbeatIntervalMs;
    
    public ExchangeConfig(Properties props) {
        this.fixPort = Integer.parseInt(props.getProperty("fix.port", "5001"));
        this.fixConfigFile = props.getProperty("fix.config.file", "quickfix.cfg");
        this.senderCompId = props.getProperty("fix.sender.comp.id", "EXCHANGE");
        this.targetCompId = props.getProperty("fix.target.comp.id", "CLIENT");
        
        this.marketDataPort = Integer.parseInt(props.getProperty("marketdata.port", "5002"));
        this.marketDataIntervalMs = Long.parseLong(props.getProperty("marketdata.interval.ms", "100"));
        
        String symbolsStr = props.getProperty("trading.symbols", "AAPL,MSFT,GOOGL,TSLA");
        this.symbols = Arrays.asList(symbolsStr.split(","));
        this.maxOrderBookLevels = Integer.parseInt(props.getProperty("trading.max.orderbook.levels", "10"));
        this.enableLogging = Boolean.parseBoolean(props.getProperty("trading.enable.logging", "true"));
        
        this.threadPoolSize = Integer.parseInt(props.getProperty("performance.thread.pool.size", "4"));
        this.heartbeatIntervalMs = Long.parseLong(props.getProperty("fix.heartbeat.interval.ms", "30000"));
    }
    
    public static ExchangeConfig loadFromProperties() throws IOException {
        Properties props = new Properties();
        
        try (InputStream input = ExchangeConfig.class.getClassLoader()
                .getResourceAsStream("exchange.properties")) {
            if (input != null) {
                props.load(input);
                logger.info("Loaded configuration from exchange.properties");
            } else {
                logger.warn("exchange.properties not found, using default configuration");
            }
        }
        
        return new ExchangeConfig(props);
    }
    
    // Getters
    public int getFixPort() { return fixPort; }
    public String getFixConfigFile() { return fixConfigFile; }
    public String getSenderCompId() { return senderCompId; }
    public String getTargetCompId() { return targetCompId; }
    public int getMarketDataPort() { return marketDataPort; }
    public long getMarketDataIntervalMs() { return marketDataIntervalMs; }
    public List<String> getSymbols() { return symbols; }
    public int getMaxOrderBookLevels() { return maxOrderBookLevels; }
    public boolean isEnableLogging() { return enableLogging; }
    public int getThreadPoolSize() { return threadPoolSize; }
    public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    
    @Override
    public String toString() {
        return "ExchangeConfig{" +
                "fixPort=" + fixPort +
                ", fixConfigFile='" + fixConfigFile + '\'' +
                ", senderCompId='" + senderCompId + '\'' +
                ", targetCompId='" + targetCompId + '\'' +
                ", marketDataPort=" + marketDataPort +
                ", marketDataIntervalMs=" + marketDataIntervalMs +
                ", symbols=" + symbols +
                ", maxOrderBookLevels=" + maxOrderBookLevels +
                ", enableLogging=" + enableLogging +
                ", threadPoolSize=" + threadPoolSize +
                ", heartbeatIntervalMs=" + heartbeatIntervalMs +
                '}';
    }
}
