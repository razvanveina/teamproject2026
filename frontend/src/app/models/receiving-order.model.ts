export interface ReceivingOrder {
    orderId: string;
    status: string;
    lines: ReceivingOrderLine[];
}

export interface ReceivingOrderLine {
    id: number;
    article: string;
    quantity: number;
    receivedQuantity: number;
    status: string;
    description: string;
}
