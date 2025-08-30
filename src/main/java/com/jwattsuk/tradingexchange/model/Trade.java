package com.jwattsuk.tradingexchange.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a trade execution between two orders.
 */
public class Trade {
    private static final AtomicLong tradeIdGenerator = new AtomicLong(1);
    
    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final LocalDateTime timestamp;
    private final String buyClientId;
    private final String sellClientId;
    
    public Trade(long buyOrderId, long sellOrderId, String symbol, double price, 
                int quantity, String buyClientId, String sellClientId) {
        this.tradeId = tradeIdGenerator.getAndIncrement();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
        this.buyClientId = buyClientId;
        this.sellClientId = sellClientId;
    }
    
    // Getters
    public long getTradeId() { return tradeId; }
    public long getBuyOrderId() { return buyOrderId; }
    public long getSellOrderId() { return sellOrderId; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getBuyClientId() { return buyClientId; }
    public String getSellClientId() { return sellClientId; }
    
    @Override
    public String toString() {
        return "Trade{" +
                "tradeId=" + tradeId +
                ", buyOrderId=" + buyOrderId +
                ", sellOrderId=" + sellOrderId +
                ", symbol='" + symbol + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
