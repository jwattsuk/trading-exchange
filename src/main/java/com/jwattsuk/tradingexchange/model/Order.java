package com.jwattsuk.tradingexchange.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a trading order in the exchange.
 * Immutable after creation to ensure thread safety.
 */
public class Order {
    private static final AtomicLong orderIdGenerator = new AtomicLong(1);
    
    private final long orderId;
    private final String clientOrderId;
    private final String symbol;
    private final Side side;
    private final OrderType orderType;
    private final double price;
    private final int quantity;
    private final int remainingQuantity;
    private final String clientId;
    private final LocalDateTime timestamp;
    private final OrderStatus status;
    
    public enum Side {
        BUY, SELL
    }
    
    public enum OrderType {
        MARKET, LIMIT, STOP, STOP_LIMIT
    }
    
    public enum OrderStatus {
        NEW, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED, PENDING_CANCEL
    }
    
    public Order(String clientOrderId, String symbol, Side side, OrderType orderType, 
                double price, int quantity, String clientId) {
        this.orderId = orderIdGenerator.getAndIncrement();
        this.clientOrderId = clientOrderId;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.clientId = clientId;
        this.timestamp = LocalDateTime.now();
        this.status = OrderStatus.NEW;
    }
    
    // Private constructor for creating modified orders
    private Order(Order original, int remainingQuantity, OrderStatus status) {
        this.orderId = original.orderId;
        this.clientOrderId = original.clientOrderId;
        this.symbol = original.symbol;
        this.side = original.side;
        this.orderType = original.orderType;
        this.price = original.price;
        this.quantity = original.quantity;
        this.remainingQuantity = remainingQuantity;
        this.clientId = original.clientId;
        this.timestamp = original.timestamp;
        this.status = status;
    }
    
    // Factory methods for creating modified orders
    public Order withPartialFill(int fillQuantity) {
        int newRemaining = Math.max(0, this.remainingQuantity - fillQuantity);
        OrderStatus newStatus = newRemaining == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        return new Order(this, newRemaining, newStatus);
    }
    
    public Order withCancel() {
        return new Order(this, this.remainingQuantity, OrderStatus.CANCELLED);
    }
    
    public Order withReject() {
        return new Order(this, this.remainingQuantity, OrderStatus.REJECTED);
    }
    
    // Getters
    public long getOrderId() { return orderId; }
    public String getClientOrderId() { return clientOrderId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public int getFilledQuantity() { return quantity - remainingQuantity; }
    public String getClientId() { return clientId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public OrderStatus getStatus() { return status; }
    
    public boolean isFilled() { return status == OrderStatus.FILLED; }
    public boolean isCancelled() { return status == OrderStatus.CANCELLED; }
    public boolean isRejected() { return status == OrderStatus.REJECTED; }
    public boolean isActive() { return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return orderId == order.orderId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", clientOrderId='" + clientOrderId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side=" + side +
                ", orderType=" + orderType +
                ", price=" + price +
                ", quantity=" + quantity +
                ", remainingQuantity=" + remainingQuantity +
                ", clientId='" + clientId + '\'' +
                ", status=" + status +
                '}';
    }
}
