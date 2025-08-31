import { useEffect, useRef, useCallback } from 'react';
import { WebSocketManager, createWebSocketManager } from '@/utils/websocket';
import { useMarketDataStore } from '@/store/marketDataStore';
import { MarketDataMessage } from '@/types/marketData';

export const useWebSocket = (url: string) => {
  const wsManagerRef = useRef<WebSocketManager | null>(null);
  const {
    setConnectionStatus,
    setError,
    processMarketDataMessage,
  } = useMarketDataStore();

  const handleMessage = useCallback((message: MarketDataMessage) => {
    processMarketDataMessage(message);
  }, [processMarketDataMessage]);

  const handleStatusChange = useCallback((status: 'connecting' | 'connected' | 'disconnected' | 'error') => {
    setConnectionStatus(status);
    if (status === 'error') {
      setError('WebSocket connection error');
    } else {
      setError(undefined);
    }
  }, [setConnectionStatus, setError]);

  const connect = useCallback(() => {
    if (!wsManagerRef.current) {
      wsManagerRef.current = createWebSocketManager(
        url,
        handleMessage,
        handleStatusChange,
        5, // maxReconnectAttempts
        3000 // reconnectInterval
      );
    }
    wsManagerRef.current.connect();
  }, [url, handleMessage, handleStatusChange]);

  const disconnect = useCallback(() => {
    if (wsManagerRef.current) {
      wsManagerRef.current.disconnect();
      wsManagerRef.current = null;
    }
    setConnectionStatus('disconnected');
  }, [setConnectionStatus]);

  const send = useCallback((message: MarketDataMessage) => {
    if (wsManagerRef.current) {
      wsManagerRef.current.send(message);
    }
  }, []);

  const isConnected = useCallback(() => {
    return wsManagerRef.current?.isConnected() || false;
  }, []);

  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    connect,
    disconnect,
    send,
    isConnected,
  };
};
