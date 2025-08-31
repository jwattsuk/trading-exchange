'use client';

import { useMemo } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ColDef, GridOptions, ModuleRegistry, AllCommunityModule } from 'ag-grid-community';
import { useQuotesQuery } from '@/hooks/useMarketData';
import { useLastUpdate } from '@/store/marketDataStore';
import { formatPrice, formatQuantity, formatSpread, formatSpreadPercentage, formatRelativeTime } from '@/utils/formatters';
import { Quote } from '@/types/marketData';

// Register AG Grid modules
ModuleRegistry.registerModules([AllCommunityModule]);

// Import AG Grid styles
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

export function MarketDataGrid() {
  const { data: quotes = [], isLoading, error } = useQuotesQuery();
  const lastUpdate = useLastUpdate();

  const columnDefs: ColDef<Quote>[] = useMemo(() => [
    {
      headerName: 'Symbol',
      field: 'symbol',
      width: 100,
      sortable: true,
      filter: true,
      pinned: 'left',
      cellStyle: { fontWeight: 'bold', textAlign: 'left' },
    },
    {
      headerName: 'Bid Price',
      field: 'bidPrice',
      width: 120,
      sortable: true,
      filter: true,
      valueFormatter: (params) => formatPrice(params.value),
      cellStyle: (params) => ({
        color: params.value ? '#16a34a' : '#6b7280',
        fontWeight: '500',
        textAlign: 'right',
      }),
    },
    {
      headerName: 'Bid Qty',
      field: 'bidQuantity',
      width: 100,
      sortable: true,
      filter: true,
      valueFormatter: (params) => formatQuantity(params.value),
      cellStyle: { textAlign: 'right', fontWeight: 'normal' },
    },
    {
      headerName: 'Ask Price',
      field: 'askPrice',
      width: 120,
      sortable: true,
      filter: true,
      valueFormatter: (params) => formatPrice(params.value),
      cellStyle: (params) => ({
        color: params.value ? '#dc2626' : '#6b7280',
        fontWeight: '500',
        textAlign: 'right',
      }),
    },
    {
      headerName: 'Ask Qty',
      field: 'askQuantity',
      width: 100,
      sortable: true,
      filter: true,
      valueFormatter: (params) => formatQuantity(params.value),
      cellStyle: { textAlign: 'right', fontWeight: 'normal' },
    },
    {
      headerName: 'Spread',
      field: 'spread',
      width: 100,
      sortable: true,
      filter: true,
      valueFormatter: (params) => formatSpread(params.value),
      cellStyle: { textAlign: 'right', fontWeight: '500' },
    },
    {
      headerName: 'Spread %',
      field: 'spread',
      width: 120,
      sortable: true,
      filter: true,
      valueFormatter: (params) => {
        const quote = params.data as Quote;
        return formatSpreadPercentage(quote.bidPrice, quote.askPrice);
      },
      cellStyle: { textAlign: 'right', fontWeight: 'normal' },
    },
    {
      headerName: 'Last Update',
      field: 'symbol',
      width: 150,
      sortable: true,
      filter: true,
      valueFormatter: (params) => {
        const symbol = params.value;
        const timestamp = lastUpdate[symbol];
        return timestamp ? formatRelativeTime(timestamp) : '-';
      },
      cellStyle: { textAlign: 'center', fontWeight: 'normal' },
    },
  ], [lastUpdate]);

  const gridOptions: GridOptions<Quote> = useMemo(() => ({
    defaultColDef: {
      resizable: true,
      sortable: true,
      filter: true,
    },
    rowData: quotes,
    columnDefs,
    pagination: false,
    domLayout: 'autoHeight',
    animateRows: true,
    enableCellTextSelection: true,
    suppressRowClickSelection: true,
    suppressCellFocus: true,
    suppressRowHoverHighlight: true,
    suppressColumnVirtualisation: false,
    suppressRowVirtualisation: false,
    rowHeight: 40,
    headerHeight: 50,
    theme: 'legacy',
    getRowStyle: () => ({
      borderBottom: '1px solid #e5e7eb',
    }),
  }), [columnDefs, quotes]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg text-neutral-600">Loading market data...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg text-red-600">Error loading market data</div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-lg overflow-hidden">
      <div className="px-6 py-4 border-b border-neutral-200">
        <h2 className="text-xl font-semibold text-neutral-900">
          Market Data ({quotes.length} symbols)
        </h2>
        <p className="text-sm text-neutral-600 mt-1">
          Real-time quotes and order book data
        </p>
      </div>
      
      <div className="w-full" style={{ height: '600px' }}>
        <AgGridReact
          {...gridOptions}
        />
      </div>
    </div>
  );
}
