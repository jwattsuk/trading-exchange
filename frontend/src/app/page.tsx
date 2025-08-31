'use client';

import { MarketDataGrid } from '@/components/MarketDataGrid';
import { ConnectionStatus } from '@/components/ConnectionStatus';
import { useWebSocket } from '@/hooks/useWebSocket';

export default function Home() {
  const websocketUrl = process.env.NEXT_PUBLIC_WEBSOCKET_URL || 'ws://localhost:5002/marketdata';
  
  // Initialize WebSocket connection
  useWebSocket(websocketUrl);

  return (
    <main className="min-h-screen bg-neutral-50">
      <div className="container mx-auto px-4 py-6">
        <header className="mb-8">
          <h1 className="text-4xl font-bold text-neutral-900 mb-2">
            Trading Exchange Market Data
          </h1>
          <p className="text-neutral-600 text-lg">
            Real-time market data with live updates
          </p>
          <ConnectionStatus />
        </header>
        
        <MarketDataGrid />
      </div>
    </main>
  );
}
