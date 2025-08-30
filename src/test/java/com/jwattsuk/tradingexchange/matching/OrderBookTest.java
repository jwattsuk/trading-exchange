package com.jwattsuk.tradingexchange.matching;

import com.jwattsuk.tradingexchange.model.Order;
import com.jwattsuk.tradingexchange.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class OrderBookTest {
    
    private OrderBook orderBook;
    
    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("AAPL");
    }
    
    @Test
    void testAddBuyOrder() {
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        
        List<Trade> trades = orderBook.addOrder(buyOrder);
        
        assertTrue(trades.isEmpty());
        assertEquals(1, orderBook.getTotalBuyOrders());
        assertEquals(0, orderBook.getTotalSellOrders());
        
        var bestBid = orderBook.getBestBid();
        assertTrue(bestBid.isPresent());
        assertEquals(150.00, bestBid.get().getPrice());
        assertEquals(100, bestBid.get().getQuantity());
    }
    
    @Test
    void testAddSellOrder() {
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 151.00, 100, "CLIENT2");
        
        List<Trade> trades = orderBook.addOrder(sellOrder);
        
        assertTrue(trades.isEmpty());
        assertEquals(0, orderBook.getTotalBuyOrders());
        assertEquals(1, orderBook.getTotalSellOrders());
        
        var bestAsk = orderBook.getBestAsk();
        assertTrue(bestAsk.isPresent());
        assertEquals(151.00, bestAsk.get().getPrice());
        assertEquals(100, bestAsk.get().getQuantity());
    }
    
    @Test
    void testMatchingBuyOrder() {
        // Add a sell order first
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        orderBook.addOrder(sellOrder);
        
        // Add a buy order that should match
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        List<Trade> trades = orderBook.addOrder(buyOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(150.00, trade.getPrice());
        assertEquals(100, trade.getQuantity());
        assertEquals(buyOrder.getOrderId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getOrderId(), trade.getSellOrderId());
        
        // Both orders should be filled
        assertEquals(0, orderBook.getTotalBuyOrders());
        assertEquals(0, orderBook.getTotalSellOrders());
    }
    
    @Test
    void testMatchingSellOrder() {
        // Add a buy order first
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        orderBook.addOrder(buyOrder);
        
        // Add a sell order that should match
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        List<Trade> trades = orderBook.addOrder(sellOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(150.00, trade.getPrice());
        assertEquals(100, trade.getQuantity());
        assertEquals(buyOrder.getOrderId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getOrderId(), trade.getSellOrderId());
        
        // Both orders should be filled
        assertEquals(0, orderBook.getTotalBuyOrders());
        assertEquals(0, orderBook.getTotalSellOrders());
    }
    
    @Test
    void testPartialFill() {
        // Add a sell order
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        orderBook.addOrder(sellOrder);
        
        // Add a buy order for more quantity
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 150, "CLIENT1");
        List<Trade> trades = orderBook.addOrder(buyOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(100, trade.getQuantity());
        
        // Sell order should be filled, buy order should be partially filled
        assertEquals(1, orderBook.getTotalBuyOrders());
        assertEquals(0, orderBook.getTotalSellOrders());
        
        var bestBid = orderBook.getBestBid();
        assertTrue(bestBid.isPresent());
        assertEquals(50, bestBid.get().getQuantity());
    }
    
    @Test
    void testPriceTimePriority() {
        // Add sell orders at same price, different times
        Order sellOrder1 = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        Order sellOrder2 = new Order("SELL2", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT3");
        
        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2);
        
        // Add buy order that should match first sell order
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        List<Trade> trades = orderBook.addOrder(buyOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(sellOrder1.getOrderId(), trade.getSellOrderId()); // First order should be matched
        
        // Second sell order should still be in the book
        assertEquals(0, orderBook.getTotalBuyOrders());
        assertEquals(1, orderBook.getTotalSellOrders());
    }
    
    @Test
    void testCancelOrder() {
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        orderBook.addOrder(buyOrder);
        
        assertEquals(1, orderBook.getTotalBuyOrders());
        
        boolean cancelled = orderBook.cancelOrder(buyOrder.getOrderId());
        assertTrue(cancelled);
        assertEquals(0, orderBook.getTotalBuyOrders());
        
        // Try to cancel again
        boolean cancelledAgain = orderBook.cancelOrder(buyOrder.getOrderId());
        assertFalse(cancelledAgain);
    }
    
    @Test
    void testOrderBookSnapshot() {
        // Add some orders
        Order buyOrder1 = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 149.00, 100, "CLIENT1");
        Order buyOrder2 = new Order("BUY2", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 148.00, 50, "CLIENT2");
        Order sellOrder1 = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 151.00, 100, "CLIENT3");
        Order sellOrder2 = new Order("SELL2", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 152.00, 50, "CLIENT4");
        
        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2);
        
        OrderBook.OrderBookSnapshot snapshot = orderBook.getSnapshot(5);
        
        assertEquals("AAPL", snapshot.getSymbol());
        assertEquals(2, snapshot.getBids().size());
        assertEquals(2, snapshot.getAsks().size());
        
        // Check price ordering (bids should be highest first, asks lowest first)
        assertEquals(149.00, snapshot.getBids().get(0).getPrice());
        assertEquals(148.00, snapshot.getBids().get(1).getPrice());
        assertEquals(151.00, snapshot.getAsks().get(0).getPrice());
        assertEquals(152.00, snapshot.getAsks().get(1).getPrice());
    }
    
    @Test
    void testMarketOrderBuy() {
        // Add a sell order first
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        orderBook.addOrder(sellOrder);
        
        // Add a market buy order that should match
        Order marketBuyOrder = new Order("MARKET_BUY1", "AAPL", Order.Side.BUY, Order.OrderType.MARKET, 0.0, 50, "CLIENT1");
        List<Trade> trades = orderBook.addOrder(marketBuyOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(150.00, trade.getPrice()); // Should match at the ask price
        assertEquals(50, trade.getQuantity());
        
        // Market order should be filled, sell order should be partially filled
        assertEquals(0, orderBook.getTotalBuyOrders());
        assertEquals(1, orderBook.getTotalSellOrders());
    }
    
    @Test
    void testMarketOrderSell() {
        // Add a buy order first
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        orderBook.addOrder(buyOrder);
        
        // Add a market sell order that should match
        Order marketSellOrder = new Order("MARKET_SELL1", "AAPL", Order.Side.SELL, Order.OrderType.MARKET, 0.0, 50, "CLIENT2");
        List<Trade> trades = orderBook.addOrder(marketSellOrder);
        
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(150.00, trade.getPrice()); // Should match at the bid price
        assertEquals(50, trade.getQuantity());
        
        // Market order should be filled, buy order should be partially filled
        assertEquals(1, orderBook.getTotalBuyOrders());
        assertEquals(0, orderBook.getTotalSellOrders());
    }
    
    @Test
    void testCancelNonExistentOrder() {
        boolean cancelled = orderBook.cancelOrder(999999L);
        assertFalse(cancelled);
    }
    
    @Test
    void testCancelAlreadyFilledOrder() {
        // Add orders that will match
        Order sellOrder = new Order("SELL1", "AAPL", Order.Side.SELL, Order.OrderType.LIMIT, 150.00, 100, "CLIENT2");
        Order buyOrder = new Order("BUY1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 100, "CLIENT1");
        
        orderBook.addOrder(sellOrder);
        orderBook.addOrder(buyOrder); // This will fill both orders
        
        // Try to cancel the filled order
        boolean cancelled = orderBook.cancelOrder(buyOrder.getOrderId());
        assertFalse(cancelled);
    }
    
    @Test
    void testEmptyOrderBook() {
        var bestBid = orderBook.getBestBid();
        var bestAsk = orderBook.getBestAsk();
        
        assertTrue(bestBid.isEmpty());
        assertTrue(bestAsk.isEmpty());
        
        OrderBook.OrderBookSnapshot snapshot = orderBook.getSnapshot(5);
        assertEquals("AAPL", snapshot.getSymbol());
        assertTrue(snapshot.getBids().isEmpty());
        assertTrue(snapshot.getAsks().isEmpty());
    }
    
    @Test
    void testZeroQuantityOrder() {
        Order zeroQtyOrder = new Order("ZERO1", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.00, 0, "CLIENT1");
        List<Trade> trades = orderBook.addOrder(zeroQtyOrder);
        
        assertTrue(trades.isEmpty());
        assertEquals(0, orderBook.getTotalBuyOrders()); // Zero quantity orders shouldn't be added
    }
}
