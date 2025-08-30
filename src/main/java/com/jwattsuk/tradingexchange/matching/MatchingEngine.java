package com.jwattsuk.tradingexchange.matching;

import com.jwattsuk.tradingexchange.config.ExchangeConfig;
import com.jwattsuk.tradingexchange.model.Order;
import com.jwattsuk.tradingexchange.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central matching engine that manages order books for all symbols.
 * Provides thread-safe order processing and trade execution.
 */
public class MatchingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    
    private final ExchangeConfig config;
    private final Map<String, OrderBook> orderBooks;
    private final AtomicLong totalTrades;
    private final AtomicLong totalOrders;
    
    public MatchingEngine(ExchangeConfig config) {
        this.config = config;
        this.orderBooks = new ConcurrentHashMap<>();
        this.totalTrades = new AtomicLong(0);
        this.totalOrders = new AtomicLong(0);
        
        // Initialize order books for all configured symbols
        for (String symbol : config.getSymbols()) {
            orderBooks.put(symbol, new OrderBook(symbol));
            logger.info("Initialized order book for symbol: {}", symbol);
        }
    }
    
    /**
     * Processes a new order and returns the resulting trades.
     */
    public OrderResult processOrder(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            logger.warn("Rejected order for unknown symbol: {}", order.getSymbol());
            return new OrderResult(order.withReject(), List.of(), "Unknown symbol");
        }
        
        // Validate order
        String validationError = validateOrder(order);
        if (validationError != null) {
            logger.warn("Rejected order: {}", validationError);
            return new OrderResult(order.withReject(), List.of(), validationError);
        }
        
        // Process order in the order book
        List<Trade> trades = orderBook.addOrder(order);
        
        // Get the updated order from the order book
        Order updatedOrder = orderBook.getOrder(order.getOrderId());
        if (updatedOrder == null) {
            updatedOrder = order; // Fallback to original order if not found
        }
        
        // Update statistics
        totalOrders.incrementAndGet();
        totalTrades.addAndGet(trades.size());
        
        // Log order processing
        if (config.isEnableLogging()) {
            logger.info("Processed order: {} -> {} trades", updatedOrder, trades.size());
            for (Trade trade : trades) {
                logger.info("Trade executed: {}", trade);
            }
        }
        
        return new OrderResult(updatedOrder, trades, null);
    }
    
    /**
     * Cancels an order.
     */
    public boolean cancelOrder(String symbol, long orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            logger.warn("Cannot cancel order for unknown symbol: {}", symbol);
            return false;
        }
        
        boolean cancelled = orderBook.cancelOrder(orderId);
        if (cancelled && config.isEnableLogging()) {
            logger.info("Cancelled order: {}:{}", symbol, orderId);
        }
        
        return cancelled;
    }
    
    /**
     * Gets the order book snapshot for a symbol.
     */
    public OrderBook.OrderBookSnapshot getOrderBookSnapshot(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return null;
        }
        return orderBook.getSnapshot(config.getMaxOrderBookLevels());
    }
    
    /**
     * Gets the best bid and ask for a symbol.
     */
    public Quote getQuote(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return null;
        }
        
        var bestBid = orderBook.getBestBid();
        var bestAsk = orderBook.getBestAsk();
        
        return new Quote(
            symbol,
            bestBid.map(OrderBook.PriceLevel::getPrice).orElse(null),
            bestBid.map(OrderBook.PriceLevel::getQuantity).orElse(0),
            bestAsk.map(OrderBook.PriceLevel::getPrice).orElse(null),
            bestAsk.map(OrderBook.PriceLevel::getQuantity).orElse(0)
        );
    }
    
    /**
     * Validates an order before processing.
     */
    private String validateOrder(Order order) {
        // Check symbol
        if (!config.getSymbols().contains(order.getSymbol())) {
            return "Invalid symbol: " + order.getSymbol();
        }
        
        // Check quantity
        if (order.getQuantity() <= 0) {
            return "Invalid quantity: " + order.getQuantity();
        }
        
        // Check price for limit orders
        if (order.getOrderType() == Order.OrderType.LIMIT && order.getPrice() <= 0) {
            return "Invalid price for limit order: " + order.getPrice();
        }
        
        // Check client order ID
        if (order.getClientOrderId() == null || order.getClientOrderId().trim().isEmpty()) {
            return "Missing client order ID";
        }
        
        // Check client ID
        if (order.getClientId() == null || order.getClientId().trim().isEmpty()) {
            return "Missing client ID";
        }
        
        return null; // Order is valid
    }
    
    /**
     * Gets statistics about the matching engine.
     */
    public MatchingEngineStats getStats() {
        long totalBuyOrders = 0;
        long totalSellOrders = 0;
        
        for (OrderBook orderBook : orderBooks.values()) {
            totalBuyOrders += orderBook.getTotalBuyOrders();
            totalSellOrders += orderBook.getTotalSellOrders();
        }
        
        return new MatchingEngineStats(
            totalOrders.get(),
            totalTrades.get(),
            totalBuyOrders,
            totalSellOrders,
            orderBooks.size()
        );
    }
    
    public List<String> getSymbols() {
        return config.getSymbols();
    }
    
    /**
     * Gets the order book for a symbol.
     */
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }
    
    /**
     * Result of processing an order.
     */
    public static class OrderResult {
        private final Order order;
        private final List<Trade> trades;
        private final String errorMessage;
        
        public OrderResult(Order order, List<Trade> trades, String errorMessage) {
            this.order = order;
            this.trades = trades;
            this.errorMessage = errorMessage;
        }
        
        public Order getOrder() { return order; }
        public List<Trade> getTrades() { return trades; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isRejected() { return errorMessage != null; }
    }
    
    /**
     * Represents a quote (best bid/ask) for a symbol.
     */
    public static class Quote {
        private final String symbol;
        private final Double bidPrice;
        private final int bidQuantity;
        private final Double askPrice;
        private final int askQuantity;
        
        public Quote(String symbol, Double bidPrice, int bidQuantity, 
                    Double askPrice, int askQuantity) {
            this.symbol = symbol;
            this.bidPrice = bidPrice;
            this.bidQuantity = bidQuantity;
            this.askPrice = askPrice;
            this.askQuantity = askQuantity;
        }
        
        public String getSymbol() { return symbol; }
        public Double getBidPrice() { return bidPrice; }
        public int getBidQuantity() { return bidQuantity; }
        public Double getAskPrice() { return askPrice; }
        public int getAskQuantity() { return askQuantity; }
        
        public double getSpread() {
            if (bidPrice != null && askPrice != null) {
                return askPrice - bidPrice;
            }
            return 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("Quote{symbol='%s', bid=%.2f@%d, ask=%.2f@%d, spread=%.2f}", 
                symbol, bidPrice, bidQuantity, askPrice, askQuantity, getSpread());
        }
    }
    
    /**
     * Statistics about the matching engine.
     */
    public static class MatchingEngineStats {
        private final long totalOrders;
        private final long totalTrades;
        private final long totalBuyOrders;
        private final long totalSellOrders;
        private final int activeSymbols;
        
        public MatchingEngineStats(long totalOrders, long totalTrades, 
                                 long totalBuyOrders, long totalSellOrders, int activeSymbols) {
            this.totalOrders = totalOrders;
            this.totalTrades = totalTrades;
            this.totalBuyOrders = totalBuyOrders;
            this.totalSellOrders = totalSellOrders;
            this.activeSymbols = activeSymbols;
        }
        
        public long getTotalOrders() { return totalOrders; }
        public long getTotalTrades() { return totalTrades; }
        public long getTotalBuyOrders() { return totalBuyOrders; }
        public long getTotalSellOrders() { return totalSellOrders; }
        public int getActiveSymbols() { return activeSymbols; }
        
        @Override
        public String toString() {
            return String.format("MatchingEngineStats{orders=%d, trades=%d, buyOrders=%d, sellOrders=%d, symbols=%d}", 
                totalOrders, totalTrades, totalBuyOrders, totalSellOrders, activeSymbols);
        }
    }
}
