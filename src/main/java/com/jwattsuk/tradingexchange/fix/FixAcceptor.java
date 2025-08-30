package com.jwattsuk.tradingexchange.fix;

import com.jwattsuk.tradingexchange.config.ExchangeConfig;
import com.jwattsuk.tradingexchange.matching.MatchingEngine;
import com.jwattsuk.tradingexchange.marketdata.MarketDataPublisher;
import com.jwattsuk.tradingexchange.model.Order;
import com.jwattsuk.tradingexchange.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FIX 4.4 Acceptor that handles client connections and message processing.
 * Uses QuickFIX/J for high-performance FIX message handling.
 */
public class FixAcceptor {
    private static final Logger logger = LoggerFactory.getLogger(FixAcceptor.class);
    
    private final ExchangeConfig config;
    private final MatchingEngine matchingEngine;
    private final MarketDataPublisher marketDataPublisher;
    private final FixMessageHandler messageHandler;
    private final FixSessionManager sessionManager;
    
    private SocketAcceptor acceptor;
    private final AtomicLong messageSequence;
    
    public FixAcceptor(ExchangeConfig config, MatchingEngine matchingEngine, 
                      MarketDataPublisher marketDataPublisher) {
        this.config = config;
        this.matchingEngine = matchingEngine;
        this.marketDataPublisher = marketDataPublisher;
        this.messageHandler = new FixMessageHandler(matchingEngine);
        this.sessionManager = new FixSessionManager();
        this.messageSequence = new AtomicLong(1);
    }
    
    public void start() throws Exception {
        logger.info("Starting FIX Acceptor on port: {}", config.getFixPort());
        
        // Create QuickFIX configuration
        SessionSettings settings = createSessionSettings();
        
        // Create message store factory (in-memory for simplicity)
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        
        // Create log factory
        LogFactory logFactory = new ScreenLogFactory(settings);
        
        // Create message factory
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // Create application
        FixApplication application = new FixApplication(messageHandler, sessionManager);
        
        // Create acceptor
        acceptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);
        
        // Start acceptor
        acceptor.start();
        
        logger.info("FIX Acceptor started successfully");
    }
    
    public void stop() {
        if (acceptor != null) {
            logger.info("Stopping FIX Acceptor...");
            acceptor.stop();
            logger.info("FIX Acceptor stopped");
        }
    }
    
    /**
     * Creates QuickFIX session settings.
     */
    private SessionSettings createSessionSettings() throws ConfigError {
        SessionSettings settings;
        
        try {
            // Try to load from configuration file
            settings = new SessionSettings(config.getFixConfigFile());
            logger.info("Loaded QuickFIX configuration from file: {}", config.getFixConfigFile());
        } catch (ConfigError e) {
            logger.warn("Failed to load QuickFIX configuration from file, using programmatic configuration");
            
            // Fallback to programmatic configuration
            settings = new SessionSettings();
            
            // Default settings
            settings.setString("ConnectionType", "acceptor");
            settings.setString("SocketAcceptPort", String.valueOf(config.getFixPort()));
            settings.setString("StartTime", "00:00:00");
            settings.setString("EndTime", "00:00:00");
            settings.setString("UseDataDictionary", "Y");
            settings.setString("DataDictionary", "FIX44.xml");
            settings.setString("HeartBtInt", String.valueOf(config.getHeartbeatIntervalMs() / 1000));
            settings.setString("ValidateUserDefinedFields", "N");
            settings.setString("ValidateIncomingMessage", "N");
            settings.setString("ValidateFieldsOutOfOrder", "N");
            settings.setString("ValidateUnorderedGroupFields", "N");
            settings.setString("ValidateFieldsHaveValues", "N");
            
            // Session settings
            settings.setString("BeginString", "FIX.4.4");
            settings.setString("SenderCompID", config.getSenderCompId());
            settings.setString("TargetCompID", config.getTargetCompId());
        }
        
        return settings;
    }
    
    /**
     * Sends an execution report to a client.
     */
    public void sendExecutionReport(Session session, Order order, List<Trade> trades) {
        try {
            Message report = new Message();
            report.getHeader().setString(MsgType.FIELD, "8"); // ExecutionReport
            
            // Set standard fields
            report.setString(OrderID.FIELD, String.valueOf(order.getOrderId()));
            report.setString(ClOrdID.FIELD, order.getClientOrderId());
            report.setString(Symbol.FIELD, order.getSymbol());
            report.setChar(Side.FIELD, order.getSide() == Order.Side.BUY ? '1' : '2');
            report.setDouble(OrderQty.FIELD, order.getQuantity());
            report.setDouble(LeavesQty.FIELD, order.getRemainingQuantity());
            report.setDouble(CumQty.FIELD, order.getFilledQuantity());
            report.setDouble(AvgPx.FIELD, calculateAveragePrice(trades));
            
            // Set execution type based on order status
            char execType = getExecutionType(order.getStatus());
            report.setChar(ExecType.FIELD, execType);
            
            // Set order status
            char orderStatus = getOrderStatus(order.getStatus());
            report.setChar(OrdStatus.FIELD, orderStatus);
            
            // Set timestamp
            report.setUtcTimeStamp(TransactTime.FIELD, java.time.LocalDateTime.now());
            
            // Send the message
            session.send(report);
            
            logger.debug("Sent execution report: {} for order: {}", execType, order.getOrderId());
            
        } catch (Exception e) {
            logger.error("Error sending execution report", e);
        }
    }
    
    /**
     * Sends a reject message to a client.
     */
    public void sendReject(Session session, String reason, int refSeqNum) {
        try {
            Message reject = new Message();
            reject.getHeader().setString(MsgType.FIELD, "3"); // Reject
            
            reject.setInt(RefSeqNum.FIELD, refSeqNum);
            reject.setString(Text.FIELD, reason);
            session.send(reject);
            
            logger.debug("Sent reject: {} for seqNum: {}", reason, refSeqNum);
            
        } catch (Exception e) {
            logger.error("Error sending reject", e);
        }
    }
    
    /**
     * Calculates average price from trades.
     */
    private double calculateAveragePrice(List<Trade> trades) {
        if (trades.isEmpty()) {
            return 0.0;
        }
        
        double totalValue = 0.0;
        int totalQuantity = 0;
        
        for (Trade trade : trades) {
            totalValue += trade.getPrice() * trade.getQuantity();
            totalQuantity += trade.getQuantity();
        }
        
        return totalQuantity > 0 ? totalValue / totalQuantity : 0.0;
    }
    
    /**
     * Maps order status to FIX execution type.
     */
    private char getExecutionType(Order.OrderStatus status) {
        return switch (status) {
            case NEW -> '0'; // ExecType.NEW
            case PARTIALLY_FILLED -> '1'; // ExecType.PARTIAL_FILL
            case FILLED -> '2'; // ExecType.FILL
            case CANCELLED -> '4'; // ExecType.CANCELED
            case REJECTED -> '8'; // ExecType.REJECTED
            case PENDING_CANCEL -> '6'; // ExecType.PENDING_CANCEL
        };
    }
    
    /**
     * Maps order status to FIX order status.
     */
    private char getOrderStatus(Order.OrderStatus status) {
        return switch (status) {
            case NEW -> '0'; // OrdStatus.NEW
            case PARTIALLY_FILLED -> '1'; // OrdStatus.PARTIALLY_FILLED
            case FILLED -> '2'; // OrdStatus.FILLED
            case CANCELLED -> '4'; // OrdStatus.CANCELED
            case REJECTED -> '8'; // OrdStatus.REJECTED
            case PENDING_CANCEL -> '6'; // OrdStatus.PENDING_CANCEL
        };
    }
    
    /**
     * QuickFIX Application implementation.
     */
    private static class FixApplication extends MessageCracker implements Application {
        private final FixMessageHandler messageHandler;
        private final FixSessionManager sessionManager;
        
        public FixApplication(FixMessageHandler messageHandler, FixSessionManager sessionManager) {
            this.messageHandler = messageHandler;
            this.sessionManager = sessionManager;
        }
        
        @Override
        public void onCreate(SessionID sessionID) {
            logger.info("FIX session created: {}", sessionID);
            sessionManager.addSession(sessionID);
        }
        
        @Override
        public void onLogon(SessionID sessionID) {
            logger.info("FIX session logged on: {}", sessionID);
        }
        
        @Override
        public void onLogout(SessionID sessionID) {
            logger.info("FIX session logged out: {}", sessionID);
            sessionManager.removeSession(sessionID);
        }
        
        @Override
        public void toAdmin(Message message, SessionID sessionID) {
            // Add admin messages if needed
        }
        
        @Override
        public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
            // Handle admin messages
        }
        
        @Override
        public void toApp(Message message, SessionID sessionID) throws DoNotSend {
            // Add application messages if needed
        }
        
        @Override
        public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
            try {
                crack(message, sessionID);
            } catch (Exception e) {
                logger.error("Error processing FIX message", e);
            }
        }
        
        @MessageCracker.Handler
        public void onMessage(Message message, SessionID sessionID) throws FieldNotFound {
            try {
                String msgType = message.getHeader().getString(MsgType.FIELD);
                switch (msgType) {
                    case "D" -> messageHandler.handleNewOrderSingle(message, sessionID);
                    case "F" -> messageHandler.handleOrderCancelRequest(message, sessionID);
                    case "G" -> messageHandler.handleOrderCancelReplaceRequest(message, sessionID);
                    default -> logger.debug("Unhandled message type: {}", msgType);
                }
            } catch (Exception e) {
                logger.error("Error processing message", e);
            }
        }
    }
}
