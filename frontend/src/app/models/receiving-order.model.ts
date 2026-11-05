export interface ReceivingOrder {
    orderId: string;
    status: string;
    lines: ReceivingOrderLine[];
}

export interface ReceivingOrderLine {
    id: number;
    barcode: string;
    quantity: number;
    receivedQuantity: number;
    status: string;
    description: string;
}

export interface PackageInfo {
    articleBarcode: string;
    packagingUnit: string;
    quantity: number;
}