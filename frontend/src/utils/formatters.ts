/**
 * Utility functions for formatting market data
 */

export const formatPrice = (price: number | null, decimals: number = 2): string => {
  if (price === null || price === undefined) return '-';
  return price.toFixed(decimals);
};

export const formatQuantity = (quantity: number): string => {
  if (quantity >= 1000000) {
    return (quantity / 1000000).toFixed(1) + 'M';
  } else if (quantity >= 1000) {
    return (quantity / 1000).toFixed(1) + 'K';
  }
  return quantity.toString();
};

export const formatSpread = (spread: number): string => {
  return formatPrice(spread, 2);
};

export const formatSpreadPercentage = (bidPrice: number | null, askPrice: number | null): string => {
  if (bidPrice === null || askPrice === null || bidPrice === 0) return '-';
  const spread = askPrice - bidPrice;
  const percentage = (spread / bidPrice) * 100;
  return percentage.toFixed(2) + '%';
};

export const formatTimestamp = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3
  });
};

export const formatRelativeTime = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;
  
  if (diff < 1000) return 'now';
  if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`;
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  return `${Math.floor(diff / 3600000)}h ago`;
};

export const getPriceChangeColor = (currentPrice: number, previousPrice: number | null): string => {
  if (previousPrice === null) return 'text-neutral-600';
  if (currentPrice > previousPrice) return 'text-bid-600';
  if (currentPrice < previousPrice) return 'text-ask-600';
  return 'text-neutral-600';
};

export const getBidAskColor = (side: 'bid' | 'ask'): string => {
  return side === 'bid' ? 'text-bid-600' : 'text-ask-600';
};

export const getBidAskBgColor = (side: 'bid' | 'ask'): string => {
  return side === 'bid' ? 'bg-bid-50' : 'bg-ask-50';
};

export const calculateSpread = (bidPrice: number | null, askPrice: number | null): number => {
  if (bidPrice === null || askPrice === null) return 0;
  return askPrice - bidPrice;
};

export const calculateMidPrice = (bidPrice: number | null, askPrice: number | null): number | null => {
  if (bidPrice === null || askPrice === null) return null;
  return (bidPrice + askPrice) / 2;
};
