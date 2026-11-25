export interface WorkplaceRoute {
  path: string;
  queryParams?: Record<string, string>;
}

export const WORKPLACE_ROUTES: Record<string, WorkplaceRoute> = {
  transport: { path: '/transport' },
  receiving: { path: '/receiving' },
  decanting: { path: '/decanting' },
  shipping: { path: '/shipping' },
  toteTransport: { path: '/toteTransport' },
  inventory: { path: '/inventory' },
  picking_1: { path: '/sim/picking', queryParams: { station: '1' } },
  picking_2: { path: '/sim/picking', queryParams: { station: '2' } },
  picking_3: { path: '/sim/picking', queryParams: { station: '3' } },
};
