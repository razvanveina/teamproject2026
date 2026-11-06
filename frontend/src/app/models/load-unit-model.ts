export interface LoadUnit {
    barcode: string;
    loadUnitType: string;
    location: string;
    items: StockItem[];
}

export interface StockItem {
    id: number;
    barcode: string;
    quantity: number;
    articleBarcode: string;
    articleDescription: string;
    packagingUnitTypeName: string;
    packagingUnitTypeQuantity: number;
}
