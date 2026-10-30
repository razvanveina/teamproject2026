export interface Order {
    orderId: string;
    status: OrderStatus;
    lines: OrderLine[];
}

export enum OrderStatus{
    NEw, ACTIVE, STARTED, FINISHED
}

export interface OrderLine {
    article: string;
    quantity: number;
    receivedQuantity: number;
    status: OrderLineStatus;
}

export enum OrderLineStatus {
    NEw, ACTIVE, STARTED, FINISHED
}