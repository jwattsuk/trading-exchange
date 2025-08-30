package com.jwattsuk.tradingexchange.fix;

import com.jwattsuk.tradingexchange.matching.MatchingEngine;
import com.jwattsuk.tradingexchange.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

/**
 * Handles FIX message processing and converts them to internal order objects.
 */
public class FixMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(FixMessageHandler.class);
    
    private final MatchingEngine matchingEngine;
    
    public FixMessageHandler(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }
    
    public void handleNewOrderSingle(Message message, SessionID sessionID) throws FieldNotFound {
        try {
            // Extract and validate order details from FIX message
            String clientOrderId = getRequiredString(message, ClOrdID.FIELD, "ClientOrderId");
            String symbol = getRequiredString(message, Symbol.FIELD, "Symbol");
            char side = getRequiredChar(message, Side.FIELD, "Side");
            int quantity = (int) getRequiredDouble(message, OrderQty.FIELD, "OrderQty");
            char orderType = getRequiredChar(message, OrdType.FIELD, "OrdType");
            
            // Price is required for limit orders
            double price = 0.0;
            if (orderType == '2') { // Limit order
                price = getRequiredDouble(message, Price.FIELD, "Price");
            }
            
            // Validate basic fields
            if (quantity <= 0) {
                throw new IllegalArgumentException("Invalid quantity: " + quantity);
            }
            if (orderType == '2' && price <= 0) {
                throw new IllegalArgumentException("Invalid price for limit order: " + price);
            }
            
            // Convert to internal order
            Order order = createOrder(clientOrderId, symbol, side, quantity, price, orderType, sessionID);
            
            // Process order
            MatchingEngine.OrderResult result = matchingEngine.processOrder(order);
            
            // Send execution report
            Session session = Session.lookupSession(sessionID);
            if (session != null) {
                // This would be handled by the FixAcceptor
                logger.info("Processed new order: {} -> {}", order, result.getTrades().size());
            }
            
        } catch (FieldNotFound e) {
            logger.error("Missing required field in NewOrderSingle: {}", e.getMessage());
            sendReject(sessionID, "Missing required field: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid field value in NewOrderSingle: {}", e.getMessage());
            sendReject(sessionID, "Invalid field value: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing NewOrderSingle", e);
            sendReject(sessionID, "Internal error processing order");
        }
    }
    
    public void handleOrderCancelRequest(Message message, SessionID sessionID) throws FieldNotFound {
        try {
            String clientOrderId = message.getString(ClOrdID.FIELD);
            String symbol = message.getString(Symbol.FIELD);
            
            // For simplicity, we'll cancel by client order ID
            // In a real system, you'd need to maintain a mapping
            logger.info("Cancel request received: {} for symbol: {}", clientOrderId, symbol);
            
        } catch (Exception e) {
            logger.error("Error processing OrderCancelRequest", e);
        }
    }
    
    public void handleOrderCancelReplaceRequest(Message message, SessionID sessionID) throws FieldNotFound {
        try {
            String clientOrderId = message.getString(ClOrdID.FIELD);
            String symbol = message.getString(Symbol.FIELD);
            int quantity = (int) message.getDouble(OrderQty.FIELD);
            double price = message.getDouble(Price.FIELD);
            
            logger.info("Cancel/Replace request received: {} for symbol: {}", clientOrderId, symbol);
            
        } catch (Exception e) {
            logger.error("Error processing OrderCancelReplaceRequest", e);
        }
    }
    
    private Order createOrder(String clientOrderId, String symbol, char side, int quantity, 
                            double price, char orderType, SessionID sessionID) {
        Order.Side orderSide = (side == '1') ? Order.Side.BUY : Order.Side.SELL;
        Order.OrderType type = convertOrderType(orderType);
        String clientId = sessionID.getSenderCompID();
        
        return new Order(clientOrderId, symbol, orderSide, type, price, quantity, clientId);
    }
    
    private Order.OrderType convertOrderType(char fixOrderType) {
        return switch (fixOrderType) {
            case '1' -> Order.OrderType.MARKET; // OrdType.MARKET
            case '2' -> Order.OrderType.LIMIT;  // OrdType.LIMIT
            case '3' -> Order.OrderType.STOP;   // OrdType.STOP
            case '4' -> Order.OrderType.STOP_LIMIT; // OrdType.STOP_LIMIT
            default -> Order.OrderType.LIMIT; // Default to limit
        };
    }
    
    // Helper methods for field validation
    private String getRequiredString(Message message, int field, String fieldName) throws FieldNotFound {
        if (!message.isSetField(field)) {
            throw new FieldNotFound(fieldName + " is required");
        }
        return message.getString(field);
    }
    
    private char getRequiredChar(Message message, int field, String fieldName) throws FieldNotFound {
        if (!message.isSetField(field)) {
            throw new FieldNotFound(fieldName + " is required");
        }
        return message.getChar(field);
    }
    
    private double getRequiredDouble(Message message, int field, String fieldName) throws FieldNotFound {
        if (!message.isSetField(field)) {
            throw new FieldNotFound(fieldName + " is required");
        }
        return message.getDouble(field);
    }
    
    private void sendReject(SessionID sessionID, String reason) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session != null) {
                // This would be handled by the FixAcceptor
                logger.warn("Would send reject for session {}: {}", sessionID, reason);
            }
        } catch (Exception e) {
            logger.error("Error sending reject", e);
        }
    }
}
