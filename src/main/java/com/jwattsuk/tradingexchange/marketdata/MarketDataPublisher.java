package com.jwattsuk.tradingexchange.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwattsuk.tradingexchange.config.ExchangeConfig;
import com.jwattsuk.tradingexchange.matching.MatchingEngine;
import com.jwattsuk.tradingexchange.matching.OrderBook;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes market data updates via WebSocket connections.
 * Provides real-time order book snapshots and trade information.
 */
public class MarketDataPublisher {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataPublisher.class);
    
    private final ExchangeConfig config;
    private final MatchingEngine matchingEngine;
    private final ObjectMapper objectMapper;
    private final List<Channel> connectedClients;
    private final AtomicBoolean running;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public MarketDataPublisher(ExchangeConfig config, MatchingEngine matchingEngine) {
        this.config = config;
        this.matchingEngine = matchingEngine;
        this.objectMapper = new ObjectMapper();
        this.connectedClients = new CopyOnWriteArrayList<>();
        this.running = new AtomicBoolean(false);
    }
    
    public void start() throws Exception {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting Market Data Publisher on port: {}", config.getMarketDataPort());
            
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new MarketDataChannelInitializer());
                
                serverChannel = bootstrap.bind(config.getMarketDataPort()).sync().channel();
                logger.info("Market Data Publisher started successfully");
                
            } catch (Exception e) {
                logger.error("Failed to start Market Data Publisher", e);
                running.set(false);
                throw e;
            }
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping Market Data Publisher...");
            
            // Close all client connections
            for (Channel client : connectedClients) {
                client.close();
            }
            connectedClients.clear();
            
            // Close server channel
            if (serverChannel != null) {
                serverChannel.close();
            }
            
            // Shutdown event loop groups
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            
            logger.info("Market Data Publisher stopped");
        }
    }
    
    /**
     * Publishes market data for all symbols.
     */
    public void publishMarketData() {
        if (!running.get()) {
            return;
        }
        
        try {
            for (String symbol : config.getSymbols()) {
                OrderBook.OrderBookSnapshot snapshot = matchingEngine.getOrderBookSnapshot(symbol);
                if (snapshot != null) {
                    publishOrderBookSnapshot(snapshot);
                }
                
                MatchingEngine.Quote quote = matchingEngine.getQuote(symbol);
                if (quote != null) {
                    publishQuote(quote);
                }
            }
        } catch (Exception e) {
            logger.error("Error publishing market data", e);
        }
    }
    
    /**
     * Publishes an order book snapshot to all connected clients.
     */
    public void publishOrderBookSnapshot(OrderBook.OrderBookSnapshot snapshot) {
        try {
            MarketDataMessage message = new MarketDataMessage(
                "ORDER_BOOK",
                snapshot.getSymbol(),
                snapshot.getTimestamp(),
                snapshot
            );
            
            String json = objectMapper.writeValueAsString(message);
            broadcastMessage(json);
            
        } catch (Exception e) {
            logger.error("Error publishing order book snapshot", e);
        }
    }
    
    /**
     * Publishes a quote to all connected clients.
     */
    public void publishQuote(MatchingEngine.Quote quote) {
        try {
            MarketDataMessage message = new MarketDataMessage(
                "QUOTE",
                quote.getSymbol(),
                System.currentTimeMillis(),
                quote
            );
            
            String json = objectMapper.writeValueAsString(message);
            broadcastMessage(json);
            
        } catch (Exception e) {
            logger.error("Error publishing quote", e);
        }
    }
    
    /**
     * Publishes a trade to all connected clients.
     */
    public void publishTrade(com.jwattsuk.tradingexchange.model.Trade trade) {
        try {
            MarketDataMessage message = new MarketDataMessage(
                "TRADE",
                trade.getSymbol(),
                System.currentTimeMillis(),
                trade
            );
            
            String json = objectMapper.writeValueAsString(message);
            broadcastMessage(json);
            
        } catch (Exception e) {
            logger.error("Error publishing trade", e);
        }
    }
    
    /**
     * Broadcasts a message to all connected clients.
     */
    private void broadcastMessage(String message) {
        TextWebSocketFrame frame = new TextWebSocketFrame(message);
        
        // Remove disconnected clients
        connectedClients.removeIf(channel -> !channel.isActive());
        
        // Send to all active clients
        for (Channel client : connectedClients) {
            if (client.isActive() && client.isWritable()) {
                client.writeAndFlush(frame.retain());
            }
        }
        
        frame.release();
    }
    
    /**
     * Adds a new client connection.
     */
    private void addClient(Channel channel) {
        connectedClients.add(channel);
        logger.info("Market data client connected. Total clients: {}", connectedClients.size());
    }
    
    /**
     * Removes a client connection.
     */
    private void removeClient(Channel channel) {
        connectedClients.remove(channel);
        logger.info("Market data client disconnected. Total clients: {}", connectedClients.size());
    }
    
    /**
     * Netty channel initializer for market data connections.
     */
    private class MarketDataChannelInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            
            // HTTP codec
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(new HttpObjectAggregator(65536));
            
            // WebSocket handler
            pipeline.addLast(new WebSocketServerProtocolHandler("/marketdata", null, true));
            pipeline.addLast(new MarketDataWebSocketHandler());
        }
    }
    
    /**
     * WebSocket handler for market data connections.
     */
    private class MarketDataWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            addClient(ctx.channel());
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            removeClient(ctx.channel());
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
            // Handle incoming messages if needed
            String request = frame.text();
            logger.debug("Received market data request: {}", request);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Market data WebSocket error", cause);
            ctx.close();
        }
    }
    
    /**
     * Market data message wrapper.
     */
    public static class MarketDataMessage {
        private final String type;
        private final String symbol;
        private final long timestamp;
        private final Object data;
        
        public MarketDataMessage(String type, String symbol, long timestamp, Object data) {
            this.type = type;
            this.symbol = symbol;
            this.timestamp = timestamp;
            this.data = data;
        }
        
        public String getType() { return type; }
        public String getSymbol() { return symbol; }
        public long getTimestamp() { return timestamp; }
        public Object getData() { return data; }
    }
}
