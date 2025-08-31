import { create } from 'zustand';
import { MarketDataState, MarketDataMessage, OrderBookSnapshot, Quote, Trade } from '@/types/marketData';

interface MarketDataActions {
  // WebSocket connection management
  setConnectionStatus: (status: 'connecting' | 'connected' | 'disconnected' | 'error') => void;
  setError: (error: string | undefined) => void;
  
  // Market data updates
  updateOrderBook: (snapshot: OrderBookSnapshot) => void;
  updateQuote: (quote: Quote) => void;
  updateTrade: (trade: Trade) => void;
  
  // Batch updates
  processMarketDataMessage: (message: MarketDataMessage) => void;
  
  // Data management
  clearData: () => void;
  removeSymbol: (symbol: string) => void;
}

type MarketDataStore = MarketDataState & MarketDataActions;

const initialState: MarketDataState = {
  orderBooks: {},
  quotes: {},
  trades: {},
  lastUpdate: {},
  connectionStatus: 'disconnected',
  error: undefined,
};

export const useMarketDataStore = create<MarketDataStore>((set, get) => ({
  ...initialState,

  setConnectionStatus: (status) => {
    set({ connectionStatus: status });
  },

  setError: (error) => {
    set({ error });
  },

  updateOrderBook: (snapshot) => {
    set((state) => {
      // Only update if the data has actually changed
      const existing = state.orderBooks[snapshot.symbol];
      if (existing && 
          existing.timestamp === snapshot.timestamp &&
          JSON.stringify(existing) === JSON.stringify(snapshot)) {
        return state; // No change, return same state
      }
      
      console.log('Updating order book for symbol:', snapshot.symbol);
      
      return {
        orderBooks: {
          ...state.orderBooks,
          [snapshot.symbol]: snapshot,
        },
        lastUpdate: {
          ...state.lastUpdate,
          [snapshot.symbol]: snapshot.timestamp,
        },
      };
    });
  },

  updateQuote: (quote) => {
    set((state) => {
      // Only update if the data has actually changed
      const existing = state.quotes[quote.symbol];
      if (existing && 
          existing.bidPrice === quote.bidPrice &&
          existing.askPrice === quote.askPrice &&
          existing.bidQuantity === quote.bidQuantity &&
          existing.askQuantity === quote.askQuantity) {
        return state; // No change, return same state
      }
      
      console.log('Updating quote for symbol:', quote.symbol);
      
      return {
        quotes: {
          ...state.quotes,
          [quote.symbol]: quote,
        },
        lastUpdate: {
          ...state.lastUpdate,
          [quote.symbol]: Date.now(),
        },
      };
    });
  },

  updateTrade: (trade) => {
    set((state) => {
      const existingTrades = state.trades[trade.symbol] || [];
      const updatedTrades = [trade, ...existingTrades.slice(0, 99)]; // Keep last 100 trades
      
      console.log('Updating trades for symbol:', trade.symbol);
      
      return {
        trades: {
          ...state.trades,
          [trade.symbol]: updatedTrades,
        },
        lastUpdate: {
          ...state.lastUpdate,
          [trade.symbol]: Date.now(),
        },
      };
    });
  },

  processMarketDataMessage: (message) => {
    const { type, data } = message;
    
    console.log('Processing market data message:', type, data);
    
    switch (type) {
      case 'ORDER_BOOK':
        get().updateOrderBook(data as OrderBookSnapshot);
        break;
      case 'QUOTE':
        get().updateQuote(data as Quote);
        break;
      case 'TRADE':
        get().updateTrade(data as Trade);
        break;
      default:
        console.warn('Unknown message type:', type);
    }
  },

  clearData: () => {
    set(initialState);
  },

  removeSymbol: (symbol: string) => {
    set((state) => {
      const { [symbol]: _removedOrderBook, ...remainingOrderBooks } = state.orderBooks;
      const { [symbol]: _removedQuote, ...remainingQuotes } = state.quotes;
      const { [symbol]: _removedTrades, ...remainingTrades } = state.trades;
      const { [symbol]: _removedUpdate, ...remainingUpdates } = state.lastUpdate;

      return {
        orderBooks: remainingOrderBooks,
        quotes: remainingQuotes,
        trades: remainingTrades,
        lastUpdate: remainingUpdates,
      };
    });
  },
}));

// Selector hooks for better performance
export const useOrderBooks = () => useMarketDataStore((state) => state.orderBooks);
export const useQuotes = () => useMarketDataStore((state) => state.quotes);
export const useTrades = () => useMarketDataStore((state) => state.trades);
export const useConnectionStatus = () => useMarketDataStore((state) => state.connectionStatus);
export const useError = () => useMarketDataStore((state) => state.error);
export const useLastUpdate = () => useMarketDataStore((state) => state.lastUpdate);

export const useSymbols = () => useMarketDataStore((state) => Object.keys(state.quotes));
export const useSymbolData = (symbol: string) => useMarketDataStore((state) => ({
  orderBook: state.orderBooks[symbol],
  quote: state.quotes[symbol],
  trades: state.trades[symbol] || [],
  lastUpdate: state.lastUpdate[symbol],
}));
