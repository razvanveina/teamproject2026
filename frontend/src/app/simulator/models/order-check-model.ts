export interface ReservedLoadUnit {
  barcode: string;
  location: string;
}

export interface OrderCheckLine {
  article: string;
  description: string;
  quantity: number;
  reserved: number;
  free: number;
  missing: number;
  loadUnits: ReservedLoadUnit[];
}

export interface OrderCheck {
  orderId: string;
  status: string;
  result: string;
  lines: OrderCheckLine[];
}
