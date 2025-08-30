package com.jwattsuk.tradingexchange.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    
    @Test
    void testOrderCreation() {
        Order order = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        
        assertEquals("CLIENT123", order.getClientOrderId());
        assertEquals("AAPL", order.getSymbol());
        assertEquals(Order.Side.BUY, order.getSide());
        assertEquals(Order.OrderType.LIMIT, order.getOrderType());
        assertEquals(150.50, order.getPrice());
        assertEquals(100, order.getQuantity());
        assertEquals(100, order.getRemainingQuantity());
        assertEquals(0, order.getFilledQuantity());
        assertEquals("CLIENT1", order.getClientId());
        assertEquals(Order.OrderStatus.NEW, order.getStatus());
        assertTrue(order.isActive());
        assertFalse(order.isFilled());
        assertFalse(order.isCancelled());
        assertFalse(order.isRejected());
    }
    
    @Test
    void testOrderWithPartialFill() {
        Order order = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        
        Order filledOrder = order.withPartialFill(30);
        
        assertEquals(70, filledOrder.getRemainingQuantity());
        assertEquals(30, filledOrder.getFilledQuantity());
        assertEquals(Order.OrderStatus.PARTIALLY_FILLED, filledOrder.getStatus());
        assertTrue(filledOrder.isActive());
    }
    
    @Test
    void testOrderWithFullFill() {
        Order order = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        
        Order filledOrder = order.withPartialFill(100);
        
        assertEquals(0, filledOrder.getRemainingQuantity());
        assertEquals(100, filledOrder.getFilledQuantity());
        assertEquals(Order.OrderStatus.FILLED, filledOrder.getStatus());
        assertFalse(filledOrder.isActive());
        assertTrue(filledOrder.isFilled());
    }
    
    @Test
    void testOrderWithCancel() {
        Order order = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        
        Order cancelledOrder = order.withCancel();
        
        assertEquals(100, cancelledOrder.getRemainingQuantity());
        assertEquals(0, cancelledOrder.getFilledQuantity());
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());
        assertFalse(cancelledOrder.isActive());
        assertTrue(cancelledOrder.isCancelled());
    }
    
    @Test
    void testOrderWithReject() {
        Order order = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        
        Order rejectedOrder = order.withReject();
        
        assertEquals(100, rejectedOrder.getRemainingQuantity());
        assertEquals(0, rejectedOrder.getFilledQuantity());
        assertEquals(Order.OrderStatus.REJECTED, rejectedOrder.getStatus());
        assertFalse(rejectedOrder.isActive());
        assertTrue(rejectedOrder.isRejected());
    }
    
    @Test
    void testOrderEquality() {
        Order order1 = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        Order order2 = new Order("CLIENT456", "MSFT", Order.Side.SELL, Order.OrderType.MARKET, 200.00, 50, "CLIENT2");
        
        // Orders should not be equal even with same fields due to different order IDs
        assertNotEquals(order1, order2);
        
        // Same order should be equal to itself
        assertEquals(order1, order1);
    }
    
    @Test
    void testOrderHashCode() {
        Order order1 = new Order("CLIENT123", "AAPL", Order.Side.BUY, Order.OrderType.LIMIT, 150.50, 100, "CLIENT1");
        Order order2 = new Order("CLIENT456", "MSFT", Order.Side.SELL, Order.OrderType.MARKET, 200.00, 50, "CLIENT2");
        
        assertNotEquals(order1.hashCode(), order2.hashCode());
        assertEquals(order1.hashCode(), order1.hashCode());
    }
}
