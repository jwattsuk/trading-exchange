package com.jwattsuk.tradingexchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Consolidated FIX test client for testing order placement with the trading exchange.
 * This client connects to the exchange and can send various order types with comprehensive logging.
 * 
 * Features:
 * - FIX 4.4 protocol support
 * - Market and limit orders
 * - Order cancellation
 * - Interactive command-line interface
 * - Comprehensive low-level debug logging
 * - Proper session management
 */
public class FixTestClient {
    private static final Logger logger = LoggerFactory.getLogger(FixTestClient.class);
    
    private SocketInitiator initiator;
    private Session session;
    private final CountDownLatch logonLatch = new CountDownLatch(1);
    private boolean isRunning = false;
    
    public static void main(String[] args) {
        try {
            logger.info("Starting FIX Test Client...");
            logger.info("This client will connect to the trading exchange and allow you to send orders");
            
            FixTestClient client = new FixTestClient();
            client.start();
            
            if (client.isConnected()) {
                client.runInteractiveMode();
            } else {
                logger.error("Failed to establish FIX session");
            }
            
        } catch (Exception e) {
            logger.error("Error during FIX test", e);
        }
    }
    
    /**
     * Start the FIX client and establish connection
     */
    public void start() throws Exception {
        logger.info("Starting FIX client connection...");
        
        // Load session settings from configuration file
        SessionSettings settings = new SessionSettings("fix-test-client.cfg");
        logger.info("Loaded session settings from fix-test-client.cfg");
        logger.info("Session configuration: {}", settings);
        
        // Create components
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        logger.info("Message store factory created (in-memory)");
        
        LogFactory logFactory = new ScreenLogFactory(settings);
        logger.info("Log factory created (screen logging enabled)");
        
        MessageFactory messageFactory = new DefaultMessageFactory();
        logger.info("Message factory created (FIX 4.4)");
        
        // Create application
        FixTestApplication application = new FixTestApplication();
        logger.info("FIX application created");
        
        // Create initiator
        initiator = new SocketInitiator(application, storeFactory, settings, logFactory, messageFactory);
        logger.info("Socket initiator created");
        
        logger.info("Starting initiator...");
        initiator.start();
        logger.info("Waiting for session establishment...");
        
        // Wait for session establishment
        if (logonLatch.await(30, TimeUnit.SECONDS)) {
            logger.info("FIX session established successfully!");
            isRunning = true;
        } else {
            logger.error("Failed to establish FIX session within 30 seconds");
            throw new RuntimeException("Session establishment timeout");
        }
    }
    
    /**
     * Stop the FIX client
     */
    public void stop() {
        if (initiator != null) {
            logger.info("Stopping FIX client...");
            initiator.stop();
            logger.info("FIX client stopped");
            isRunning = false;
        }
    }
    
    /**
     * Check if client is connected
     */
    public boolean isConnected() {
        return isRunning && session != null && session.isLoggedOn();
    }
    
    /**
     * Send a new order single (market or limit order)
     */
    public void sendNewOrderSingle(String symbol, char side, int quantity, double price, char orderType) {
        if (!isConnected()) {
            logger.error("No active FIX session");
            return;
        }
        
        try {
            logger.info("Sending NewOrderSingle message...");
            logger.info("Order details: Symbol={}, Side={}, Quantity={}, Price={}, Type={}", 
                symbol, side == '1' ? "BUY" : "SELL", quantity, price, orderType == '1' ? "MARKET" : "LIMIT");
            
            // Create a NewOrderSingle message
            quickfix.fix44.NewOrderSingle order = new quickfix.fix44.NewOrderSingle();
            
            // Set required fields
            String clientOrderId = "ORDER_" + System.currentTimeMillis();
            order.set(new ClOrdID(clientOrderId));
            order.set(new Symbol(symbol));
            order.set(new Side(side));
            order.set(new OrderQty(quantity));
            order.set(new OrdType(orderType));
            order.set(new TransactTime());
            
            // Set price for limit orders
            if (orderType == '2') { // Limit order
                order.set(new Price(price));
                logger.info("Limit order price set to: {}", price);
            }
            
            // Log the complete message
            logger.info("FIX message prepared: {}", order);
            
            // Send the order
            boolean sent = session.send(order);
            if (sent) {
                logger.info("Order sent successfully! ClientOrderID: {}", clientOrderId);
            } else {
                logger.error("Failed to send order");
            }
            
        } catch (Exception e) {
            logger.error("Error sending order", e);
        }
    }
    
    /**
     * Send an order cancel request
     */
    public void sendOrderCancelRequest(String clientOrderId, String symbol, char side, int quantity) {
        if (!isConnected()) {
            logger.error("No active FIX session");
            return;
        }
        
        try {
            logger.info("Sending OrderCancelRequest...");
            logger.info("Cancel details: ClOrdID={}, Symbol={}, Side={}, Quantity={}", 
                clientOrderId, symbol, side == '1' ? "BUY" : "SELL", quantity);
            
            quickfix.fix44.OrderCancelRequest cancelRequest = new quickfix.fix44.OrderCancelRequest();
            cancelRequest.set(new OrigClOrdID(clientOrderId));
            cancelRequest.set(new ClOrdID("CANCEL_" + System.currentTimeMillis()));
            cancelRequest.set(new Symbol(symbol));
            cancelRequest.set(new Side(side));
            cancelRequest.set(new OrderQty(quantity));
            cancelRequest.set(new TransactTime());
            
            logger.info("Cancel request prepared: {}", cancelRequest);
            
            boolean sent = session.send(cancelRequest);
            if (sent) {
                logger.info("Cancel request sent successfully!");
            } else {
                logger.error("Failed to send cancel request");
            }
            
        } catch (Exception e) {
            logger.error("Error sending cancel request", e);
        }
    }
    
    /**
     * Run interactive mode for testing
     */
    private void runInteractiveMode() {
        logger.info("Entering interactive mode...");
        logger.info("Available commands:");
        logger.info("  buy <symbol> <quantity> [price]  - Send buy order (market if no price, limit if price given)");
        logger.info("  sell <symbol> <quantity> [price] - Send sell order (market if no price, limit if price given)");
        logger.info("  cancel <clordid> <symbol> <side> <quantity> - Cancel an order");
        logger.info("  status - Show connection status");
        logger.info("  quit - Exit the client");
        logger.info("");
        
        Scanner scanner = new Scanner(System.in);
        
        while (isRunning) {
            try {
                System.out.print("FIX> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                
                switch (command) {
                    case "buy":
                        handleBuyOrder(parts);
                        break;
                    case "sell":
                        handleSellOrder(parts);
                        break;
                    case "cancel":
                        handleCancelOrder(parts);
                        break;
                    case "status":
                        showStatus();
                        break;
                    case "quit":
                    case "exit":
                        logger.info("Goodbye!");
                        isRunning = false;
                        break;
                    default:
                        logger.warn("Unknown command: {}. Type 'help' for available commands.", command);
                }
                
            } catch (Exception e) {
                logger.error("Error processing command", e);
            }
        }
        
        scanner.close();
        stop();
    }
    
    private void handleBuyOrder(String[] parts) {
        if (parts.length < 3) {
            logger.warn("Usage: buy <symbol> <quantity> [price]");
            return;
        }
        
        String symbol = parts[1];
        int quantity = Integer.parseInt(parts[2]);
        char orderType = '1'; // Market
        double price = 0.0;
        
        if (parts.length > 3) {
            price = Double.parseDouble(parts[3]);
            orderType = '2'; // Limit
        }
        
        sendNewOrderSingle(symbol, '1', quantity, price, orderType);
    }
    
    private void handleSellOrder(String[] parts) {
        if (parts.length < 3) {
            logger.warn("Usage: sell <symbol> <quantity> [price]");
            return;
        }
        
        String symbol = parts[1];
        int quantity = Integer.parseInt(parts[2]);
        char orderType = '1'; // Market
        double price = 0.0;
        
        if (parts.length > 3) {
            price = Double.parseDouble(parts[3]);
            orderType = '2'; // Limit
        }
        
        sendNewOrderSingle(symbol, '2', quantity, price, orderType);
    }
    
    private void handleCancelOrder(String[] parts) {
        if (parts.length < 5) {
            logger.warn("Usage: cancel <clordid> <symbol> <side> <quantity>");
            return;
        }
        
        String clientOrderId = parts[1];
        String symbol = parts[2];
        char side = parts[3].equalsIgnoreCase("buy") ? '1' : '2';
        int quantity = Integer.parseInt(parts[4]);
        
        sendOrderCancelRequest(clientOrderId, symbol, side, quantity);
    }
    
    private void showStatus() {
        logger.info("Connection Status:");
        logger.info("  Running: {}", isRunning);
        logger.info("  Session: {}", session != null ? session.getSessionID() : "None");
        logger.info("  Logged On: {}", session != null && session.isLoggedOn());
        logger.info("  Initiator: {}", initiator != null ? "Active" : "Inactive");
    }
    
    /**
     * FIX Application implementation with comprehensive logging
     */
    private class FixTestApplication extends MessageCracker implements Application {
        
        @Override
        public void onCreate(SessionID sessionID) {
            logger.info("FIX session created: {}", sessionID);
        }
        
        @Override
        public void onLogon(SessionID sessionID) {
            logger.info("FIX session logged on: {}", sessionID);
            session = Session.lookupSession(sessionID);
            logonLatch.countDown();
        }
        
        @Override
        public void onLogout(SessionID sessionID) {
            logger.info("FIX session logged out: {}", sessionID);
            session = null;
            isRunning = false;
        }
        
        @Override
        public void toAdmin(Message message, SessionID sessionID) {
            logger.debug("Sending admin message: {}", message);
        }
        
        @Override
        public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
            logger.debug("Received admin message: {}", message);
            
            // Handle specific admin messages
            try {
                String msgType = message.getHeader().getString(MsgType.FIELD);
                if ("A".equals(msgType)) { // Logon
                    logger.info("Received Logon message from session: {}", sessionID);
                } else if ("5".equals(msgType)) { // Logout
                    logger.info("Received Logout message from session: {}", sessionID);
                } else if ("0".equals(msgType)) { // Heartbeat
                    logger.debug("Received Heartbeat from session: {}", sessionID);
                } else if ("1".equals(msgType)) { // Test Request
                    logger.debug("Received Test Request from session: {}", sessionID);
                }
            } catch (FieldNotFound e) {
                logger.warn("Admin message missing MsgType field");
            }
        }
        
        @Override
        public void toApp(Message message, SessionID sessionID) throws DoNotSend {
            logger.debug("Sending app message: {}", message);
        }
        
        @Override
        public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
            logger.info("Received application message from session: {}", sessionID);
            logger.info("Message content: {}", message);
            
            try {
                crack(message, sessionID);
            } catch (Exception e) {
                logger.error("Error processing FIX message", e);
            }
        }
        
        /**
         * Handle execution reports
         */
        @MessageCracker.Handler
        public void onMessage(quickfix.fix44.ExecutionReport message, SessionID sessionID) throws FieldNotFound {
            String orderId = message.getString(OrderID.FIELD);
            String clientOrderId = message.getString(ClOrdID.FIELD);
            char execType = message.getChar(ExecType.FIELD);
            char ordStatus = message.getChar(OrdStatus.FIELD);
            
            logger.info("Execution Report received:");
            logger.info("  OrderID: {}", orderId);
            logger.info("  ClOrdID: {}", clientOrderId);
            logger.info("  ExecType: {}", execType);
            logger.info("  OrdStatus: {}", ordStatus);
            
            // Log additional fields if present
            try {
                if (message.isSetField(Symbol.FIELD)) {
                    String symbol = message.getString(Symbol.FIELD);
                    logger.info("  Symbol: {}", symbol);
                }
                if (message.isSetField(OrderQty.FIELD)) {
                    double quantity = message.getDouble(OrderQty.FIELD);
                    logger.info("  Quantity: {}", quantity);
                }
                if (message.isSetField(Price.FIELD)) {
                    double price = message.getDouble(Price.FIELD);
                    logger.info("  Price: {}", price);
                }
            } catch (FieldNotFound e) {
                // Optional fields not present
            }
        }
        
        /**
         * Handle reject messages
         */
        @MessageCracker.Handler
        public void onMessage(quickfix.fix44.Reject message, SessionID sessionID) throws FieldNotFound {
            String reason = message.getString(Text.FIELD);
            logger.warn("Reject message received: {}", reason);
            logger.warn("Reject details: {}", message);
        }
        
        /**
         * Handle order cancel reject messages
         */
        @MessageCracker.Handler
        public void onMessage(quickfix.fix44.OrderCancelReject message, SessionID sessionID) throws FieldNotFound {
            String clientOrderId = message.getString(ClOrdID.FIELD);
            String reason = message.getString(Text.FIELD);
            logger.warn("Order Cancel Reject received for ClOrdID: {}", clientOrderId);
            logger.warn("Reject reason: {}", reason);
        }
    }
}
