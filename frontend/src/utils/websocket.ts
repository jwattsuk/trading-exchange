import { MarketDataMessage } from '@/types/marketData';

export class WebSocketManager {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts: number;
  private reconnectInterval: number;
  private url: string;
  private onMessage: (message: MarketDataMessage) => void;
  private onStatusChange: (status: 'connecting' | 'connected' | 'disconnected' | 'error') => void;
  private reconnectTimeout: NodeJS.Timeout | null = null;

  constructor(
    url: string,
    onMessage: (message: MarketDataMessage) => void,
    onStatusChange: (status: 'connecting' | 'connected' | 'disconnected' | 'error') => void,
    maxReconnectAttempts = 5,
    reconnectInterval = 3000
  ) {
    this.url = url;
    this.onMessage = onMessage;
    this.onStatusChange = onStatusChange;
    this.maxReconnectAttempts = maxReconnectAttempts;
    this.reconnectInterval = reconnectInterval;
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return;
    }

    this.onStatusChange('connecting');
    
    try {
      this.ws = new WebSocket(this.url);
      
      this.ws.onopen = () => {
        console.log('WebSocket connected to:', this.url);
        this.reconnectAttempts = 0;
        this.onStatusChange('connected');
      };

      this.ws.onmessage = (event) => {
        try {
          const message: MarketDataMessage = JSON.parse(event.data);
          this.onMessage(message);
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        this.onStatusChange('disconnected');
        
        if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect();
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.onStatusChange('error');
      };

    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.onStatusChange('error');
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Max reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1);
    
    console.log(`Scheduling reconnection attempt ${this.reconnectAttempts} in ${delay}ms`);
    
    this.reconnectTimeout = setTimeout(() => {
      this.connect();
    }, delay);
  }

  send(message: MarketDataMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket is not connected');
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }
}

export const createWebSocketManager = (
  url: string,
  onMessage: (message: MarketDataMessage) => void,
  onStatusChange: (status: 'connecting' | 'connected' | 'disconnected' | 'error') => void,
  maxReconnectAttempts = 5,
  reconnectInterval = 3000
): WebSocketManager => {
  return new WebSocketManager(url, onMessage, onStatusChange, maxReconnectAttempts, reconnectInterval);
};
