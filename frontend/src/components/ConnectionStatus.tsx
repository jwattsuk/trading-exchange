'use client';

import { useConnectionStatus, useError } from '@/store/marketDataStore';

export function ConnectionStatus() {
  const connectionStatus = useConnectionStatus();
  const error = useError();

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'connected':
        return 'bg-green-500';
      case 'connecting':
        return 'bg-yellow-500';
      case 'disconnected':
        return 'bg-gray-500';
      case 'error':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'connected':
        return 'Connected';
      case 'connecting':
        return 'Connecting...';
      case 'disconnected':
        return 'Disconnected';
      case 'error':
        return 'Connection Error';
      default:
        return 'Unknown';
    }
  };

  return (
    <div className="flex items-center gap-3 mt-4">
      <div className="flex items-center gap-2">
        <div className={`w-3 h-3 rounded-full ${getStatusColor()} animate-pulse`} />
        <span className="text-sm font-medium text-neutral-700">
          {getStatusText()}
        </span>
      </div>
      
      {error && (
        <div className="text-sm text-red-600 bg-red-50 px-3 py-1 rounded-md">
          {error}
        </div>
      )}
      
      <div className="text-xs text-neutral-500">
        WebSocket: {process.env.NEXT_PUBLIC_WEBSOCKET_URL || 'ws://localhost:5002/marketdata'}
      </div>
    </div>
  );
}
