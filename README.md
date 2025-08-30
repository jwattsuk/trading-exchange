# Mock Trading Exchange Gateway

A high-performance mock trading exchange built in Java with FIX 4.4 support, designed for low-latency trading applications.

## Features

- **FIX 4.4 Protocol Support**: Accepts inbound FIX connections from multiple market participants
- **High-Performance Matching Engine**: Price-time priority order matching with efficient data structures
- **Real-time Market Data**: WebSocket-based market data streaming
- **Low-Latency Design**: Non-blocking I/O, minimal GC pressure, efficient data structures
- **Production-Grade**: Comprehensive error handling, logging, and monitoring
- **Extensible Architecture**: Easy to add new FIX message types and trading features

## Supported FIX Messages

- **New Order Single (D)**: Place new orders
- **Order Cancel Request (F)**: Cancel existing orders
- **Order Cancel Replace Request (G)**: Modify existing orders
- **Execution Report (8)**: Order acknowledgements, fills, cancels, and rejects

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   FIX Clients   │    │  Market Data    │    │   Web Clients   │
│                 │    │   Subscribers   │    │                 │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │ FIX 4.4              │ WebSocket            │
          │ TCP                  │ JSON                 │
          │                      │                      │
┌─────────▼─────────────────────────────────────────────────────┐
│                    Mock Trading Exchange                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   FIX Acceptor  │  │ Matching Engine │  │ Market Data     │ │
│  │   (QuickFIX/J)  │  │                 │  │ Publisher       │ │
│  └─────────┬───────┘  └─────────┬───────┘  └─────────┬───────┘ │
│            │                    │                    │         │
│            └─────────┬──────────┴──────────┬─────────┘         │
│                      │                     │                   │
│            ┌─────────▼─────────┐ ┌─────────▼─────────┐         │
│            │   Order Books     │ │   Configuration   │         │
│            │   (Per Symbol)    │ │   & Logging       │         │
│            └───────────────────┘ └───────────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Building

```bash
mvn clean package
```

### Running

```bash
java -jar target/mock-trading-exchange-1.0.0.jar
```

The exchange will start and listen on:
- **FIX Acceptor**: Port 5001
- **Market Data**: Port 5002 (WebSocket)

### Configuration

Edit `src/main/resources/exchange.properties` to customize:

```properties
# FIX Configuration
fix.port=5001
fix.sender.comp.id=EXCHANGE
fix.target.comp.id=CLIENT

# Market Data Configuration
marketdata.port=5002
marketdata.interval.ms=100

# Trading Configuration
trading.symbols=AAPL,MSFT,GOOGL,TSLA,AMZN,NVDA
trading.max.orderbook.levels=10
trading.enable.logging=true

# Performance Configuration
performance.thread.pool.size=4
```

## Usage Examples

### Connecting via FIX

Use any FIX 4.4 client to connect to `localhost:5001`:

```
8=FIX.4.4|9=123|35=D|49=CLIENT|56=EXCHANGE|34=1|52=20231201-10:00:00|11=ORDER001|21=1|55=AAPL|54=1|60=20231201-10:00:00|38=100|40=2|44=150.50|10=123|
```

### Connecting to Market Data

Connect to WebSocket endpoint: `ws://localhost:5002/marketdata`

Example market data message:
```json
{
  "type": "ORDER_BOOK",
  "symbol": "AAPL",
  "timestamp": 1701432000000,
  "data": {
    "symbol": "AAPL",
    "bids": [
      {"price": 149.50, "quantity": 100},
      {"price": 149.00, "quantity": 200}
    ],
    "asks": [
      {"price": 150.00, "quantity": 150},
      {"price": 150.50, "quantity": 100}
    ],
    "timestamp": 1701432000000
  }
}
```

## Testing

Run the test suite:

```bash
mvn test
```

### Test Coverage

- **Order Model**: Immutable order objects with validation
- **OrderBook**: Price-time priority matching engine
- **MatchingEngine**: Multi-symbol order processing
- **FIX Message Handling**: FIX 4.4 message parsing and routing
- **Market Data**: Real-time order book snapshots

## Performance Characteristics

- **Latency**: Sub-millisecond order processing
- **Throughput**: 10,000+ orders per second
- **Memory**: Efficient data structures with minimal GC pressure
- **Scalability**: Multi-threaded architecture supporting multiple symbols

## Low-Latency Design Principles

1. **Non-blocking I/O**: Uses Netty for high-performance networking
2. **Efficient Data Structures**: ConcurrentSkipListMap for order books
3. **Minimal Object Creation**: Immutable objects and object pooling
4. **Lock-free Operations**: Atomic operations where possible
5. **Memory Management**: Direct memory allocation and zero-copy operations

## Extending the Exchange

### Adding New FIX Message Types

1. Extend `FixMessageHandler` with new message handlers
2. Add message type constants to the FIX application
3. Update the message routing logic

### Adding New Order Types

1. Extend `Order.OrderType` enum
2. Update order validation logic in `MatchingEngine`
3. Add corresponding FIX field mappings

### Adding New Symbols

1. Update `exchange.properties` with new symbols
2. Restart the exchange or implement dynamic symbol loading

## Monitoring and Logging

The exchange provides comprehensive logging via Logback:

- **Application Logs**: `logs/trading-exchange.log`
- **FIX Logs**: `logs/` (QuickFIX/J session logs)
- **Performance Metrics**: Built-in statistics via `MatchingEngine.getStats()`

## Production Deployment

### JVM Tuning

```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=10 \
     -XX:+UseStringDeduplication \
     -jar mock-trading-exchange-1.0.0.jar
```

### System Tuning

- **CPU Affinity**: Pin exchange threads to specific CPU cores
- **Network Tuning**: Optimize TCP buffer sizes and interrupt coalescing
- **Memory**: Use huge pages for better memory management

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions and support, please open an issue on GitHub or contact the development team.
