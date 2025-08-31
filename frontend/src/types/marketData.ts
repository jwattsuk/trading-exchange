/**
 * Market data types that correspond to the Java backend
 */

export interface MarketDataMessage {
  type: 'ORDER_BOOK' | 'QUOTE' | 'TRADE';
  symbol: string;
  timestamp: number;
  data: OrderBookSnapshot | Quote | Trade;
}

export interface OrderBookSnapshot {
  symbol: string;
  bids: PriceLevel[];
  asks: PriceLevel[];
  timestamp: number;
}

export interface PriceLevel {
  price: number;
  quantity: number;
}

export interface Quote {
  symbol: string;
  bidPrice: number | null;
  bidQuantity: number;
  askPrice: number | null;
  askQuantity: number;
  spread: number;
}

export interface Trade {
  tradeId: number;
  symbol: string;
  price: number;
  quantity: number;
  timestamp: number;
  buyOrderId: number;
  sellOrderId: number;
}

export interface MarketDataState {
  orderBooks: Record<string, OrderBookSnapshot>;
  quotes: Record<string, Quote>;
  trades: Record<string, Trade[]>;
  lastUpdate: Record<string, number>;
  connectionStatus: 'connecting' | 'connected' | 'disconnected' | 'error';
  error?: string;
}

export interface WebSocketConfig {
  url: string;
  reconnectInterval: number;
  maxReconnectAttempts: number;
}
