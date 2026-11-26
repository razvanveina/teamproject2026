export interface OrderCheckLine {
  article: string;
  description: string;
  needed: number;
  free: number;
  missing: number;
}

export interface OrderCheck {
  orderId: string;
  status: string;
  result: string;
  lines: OrderCheckLine[];
}
