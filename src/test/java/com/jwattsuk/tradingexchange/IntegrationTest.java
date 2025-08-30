package com.jwattsuk.tradingexchange;

import com.jwattsuk.tradingexchange.config.ExchangeConfig;
import com.jwattsuk.tradingexchange.matching.MatchingEngine;
import com.jwattsuk.tradingexchange.model.Order;
import com.jwattsuk.tradingexchange.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

/**
 * Integration test demonstrating the full trading exchange functionality.
 */
class IntegrationTest {
    
    private MockTradingExchange exchange;
    private MatchingEngine matchingEngine;
    
    @BeforeEach
    void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("fix.port", "5001");
        props.setProperty("marketdata.port", "5002");
        props.setProperty("trading.symbols", "AAPL,MSFT");
        props.setProperty("trading.enable.logging", "false");
        
        ExchangeConfig config = new ExchangeConfig(props);
        exchange = new MockTradingExchange(config);
        matchingEngine = new MatchingEngine(config);
    }
    
    @Test
    void testCompleteTradingScenario() {
        // Test 1: Place a sell order
        Order sellOrder = new Order("SELL001", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        MatchingEngine.OrderResult sellResult = matchingEngine.processOrder(sellOrder);
        
        assertFalse(sellResult.isRejected());
        assertTrue(sellResult.getTrades().isEmpty());
        assertEquals(Order.OrderStatus.NEW, sellResult.getOrder().getStatus());
        
        // Test 2: Place a buy order that should match
        Order buyOrder = new Order("BUY001", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        MatchingEngine.OrderResult buyResult = matchingEngine.processOrder(buyOrder);
        
        assertFalse(buyResult.isRejected());
        assertEquals(1, buyResult.getTrades().size());
        
        Trade trade = buyResult.getTrades().get(0);
        assertEquals(150.00, trade.getPrice());
        assertEquals(100, trade.getQuantity());
        assertEquals("AAPL", trade.getSymbol());
        assertEquals(buyOrder.getOrderId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getOrderId(), trade.getSellOrderId());
        
        // Test 3: Verify both orders are filled
        assertTrue(buyResult.getOrder().isFilled());
        
        // Get the updated sell order from the order book
        Order updatedSellOrder = matchingEngine.getOrderBook("AAPL").getOrder(sellOrder.getOrderId());
        assertTrue(updatedSellOrder.isFilled());
        
        // Test 4: Check order book is empty
        var snapshot = matchingEngine.getOrderBookSnapshot("AAPL");
        assertNotNull(snapshot);
        assertTrue(snapshot.getBids().isEmpty());
        assertTrue(snapshot.getAsks().isEmpty());
        
        // Test 5: Place orders for different symbol
        Order msftOrder = new Order("MSFT001", "MSFT", Order.Side.BUY, Order.OrderType.LIMIT, 200.00, 50, "CLIENT3");
        MatchingEngine.OrderResult msftResult = matchingEngine.processOrder(msftOrder);
        
        assertFalse(msftResult.isRejected());
        assertTrue(msftResult.getTrades().isEmpty());
        
        // Test 6: Check statistics
        var stats = matchingEngine.getStats();
        assertEquals(3, stats.getTotalOrders());
        assertEquals(1, stats.getTotalTrades());
        assertEquals(1, stats.getTotalBuyOrders()); // MSFT order
        assertEquals(0, stats.getTotalSellOrders());
        assertEquals(2, stats.getActiveSymbols());
    }
    
    @Test
    void testPartialFills() {
        // Place a sell order for 100 shares
        Order sellOrder = new Order("SELL001", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        matchingEngine.processOrder(sellOrder);
        
        // Place a buy order for 150 shares (should partially fill)
        Order buyOrder = new Order("BUY001", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 150, "CLIENT2");
        MatchingEngine.OrderResult buyResult = matchingEngine.processOrder(buyOrder);
        
        assertEquals(1, buyResult.getTrades().size());
        Trade trade = buyResult.getTrades().get(0);
        assertEquals(100, trade.getQuantity()); // Only 100 shares traded
        
        // Buy order should be partially filled
        assertEquals(Order.OrderStatus.PARTIALLY_FILLED, buyResult.getOrder().getStatus());
        assertEquals(50, buyResult.getOrder().getRemainingQuantity());
        assertEquals(100, buyResult.getOrder().getFilledQuantity());
        
        // Sell order should be filled
        Order updatedSellOrder = matchingEngine.getOrderBook("AAPL").getOrder(sellOrder.getOrderId());
        assertTrue(updatedSellOrder.isFilled());
        
        // Check order book has remaining buy order
        var snapshot = matchingEngine.getOrderBookSnapshot("AAPL");
        assertEquals(1, snapshot.getBids().size());
        assertEquals(50, snapshot.getBids().get(0).getQuantity());
        assertTrue(snapshot.getAsks().isEmpty());
    }
    
    @Test
    void testPriceTimePriority() {
        // Place sell orders at same price, different times
        Order sellOrder1 = new Order("SELL001", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        Order sellOrder2 = new Order("SELL002", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        
        matchingEngine.processOrder(sellOrder1);
        matchingEngine.processOrder(sellOrder2);
        
        // Place buy order that should match first sell order
        Order buyOrder = new Order("BUY001", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT3");
        MatchingEngine.OrderResult buyResult = matchingEngine.processOrder(buyOrder);
        
        assertEquals(1, buyResult.getTrades().size());
        Trade trade = buyResult.getTrades().get(0);
        assertEquals(sellOrder1.getOrderId(), trade.getSellOrderId()); // First order should be matched
        
        // Second sell order should still be in the book
        var snapshot = matchingEngine.getOrderBookSnapshot("AAPL");
        assertEquals(1, snapshot.getAsks().size());
        assertEquals(100, snapshot.getAsks().get(0).getQuantity());
    }
    
    @Test
    void testOrderCancellation() {
        // Place an order
        Order order = new Order("ORDER001", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        matchingEngine.processOrder(order);
        
        // Verify order is in the book
        var snapshot = matchingEngine.getOrderBookSnapshot("AAPL");
        assertEquals(1, snapshot.getBids().size());
        
        // Cancel the order
        boolean cancelled = matchingEngine.cancelOrder("AAPL", order.getOrderId());
        assertTrue(cancelled);
        
        // Verify order is removed from the book
        snapshot = matchingEngine.getOrderBookSnapshot("AAPL");
        assertTrue(snapshot.getBids().isEmpty());
        
        // Try to cancel again (should fail)
        boolean cancelledAgain = matchingEngine.cancelOrder("AAPL", order.getOrderId());
        assertFalse(cancelledAgain);
    }
    
    @Test
    void testQuoteGeneration() {
        // Place orders to create a spread
        Order buyOrder = new Order("BUY001", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 149.00, 100, "CLIENT1");
        Order sellOrder = new Order("SELL001", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 151.00, 100, "CLIENT2");
        
        matchingEngine.processOrder(buyOrder);
        matchingEngine.processOrder(sellOrder);
        
        // Get quote
        var quote = matchingEngine.getQuote("AAPL");
        assertNotNull(quote);
        assertEquals("AAPL", quote.getSymbol());
        assertEquals(149.00, quote.getBidPrice());
        assertEquals(151.00, quote.getAskPrice());
        assertEquals(2.00, quote.getSpread());
        assertEquals(100, quote.getBidQuantity());
        assertEquals(100, quote.getAskQuantity());
    }
}
