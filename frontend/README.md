# Trading Exchange Frontend

A real-time market data display application built with Next.js, AG Grid, and WebSocket integration.

## Features

- **Real-time Market Data**: Live updates via WebSocket connection
- **High-performance Grid**: AG Grid for efficient data display
- **TypeScript**: Full type safety throughout the application
- **Responsive Design**: Tailwind CSS for modern, responsive UI
- **State Management**: Zustand for lightweight state management
- **Data Synchronization**: React Query for efficient data handling

## Tech Stack

- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Data Grid**: AG Grid Community + Enterprise
- **State Management**: Zustand
- **Data Fetching**: React Query (TanStack Query)
- **WebSocket**: Native WebSocket API
- **Build Tool**: Vite (via Next.js)

## Prerequisites

- Node.js 18+ 
- npm or yarn
- Java backend running on port 5002 (see main project README)

## Installation

1. Install dependencies:
```bash
npm install
```

2. Set up environment variables:
```bash
cp .env.local.example .env.local
```

Edit `.env.local` and configure:
```env
NEXT_PUBLIC_WEBSOCKET_URL=ws://localhost:5002/marketdata
NEXT_PUBLIC_API_BASE_URL=http://localhost:5002
NEXT_PUBLIC_APP_NAME=Trading Exchange Market Data
NEXT_PUBLIC_UPDATE_INTERVAL_MS=100
NEXT_PUBLIC_AG_GRID_LICENSE_KEY=your_license_key_here
```

## Development

Start the development server:
```bash
npm run dev
```

The application will be available at [http://localhost:3000](http://localhost:3000).

## Building for Production

Build the application:
```bash
npm run build
```

Start the production server:
```bash
npm start
```

## Project Structure

```
src/
├── app/                    # Next.js App Router
│   ├── layout.tsx         # Root layout with providers
│   ├── page.tsx           # Main page component
│   └── providers.tsx      # React Query provider
├── components/             # React components
│   ├── MarketDataGrid.tsx # Main AG Grid component
│   └── ConnectionStatus.tsx # WebSocket status display
├── hooks/                  # Custom React hooks
│   ├── useWebSocket.ts    # WebSocket connection management
│   └── useMarketData.ts   # React Query integration
├── store/                  # State management
│   └── marketDataStore.ts # Zustand store
├── types/                  # TypeScript type definitions
│   └── marketData.ts      # Market data interfaces
└── utils/                  # Utility functions
    ├── websocket.ts       # WebSocket utilities
    └── formatters.ts      # Data formatting utilities
```

## Data Flow

1. **WebSocket Connection**: Connects to Java backend on port 5002
2. **Message Processing**: Receives ORDER_BOOK, QUOTE, and TRADE messages
3. **State Updates**: Updates Zustand store with new data
4. **React Query**: Provides data to components with caching
5. **AG Grid**: Displays data with real-time updates

## WebSocket Message Format

The application expects messages in this format:
```typescript
interface MarketDataMessage {
  type: 'ORDER_BOOK' | 'QUOTE' | 'TRADE';
  symbol: string;
  timestamp: number;
  data: OrderBookSnapshot | Quote | Trade;
}
```

## Configuration

### Backend Requirements

- Java backend must be running on port 5002
- WebSocket endpoint: `/marketdata`
- CORS enabled for frontend domain
- Market data publishing enabled

### Frontend Configuration

- WebSocket URL: Configurable via environment variable
- Update interval: 100ms (configurable)
- AG Grid license: Required for enterprise features

## Troubleshooting

### WebSocket Connection Issues

1. Verify backend is running on port 5002
2. Check firewall settings
3. Verify WebSocket endpoint is accessible
4. Check browser console for connection errors

### AG Grid Issues

1. Verify license key is set (if using enterprise features)
2. Check CSS imports are correct
3. Verify column definitions match data structure

### Performance Issues

1. Check WebSocket message frequency
2. Monitor React Query cache size
3. Verify AG Grid virtualization settings

## Contributing

1. Follow TypeScript best practices
2. Use consistent naming conventions
3. Add proper error handling
4. Test WebSocket reconnection logic
5. Verify real-time updates work correctly

## License

This project is part of the Trading Exchange system. See main project for license details.
