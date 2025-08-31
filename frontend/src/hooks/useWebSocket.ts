import { useEffect, useRef, useCallback, useMemo } from 'react';
import { WebSocketManager, createWebSocketManager } from '@/utils/websocket';
import { useMarketDataStore } from '@/store/marketDataStore';
import { MarketDataMessage } from '@/types/marketData';

export const useWebSocket = (url: string) => {
  const wsManagerRef = useRef<WebSocketManager | null>(null);
  
  // Memoize the store actions to prevent unnecessary re-renders
  const storeActions = useMemo(() => ({
    setConnectionStatus: useMarketDataStore.getState().setConnectionStatus,
    setError: useMarketDataStore.getState().setError,
    processMarketDataMessage: useMarketDataStore.getState().processMarketDataMessage,
  }), []);

  const handleMessage = useCallback((message: MarketDataMessage) => {
    console.log('WebSocket received message:', message);
    storeActions.processMarketDataMessage(message);
  }, [storeActions]);

  const handleStatusChange = useCallback((status: 'connecting' | 'connected' | 'disconnected' | 'error') => {
    console.log('WebSocket status change:', status);
    storeActions.setConnectionStatus(status);
    if (status === 'error') {
      storeActions.setError('WebSocket connection error');
    } else {
      storeActions.setError(undefined);
    }
  }, [storeActions]);

  const connect = useCallback(() => {
    console.log('Connecting to WebSocket:', url);
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
    console.log('Disconnecting WebSocket');
    if (wsManagerRef.current) {
      wsManagerRef.current.disconnect();
      wsManagerRef.current = null;
    }
    storeActions.setConnectionStatus('disconnected');
  }, [storeActions]);

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
