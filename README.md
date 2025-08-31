# Mock Trading Exchange Gateway

A high-performance mock trading exchange built in Java with FIX 4.4 support, designed for low-latency trading applications.

## Features

- **FIX 4.4 Protocol Support**: Accepts inbound FIX connections from multiple market participants
- **High-Performance Matching Engine**: Price-time priority order matching with efficient data structures
- **Real-time Market Data**: WebSocket-based market data streaming
- **Low-Latency Design**: Non-blocking I/O, minimal GC pressure, efficient data structures
- **Production-Grade**: Comprehensive error handling, logging, and monitoring
- **Extensible Architecture**: Easy to add new FIX message types and trading features
- **Modern Web Frontend**: Real-time market data display with AG Grid and WebSocket integration

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
                                │
                                │ WebSocket (Port 5002)
                                ▼
                    ┌─────────────────────────┐
                    │    Frontend (Port 3000) │
                    │  ┌─────────────────────┐ │
                    │  │   Next.js App       │ │
                    │  │   - AG Grid         │ │
                    │  │   - WebSocket       │ │
                    │  │   - React Query     │ │
                    │  │   - Zustand Store   │ │
                    │  └─────────────────────┘ │
                    └─────────────────────────┘
```

## Quick Start

### Prerequisites

- **Backend**: Java 17 or higher, Maven 3.6 or higher
- **Frontend**: Node.js 18 or higher, npm or yarn

### Building and Running the Backend

```bash
# Build the Java backend
mvn clean package

# Run the trading exchange
java -jar target/mock-trading-exchange-1.0.0.jar
```

The exchange will start and listen on:
- **FIX Acceptor**: Port 5001
- **Market Data**: Port 5002 (WebSocket)

### Building and Running the Frontend

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will be available at:
- **Local**: http://localhost:3000
- **Network**: http://[your-ip]:3000

## FIX Test Client

The project includes a consolidated FIX test client for testing order placement with the trading exchange.

### Features
- **FIX 4.4 Protocol Support**: Full FIX protocol implementation
- **Interactive Mode**: Command-line interface for sending orders
- **Comprehensive Logging**: Low-level debug logging for troubleshooting
- **Order Types**: Market orders, limit orders, and order cancellation
- **Real-time Feedback**: Execution reports and status updates

### Running the FIX Test Client

#### Unix/Linux/MacOS:
```bash
# Run with default settings (localhost:5001) - Smart compile (won't affect running backend)
./run-fix-client.sh

```

### Interactive Commands
Once connected, you can use these commands:
- `buy <symbol> <quantity> [price]` - Send buy order (market if no price, limit if price given)
- `sell <symbol> <quantity> [price]` - Send sell order (market if no price, limit if price given)
- `cancel <clordid> <symbol> <side> <quantity>` - Cancel an order
- `status` - Show connection status
- `quit` - Exit the client

### Examples
```
FIX> buy AAPL 100
FIX> sell MSFT 50 150.25
FIX> cancel ORDER_1234567890 AAPL buy 100
FIX> status
FIX> quit
```

### Production Build

```bash
# Build for production
npm run build

# Start production server
npm start
```

## Frontend Architecture

### Tech Stack

- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Data Grid**: AG Grid Community + Enterprise
- **State Management**: Zustand
- **Data Fetching**: React Query (TanStack Query)
- **WebSocket**: Native WebSocket API
- **Build Tool**: Vite (via Next.js)

### Project Structure

```
frontend/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── layout.tsx         # Root layout with providers
│   │   ├── page.tsx           # Main page component
│   │   └── providers.tsx      # React Query provider
│   ├── components/             # React components
│   │   ├── MarketDataGrid.tsx # Main AG Grid component
│   │   └── ConnectionStatus.tsx # WebSocket status display
│   ├── hooks/                  # Custom React hooks
│   │   ├── useWebSocket.ts    # WebSocket connection management
│   │   └── useMarketData.ts   # React Query integration
│   ├── store/                  # State management
│   │   └── marketDataStore.ts # Zustand store
│   ├── types/                  # TypeScript type definitions
│   │   └── marketData.ts      # Market data interfaces
│   └── utils/                  # Utility functions
│       ├── websocket.ts       # WebSocket utilities
│       └── formatters.ts      # Data formatting utilities
```

### Data Flow

1. **WebSocket Connection**: Connects to Java backend on port 5002
2. **Message Processing**: Receives ORDER_BOOK, QUOTE, and TRADE messages
3. **State Updates**: Updates Zustand store with new data
4. **React Query**: Provides data to components with caching
5. **AG Grid**: Displays data with real-time updates

### Key Components

#### MarketDataGrid
- **High-performance data grid** using AG Grid
- **Real-time updates** with visual indicators
- **Sortable and filterable columns**
- **Responsive design** for different screen sizes

#### WebSocket Integration
- **Automatic reconnection** with exponential backoff
- **Connection status monitoring**
- **Error handling** and recovery
- **Message parsing** and validation

#### State Management
- **Zustand store** for lightweight state management
- **React Query** for efficient data synchronization
- **Optimistic updates** for better user experience
- **Performance optimizations** with selective re-rendering

## Configuration

### Backend Configuration

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

### Frontend Configuration

Edit `frontend/.env.local` to customize:

```env
# WebSocket Configuration
NEXT_PUBLIC_WEBSOCKET_URL=ws://localhost:5002/marketdata
NEXT_PUBLIC_API_BASE_URL=http://localhost:5002
NEXT_PUBLIC_APP_NAME=Trading Exchange Market Data
NEXT_PUBLIC_UPDATE_INTERVAL_MS=100

# AG Grid License (if using enterprise features)
NEXT_PUBLIC_AG_GRID_LICENSE_KEY=your_license_key_here
```

## Usage Examples

### Connecting via FIX

Use any FIX 4.4 client to connect to `localhost:5001`:

```fix
8=FIX.4.4|9=123|35=D|49=CLIENT|56=EXCHANGE|34=1|52=20231201-10:00:00|11=12345|21=1|55=AAPL|54=1|60=20231201-10:00:00|38=100|40=2|44=150.00|10=123|
```

### WebSocket Market Data

Connect to `ws://localhost:5002/marketdata` to receive real-time updates:

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

### Frontend Features

- **Real-time market data display** with live updates
- **Interactive data grid** with sorting and filtering
- **Connection status monitoring** with visual indicators
- **Responsive design** for desktop and mobile devices
- **Performance optimizations** for high-frequency updates

## Testing

### Backend Tests

Run the Java test suite:

```bash
mvn test
```

### Frontend Tests

```bash
# Navigate to frontend directory
cd frontend

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

### Test Coverage

- **Order Model**: Immutable order objects with validation
- **OrderBook**: Price-time priority matching engine
- **MatchingEngine**: Multi-symbol order processing
- **FIX Message Handling**: FIX 4.4 message parsing and routing
- **Market Data**: Real-time order book snapshots
- **Frontend Components**: React component testing
- **WebSocket Integration**: Connection and message handling
- **State Management**: Zustand store and React Query integration

## Performance Characteristics

- **Backend Latency**: Sub-millisecond order processing
- **Backend Throughput**: 10,000+ orders per second
- **Frontend Updates**: Real-time with <100ms latency
- **Memory**: Efficient data structures with minimal GC pressure
- **Scalability**: Multi-threaded architecture supporting multiple symbols

## Low-Latency Design Principles

1. **Non-blocking I/O**: Uses Netty for high-performance networking
2. **Efficient Data Structures**: ConcurrentSkipListMap for order books
3. **Minimal Object Creation**: Immutable objects and object pooling
4. **Lock-free Operations**: Atomic operations where possible
5. **Memory Management**: Direct memory allocation and zero-copy operations
6. **Frontend Optimization**: Virtual scrolling, memoization, and selective updates

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

### Frontend Extensions

1. **New Data Types**: Add to `marketData.ts` types
2. **New Components**: Create React components in `components/` directory
3. **New Hooks**: Add custom hooks in `hooks/` directory
4. **New Grid Columns**: Extend `MarketDataGrid` column definitions

## Monitoring and Logging

The exchange provides comprehensive logging via Logback:

- **Application Logs**: `logs/trading-exchange.log`
- **FIX Logs**: `logs/` (QuickFIX/J session logs)
- **Performance Metrics**: Built-in statistics via `MatchingEngine.getStats()`
- **Frontend Logs**: Browser console and React Query DevTools
- **WebSocket Status**: Real-time connection monitoring in UI

## Production Deployment

### Backend JVM Tuning

```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=10 \
     -XX:+UseStringDeduplication \
     -jar mock-trading-exchange-1.0.0.jar
```

### Frontend Production Build

```bash
# Build optimized production bundle
npm run build

# Start production server
npm start

# Or deploy to static hosting (Vercel, Netlify, etc.)
npm run export
```

### System Tuning

- **CPU Affinity**: Pin exchange threads to specific CPU cores
- **Network Tuning**: Optimize TCP buffer sizes and interrupt coalescing
- **Memory**: Use huge pages for better memory management
- **Frontend CDN**: Use CDN for static assets and global distribution

## Troubleshooting

### Common Issues

#### Backend
- **Port conflicts**: Check if ports 5001/5002 are available
- **Memory issues**: Increase JVM heap size
- **Performance**: Monitor GC logs and thread utilization

#### Frontend
- **WebSocket connection**: Verify backend is running on port 5002
- **AG Grid errors**: Check module registration and CSS imports
- **Build errors**: Clear `.next` directory and reinstall dependencies

### Performance Monitoring

- **Backend**: Use JVM monitoring tools (JConsole, VisualVM)
- **Frontend**: React DevTools, AG Grid performance metrics
- **Network**: Monitor WebSocket message frequency and latency

## Contributing

1. Follow Java and TypeScript best practices
2. Use consistent naming conventions
3. Add proper error handling and logging
4. Test WebSocket reconnection logic
5. Verify real-time updates work correctly
6. Maintain performance characteristics
7. Update documentation for new features

## License

This project is licensed under the MIT License. See LICENSE file for details.


