package com.jwattsuk.tradingexchange.matching;

import com.jwattsuk.tradingexchange.model.Order;
import com.jwattsuk.tradingexchange.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains an order book for a single symbol with price-time priority.
 * Uses ConcurrentSkipListMap for efficient price level management.
 */
public class OrderBook {
    private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);
    
    private final String symbol;
    
    // Buy orders: price -> list of orders (highest price first)
    private final ConcurrentSkipListMap<Double, List<Order>> buyOrders;
    
    // Sell orders: price -> list of orders (lowest price first)
    private final ConcurrentSkipListMap<Double, List<Order>> sellOrders;
    
    // Order lookup by ID for fast cancellation
    private final Map<Long, Order> orderById;
    
    // Price level tracking
    private final AtomicInteger totalBuyOrders;
    private final AtomicInteger totalSellOrders;
    
    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new ConcurrentSkipListMap<>(Collections.reverseOrder()); // Highest price first
        this.sellOrders = new ConcurrentSkipListMap<>(); // Lowest price first
        this.orderById = new ConcurrentHashMap<>();
        this.totalBuyOrders = new AtomicInteger(0);
        this.totalSellOrders = new AtomicInteger(0);
    }
    
    /**
     * Adds an order to the book and attempts to match it.
     * Returns list of trades that occurred.
     */
    public synchronized List<Trade> addOrder(Order order) {
        List<Trade> trades = new ArrayList<>();
        
        if (order.getSide() == Order.Side.BUY) {
            trades.addAll(matchBuyOrder(order));
            // Calculate total filled quantity from trades
            int totalFilled = trades.stream().mapToInt(Trade::getQuantity).sum();
            Order updatedOrder = totalFilled > 0 ? order.withPartialFill(totalFilled) : order;
            
            if (updatedOrder.getRemainingQuantity() > 0) {
                addToBook(updatedOrder, buyOrders);
                totalBuyOrders.incrementAndGet();
            }
            // Store updated order for lookup
            orderById.put(updatedOrder.getOrderId(), updatedOrder);
        } else {
            trades.addAll(matchSellOrder(order));
            // Calculate total filled quantity from trades
            int totalFilled = trades.stream().mapToInt(Trade::getQuantity).sum();
            Order updatedOrder = totalFilled > 0 ? order.withPartialFill(totalFilled) : order;
            
            if (updatedOrder.getRemainingQuantity() > 0) {
                addToBook(updatedOrder, sellOrders);
                totalSellOrders.incrementAndGet();
            }
            // Store updated order for lookup
            orderById.put(updatedOrder.getOrderId(), updatedOrder);
        }
        
        return trades;
    }
    
    /**
     * Cancels an order from the book.
     */
    public synchronized boolean cancelOrder(long orderId) {
        Order order = orderById.get(orderId);
        if (order == null || !order.isActive()) {
            return false;
        }
        
        boolean removed = false;
        if (order.getSide() == Order.Side.BUY) {
            removed = removeFromBook(order, buyOrders);
            if (removed) totalBuyOrders.decrementAndGet();
        } else {
            removed = removeFromBook(order, sellOrders);
            if (removed) totalSellOrders.decrementAndGet();
        }
        
        if (removed) {
            orderById.remove(orderId);
        }
        
        return removed;
    }
    
    /**
     * Attempts to match a buy order against existing sell orders.
     */
    private List<Trade> matchBuyOrder(Order buyOrder) {
        List<Trade> trades = new ArrayList<>();
        int remainingQuantity = buyOrder.getRemainingQuantity();
        
        // For market orders, set price to highest possible to match all available liquidity
        double buyPrice = buyOrder.getOrderType() == Order.OrderType.MARKET ? 
            Double.MAX_VALUE : buyOrder.getPrice();
        
        // Try to match against sell orders (lowest price first)
        for (Map.Entry<Double, List<Order>> entry : sellOrders.entrySet()) {
            double sellPrice = entry.getKey();
            
            // Stop if buy price is lower than sell price
            if (buyPrice < sellPrice) {
                break;
            }
            
            List<Order> sellOrdersAtPrice = entry.getValue();
            Iterator<Order> iterator = sellOrdersAtPrice.iterator();
            
            while (iterator.hasNext() && remainingQuantity > 0) {
                Order sellOrder = iterator.next();
                
                if (!sellOrder.isActive()) {
                    iterator.remove();
                    continue;
                }
                
                int matchQuantity = Math.min(remainingQuantity, sellOrder.getRemainingQuantity());
                double tradePrice = sellPrice; // Price of the resting order
                
                // Create trade
                Trade trade = new Trade(
                    buyOrder.getOrderId(),
                    sellOrder.getOrderId(),
                    symbol,
                    tradePrice,
                    matchQuantity,
                    buyOrder.getClientId(),
                    sellOrder.getClientId()
                );
                trades.add(trade);
                
                // Update quantities
                remainingQuantity -= matchQuantity;
                
                // Update sell order
                Order updatedSellOrder = sellOrder.withPartialFill(matchQuantity);
                orderById.put(updatedSellOrder.getOrderId(), updatedSellOrder);
                
                if (updatedSellOrder.isFilled()) {
                    iterator.remove();
                    totalSellOrders.decrementAndGet();
                } else {
                    // Replace the order in the list
                    int index = sellOrdersAtPrice.indexOf(sellOrder);
                    if (index >= 0) {
                        sellOrdersAtPrice.set(index, updatedSellOrder);
                    }
                }
            }
            
            // Remove empty price levels
            if (sellOrdersAtPrice.isEmpty()) {
                sellOrders.remove(sellPrice);
            }
            
            if (remainingQuantity == 0) {
                break;
            }
        }
        
        return trades;
    }
    
    /**
     * Attempts to match a sell order against existing buy orders.
     */
    private List<Trade> matchSellOrder(Order sellOrder) {
        List<Trade> trades = new ArrayList<>();
        int remainingQuantity = sellOrder.getRemainingQuantity();
        
        // For market orders, set price to lowest possible to match all available liquidity
        double sellPrice = sellOrder.getOrderType() == Order.OrderType.MARKET ? 
            0.0 : sellOrder.getPrice();
        
        // Try to match against buy orders (highest price first)
        for (Map.Entry<Double, List<Order>> entry : buyOrders.entrySet()) {
            double buyPrice = entry.getKey();
            
            // Stop if sell price is higher than buy price
            if (sellPrice > buyPrice) {
                break;
            }
            
            List<Order> buyOrdersAtPrice = entry.getValue();
            Iterator<Order> iterator = buyOrdersAtPrice.iterator();
            
            while (iterator.hasNext() && remainingQuantity > 0) {
                Order buyOrder = iterator.next();
                
                if (!buyOrder.isActive()) {
                    iterator.remove();
                    continue;
                }
                
                int matchQuantity = Math.min(remainingQuantity, buyOrder.getRemainingQuantity());
                double tradePrice = buyPrice; // Price of the resting order
                
                // Create trade
                Trade trade = new Trade(
                    buyOrder.getOrderId(),
                    sellOrder.getOrderId(),
                    symbol,
                    tradePrice,
                    matchQuantity,
                    buyOrder.getClientId(),
                    sellOrder.getClientId()
                );
                trades.add(trade);
                
                // Update quantities
                remainingQuantity -= matchQuantity;
                
                // Update buy order
                Order updatedBuyOrder = buyOrder.withPartialFill(matchQuantity);
                orderById.put(updatedBuyOrder.getOrderId(), updatedBuyOrder);
                
                if (updatedBuyOrder.isFilled()) {
                    iterator.remove();
                    totalBuyOrders.decrementAndGet();
                } else {
                    // Replace the order in the list
                    int index = buyOrdersAtPrice.indexOf(buyOrder);
                    if (index >= 0) {
                        buyOrdersAtPrice.set(index, updatedBuyOrder);
                    }
                }
            }
            
            // Remove empty price levels
            if (buyOrdersAtPrice.isEmpty()) {
                buyOrders.remove(buyPrice);
            }
            
            if (remainingQuantity == 0) {
                break;
            }
        }
        
        return trades;
    }
    
    private void addToBook(Order order, ConcurrentSkipListMap<Double, List<Order>> orders) {
        orders.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
    }
    
    private boolean removeFromBook(Order order, ConcurrentSkipListMap<Double, List<Order>> orders) {
        List<Order> ordersAtPrice = orders.get(order.getPrice());
        if (ordersAtPrice != null) {
            boolean removed = ordersAtPrice.remove(order);
            if (ordersAtPrice.isEmpty()) {
                orders.remove(order.getPrice());
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Gets the best bid price and quantity.
     */
    public Optional<PriceLevel> getBestBid() {
        Map.Entry<Double, List<Order>> entry = buyOrders.firstEntry();
        if (entry != null && !entry.getValue().isEmpty()) {
            double price = entry.getKey();
            int quantity = entry.getValue().stream()
                .mapToInt(Order::getRemainingQuantity)
                .sum();
            return Optional.of(new PriceLevel(price, quantity));
        }
        return Optional.empty();
    }
    
    /**
     * Gets the best ask price and quantity.
     */
    public Optional<PriceLevel> getBestAsk() {
        Map.Entry<Double, List<Order>> entry = sellOrders.firstEntry();
        if (entry != null && !entry.getValue().isEmpty()) {
            double price = entry.getKey();
            int quantity = entry.getValue().stream()
                .mapToInt(Order::getRemainingQuantity)
                .sum();
            return Optional.of(new PriceLevel(price, quantity));
        }
        return Optional.empty();
    }
    
    /**
     * Gets the order book snapshot for market data.
     */
    public OrderBookSnapshot getSnapshot(int maxLevels) {
        List<PriceLevel> bids = new ArrayList<>();
        List<PriceLevel> asks = new ArrayList<>();
        
        // Get top bid levels
        int bidCount = 0;
        for (Map.Entry<Double, List<Order>> entry : buyOrders.entrySet()) {
            if (bidCount >= maxLevels) break;
            double price = entry.getKey();
            int quantity = entry.getValue().stream()
                .mapToInt(Order::getRemainingQuantity)
                .sum();
            bids.add(new PriceLevel(price, quantity));
            bidCount++;
        }
        
        // Get top ask levels
        int askCount = 0;
        for (Map.Entry<Double, List<Order>> entry : sellOrders.entrySet()) {
            if (askCount >= maxLevels) break;
            double price = entry.getKey();
            int quantity = entry.getValue().stream()
                .mapToInt(Order::getRemainingQuantity)
                .sum();
            asks.add(new PriceLevel(price, quantity));
            askCount++;
        }
        
        return new OrderBookSnapshot(symbol, bids, asks);
    }
    
    public String getSymbol() { return symbol; }
    public int getTotalBuyOrders() { return totalBuyOrders.get(); }
    public int getTotalSellOrders() { return totalSellOrders.get(); }
    
    /**
     * Gets an order by its ID.
     */
    public Order getOrder(long orderId) {
        return orderById.get(orderId);
    }
    
    /**
     * Represents a price level in the order book.
     */
    public static class PriceLevel {
        private final double price;
        private final int quantity;
        
        public PriceLevel(double price, int quantity) {
            this.price = price;
            this.quantity = quantity;
        }
        
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        
        @Override
        public String toString() {
            return String.format("%.2f@%d", price, quantity);
        }
    }
    
    /**
     * Represents a snapshot of the order book.
     */
    public static class OrderBookSnapshot {
        private final String symbol;
        private final List<PriceLevel> bids;
        private final List<PriceLevel> asks;
        private final long timestamp;
        
        public OrderBookSnapshot(String symbol, List<PriceLevel> bids, List<PriceLevel> asks) {
            this.symbol = symbol;
            this.bids = new ArrayList<>(bids);
            this.asks = new ArrayList<>(asks);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getSymbol() { return symbol; }
        public List<PriceLevel> getBids() { return bids; }
        public List<PriceLevel> getAsks() { return asks; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("OrderBookSnapshot{symbol='%s', bids=%s, asks=%s}", 
                symbol, bids, asks);
        }
    }
}
