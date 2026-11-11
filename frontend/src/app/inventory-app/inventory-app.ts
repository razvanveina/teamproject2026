import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../api.config';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export interface ScanLine {
    article: string;
    description: string;
    quantity: number;
    quantityUnit: string;
    quantityPerUnit: number;
}

interface QuantityDifference {
    article: string;
    description: string;
    packagingUnit: string;
    sqlQuantity: number;
    scannedQuantity: number;
    difference: number;
}

interface BarcodeRow {
    BARCODE: string;
}

interface EligibleLoadUnit {
    id: number;
    barcode: string;
    loadUnitType: string;
    locationName: string;
}

interface InventoryOrderItem {
    articleBarcode: string;
    articleDescription: string;
    packagingUnitName: string;
    quantity: number;
}

interface InventoryOrderLine {
    loadUnitBarcode: string;
    loadUnitType: string;
    locationName: string;
    items: InventoryOrderItem[];
}

interface ArticleRow extends BarcodeRow {
    ID: number;
    DESCRIPTION: string;
}

interface ArticlePackagingUnitLookupRow extends BarcodeRow {
    ID: number;
}

interface PackagingUnitTypeRow {
    NAME: string;
    QUANTITY: number;
}

interface StockItemRow {
    ID: number;
    QUANTITY: number;
    LOAD_UNIT_ID: number;
    ARTICLE_PACKAGING_UNIT_TYPE_ID: number;
}

interface ArticlePackagingUnitRecord {
    ARTICLE_ID: number;
    PACKAGING_UNIT_TYPE_ID: number;
}

interface SavedStockItem {
    articleBarcode: string;
    articleDescription: string;
    packagingQuantity: number;
    packagingUnit: string;
}

@Component({
    selector: 'app-scan-app',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './inventory-app.html',
    styleUrl: './inventory-app.css'
})

export class InventoryApp {

    private readonly http = inject(HttpClient);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly dataUrl = `${API_BASE_URL}/data`;

    loadUnit = '';
    loadUnitType = '';
    locationName = '';
    private loadUnitId: number | null = null;
    article = '';
    quantity = 0;

    currentArticleDescription = '';
    currentQuantityUnit = '';
    currentQuantityPerUnit = 1;

    lines: ScanLine[] = [];
    completedLines: InventoryOrderLine[] = [];
    quantityDifferences: QuantityDifference[] = [];
    comparisonComplete = false;
    showMismatchDialog = false;
    showCompletionDialog = false;
    comparisonError = '';
    isComparing = false;
    isSaving = false;

    step = 1;

    get currentInstruction(): string {
        switch (this.step) {
            case 1:
                return 'Scan Load Unit barcode';

            case 2:
                return 'Scan article/package barcode';

            case 3:
                return 'Enter quantity';

            default:
                return '';
        }
    }

    onScan(value: string): void {
        const barcode = value.trim();
        if (!barcode) {
            return;
        }
        switch (this.step) {

            case 1:
                this.isComparing = true;
                this.comparisonError = '';
                this.http
                    .get<EligibleLoadUnit>(
                        `${API_BASE_URL}/inventory-orders/load-units/${encodeURIComponent(barcode)}`
                    )
                    .subscribe({
                        next: loadUnit => {
                            this.isComparing = false;
                            if (this.completedLines.some(line => line.loadUnitBarcode === loadUnit.barcode)) {
                                this.comparisonError = 'This load unit is already in the inventory order.';
                                this.cdr.markForCheck();
                                return;
                            }
                            this.loadUnit = loadUnit.barcode;
                            this.loadUnitType = loadUnit.loadUnitType;
                            this.locationName = loadUnit.locationName;
                            this.loadUnitId = loadUnit.id;
                            this.lines = [];
                            this.comparisonError = '';
                            this.step = 2;
                            this.cdr.markForCheck();
                        },
                        error: error => {
                            this.isComparing = false;
                            this.comparisonError =
                                error.error?.message ?? 'Could not verify this load unit is eligible.';
                            this.cdr.markForCheck();
                        }
                    });
                break;

            case 2:
                this.http
                    .get<ArticlePackagingUnitLookupRow[]>(`${this.dataUrl}/ArticlePackagingUnitType`, {
                        params: { BARCODE: barcode }
                    })
                    .subscribe({
                        next: packagingRows => {
                            const packaging = packagingRows.find(row => row.BARCODE === barcode);
                            if (!packaging) {
                                this.loadArticleBarcode(barcode);
                                return;
                            }

                            this.http
                                .get<ArticlePackagingUnitRecord>(
                                    `${this.dataUrl}/ArticlePackagingUnitType/${packaging.ID}`
                                )
                                .subscribe({
                                    next: articlePackagingUnit =>
                                        this.loadArticlePackagingUnitDetails(articlePackagingUnit),
                                    error: () => alert('Could not fetch article packaging unit')
                                });
                        },
                        error: () => {
                            alert('Could not fetch article packaging unit');
                        }
                    });
                break;

            case 3:

                const quantity = Number(barcode);
                if (!Number.isSafeInteger(quantity) || quantity < 0) {
                    this.comparisonError = 'Enter a whole-number quantity of zero or more.';
                    this.cdr.markForCheck();
                    return;
                }

                this.lines.push({
                    article: this.article,
                    description: this.currentArticleDescription,
                    quantity,
                    quantityUnit: this.currentQuantityUnit,
                    quantityPerUnit: this.currentQuantityPerUnit
                });

                this.quantityDifferences = [];
                this.comparisonError = '';
                this.article = '';
                this.quantity = 0;
                this.currentArticleDescription = '';
                this.currentQuantityUnit = '';
                this.currentQuantityPerUnit = 1;
                this.comparisonError = '';

                this.step = 2;

                break;
        }
    }

    onCancel(): void {
        history.back();
    }

    removeLine(index: number): void {
        if (index < 0 || index >= this.lines.length) {
            return;
        }

        this.lines = this.lines.filter((_, lineIndex) => lineIndex !== index);
        this.quantityDifferences = [];
        this.comparisonComplete = false;
        this.showMismatchDialog = false;
        this.showCompletionDialog = false;
        this.comparisonError = '';
    }

    removeOrderLine(index: number): void {
        if (index < 0 || index >= this.completedLines.length) {
            return;
        }
        this.completedLines = this.completedLines.filter((_, lineIndex) => lineIndex !== index);
    }

    private loadArticleBarcode(barcode: string): void {
        this.http
            .get<ArticleRow[]>(`${this.dataUrl}/Article`, { params: { BARCODE: barcode } })
            .subscribe({
                next: rows => {
                    const article = rows.find(row => row.BARCODE === barcode);
                    if (!article) {
                        this.prepareUnknownArticle(barcode);
                        return;
                    }
                    this.loadArticlePackagingUnit(article.ID);
                },
                error: () => alert('Could not fetch article')
            });
    }

    private prepareUnknownArticle(barcode: string): void {
        this.article = barcode;
        this.currentArticleDescription = 'Unknown article';
        this.currentQuantityUnit = 'unit';
        this.currentQuantityPerUnit = 1;
        this.step = 3;
        this.cdr.markForCheck();
    }

    private loadArticleAsUnit(articleId: number): void {
        this.http.get<ArticleRow>(`${this.dataUrl}/Article/${articleId}`).subscribe({
            next: article => {
                this.article = article.BARCODE;
                this.currentArticleDescription = article.DESCRIPTION;
                this.currentQuantityUnit = 'unit';
                this.currentQuantityPerUnit = 1;
                this.step = 3;
                this.cdr.markForCheck();
            },
            error: () => alert('Could not fetch article')
        });
    }

    private loadArticlePackagingUnit(articleId: number): void {
        if (this.loadUnitId === null) {
            this.loadArticleAsUnit(articleId);
            return;
        }

        this.http
            .get<StockItemRow[]>(`${this.dataUrl}/StockItem`, {
                params: { LOAD_UNIT_ID: String(this.loadUnitId) }
            })
            .subscribe({
                next: rows => {
                    const stockItems = rows.filter(
                        item => String(item.LOAD_UNIT_ID) === String(this.loadUnitId)
                    );
                    if (stockItems.length === 0) {
                        this.loadArticleAsUnit(articleId);
                        return;
                    }

                    forkJoin(
                        stockItems.map(item =>
                            this.http
                                .get<ArticlePackagingUnitRecord>(
                                    `${this.dataUrl}/ArticlePackagingUnitType/${item.ARTICLE_PACKAGING_UNIT_TYPE_ID}`
                                )
                        )
                    ).subscribe({
                        next: stockPackaging => {
                            const matchingArticle = stockPackaging.filter(
                                packaging => packaging.ARTICLE_ID === articleId
                            );
                            if (matchingArticle.length === 0) {
                                this.loadArticleAsUnit(articleId);
                                return;
                            }

                            this.loadArticlePackagingUnitDetails(matchingArticle[0]);
                        },
                        error: () => alert('Could not inspect the load unit stock packaging')
                    });
                },
                error: () => alert('Could not load stock items for this load unit')
            });
    }

    private loadArticlePackagingUnitDetails(articlePackagingUnit: ArticlePackagingUnitRecord): void {
        forkJoin({
            article: this.http.get<ArticleRow>(`${this.dataUrl}/Article/${articlePackagingUnit.ARTICLE_ID}`),
            packagingUnit: this.http.get<PackagingUnitTypeRow>(
                `${this.dataUrl}/PackagingUnitType/${articlePackagingUnit.PACKAGING_UNIT_TYPE_ID}`
            )
        }).subscribe({
            next: ({ article, packagingUnit }) => {
                this.article = article.BARCODE;
                this.currentArticleDescription = article.DESCRIPTION;
                this.currentQuantityPerUnit = packagingUnit.QUANTITY;
                this.currentQuantityUnit = packagingUnit.NAME;
                this.step = 3;
                this.cdr.markForCheck();
            },
            error: () => {
                alert('Could not fetch article or packaging type');
            }
        });
    }

    onFinish(): void {
        if (!this.loadUnit || this.loadUnitId === null || this.isComparing || this.isSaving) {
            return;
        }
        this.isComparing = true;
        this.showMismatchDialog = false;
        this.comparisonError = '';

        this.getSavedStockItems(this.loadUnitId)
            .subscribe({
                next: stockItems => {
                    this.compareQuantities(stockItems);
                    this.isComparing = false;
                    if (this.quantityDifferences.length === 0) {
                        this.completeCurrentLine();
                    }
                    this.cdr.markForCheck();
                },
                error: () => {
                    this.comparisonError = 'Could not load stock items for this load unit.';
                    this.isComparing = false;
                    this.cdr.markForCheck();
                }
            });
    }

    private getSavedStockItems(loadUnitId: number): Observable<SavedStockItem[]> {
        return this.http
            .get<StockItemRow[]>(`${this.dataUrl}/StockItem`, {
                params: { LOAD_UNIT_ID: String(loadUnitId) }
            })
            .pipe(
                map(rows => rows.filter(row => String(row.LOAD_UNIT_ID) === String(loadUnitId))),
                switchMap(stockItems => stockItems.length === 0
                    ? of<SavedStockItem[]>([])
                    : forkJoin(stockItems.map(item => this.getSavedStockItem(item))))
            );
    }

    private getSavedStockItem(stockItem: StockItemRow): Observable<SavedStockItem> {
        return this.http
            .get<ArticlePackagingUnitRecord>(
                `${this.dataUrl}/ArticlePackagingUnitType/${stockItem.ARTICLE_PACKAGING_UNIT_TYPE_ID}`
            )
            .pipe(
                switchMap(articlePackagingUnit =>
                    forkJoin({
                        article: this.http.get<ArticleRow>(
                            `${this.dataUrl}/Article/${articlePackagingUnit.ARTICLE_ID}`
                        ),
                        packagingUnitType: this.http.get<PackagingUnitTypeRow>(
                            `${this.dataUrl}/PackagingUnitType/${articlePackagingUnit.PACKAGING_UNIT_TYPE_ID}`
                        )
                    }).pipe(
                        map(({ article, packagingUnitType }) => ({
                            articleBarcode: article.BARCODE,
                            articleDescription: article.DESCRIPTION,
                            packagingQuantity: stockItem.QUANTITY,
                            packagingUnit: packagingUnitType.NAME
                        }))
                    )
                )
            );
    }

    private compareQuantities(savedStockItems: SavedStockItem[]): void {
        const quantities = new Map<string, QuantityDifference>();

        const addQuantity = (
            article: string,
            description: string,
            packagingUnit: string,
            quantity: number,
            source: 'sql' | 'scanned'
        ): void => {
            const key = JSON.stringify([article, packagingUnit]);
            const comparison = quantities.get(key) ?? {
                article,
                description,
                packagingUnit,
                sqlQuantity: 0,
                scannedQuantity: 0,
                difference: 0
            };

            if (source === 'sql') {
                comparison.sqlQuantity += quantity;
            } else {
                comparison.scannedQuantity += quantity;
            }
            quantities.set(key, comparison);
        };

        for (const item of savedStockItems) {
            addQuantity(
                item.articleBarcode,
                item.articleDescription,
                item.packagingUnit,
                item.packagingQuantity,
                'sql'
            );
        }

        for (const line of this.lines) {
            addQuantity(
                line.article,
                line.description,
                line.quantityUnit,
                line.quantity,
                'scanned'
            );
        }

        this.quantityDifferences = [...quantities.values()]
            .map(comparison => ({
                ...comparison,
                difference: comparison.scannedQuantity - comparison.sqlQuantity
            }))
            .filter(comparison => comparison.difference !== 0);
        if (this.quantityDifferences.length > 0) {
            this.showMismatchDialog = true;
        }
    }

    private completeCurrentLine(): void {
        this.completedLines.push({
            loadUnitBarcode: this.loadUnit,
            loadUnitType: this.loadUnitType,
            locationName: this.locationName,
            items: this.lines.map(line => ({
                articleBarcode: line.article,
                articleDescription: line.description,
                packagingUnitName: line.quantityUnit,
                quantity: line.quantity
            }))
        });
        this.loadUnit = '';
        this.loadUnitType = '';
        this.locationName = '';
        this.loadUnitId = null;
        this.lines = [];
        this.quantityDifferences = [];
        this.comparisonError = '';
        this.step = 1;
        this.cdr.markForCheck();
    }

    onFinishOrder(): void {
        if (!this.completedLines.length || this.loadUnit || this.isComparing || this.isSaving) {
            return;
        }

        this.isSaving = true;
        this.comparisonError = '';

        const request = {
            lines: this.completedLines.map(line => ({
                loadUnitBarcode: line.loadUnitBarcode,
                items: line.items
            }))
        };
        this.http.post<void>(`${API_BASE_URL}/inventory-orders`, request).subscribe({
            next: () => {
                this.isSaving = false;
                this.comparisonComplete = true;
                this.showCompletionDialog = true;
                this.cdr.markForCheck();
            },
            error: error => {
                this.isSaving = false;
                this.comparisonError =
                    error.error?.message ?? 'Could not save the inventory order. Please try again.';
                this.cdr.markForCheck();
            }
        });
    }

    confirmMismatch(): void {
        this.showMismatchDialog = false;
        this.completeCurrentLine();
    }

    cancelMismatch(): void {
        this.showMismatchDialog = false;
        this.quantityDifferences = [];
    }

    startAnotherScan(): void {
        this.loadUnit = '';
        this.loadUnitType = '';
        this.locationName = '';
        this.loadUnitId = null;
        this.article = '';
        this.quantity = 0;
        this.currentArticleDescription = '';
        this.currentQuantityUnit = '';
        this.currentQuantityPerUnit = 1;
        this.lines = [];
        this.completedLines = [];
        this.quantityDifferences = [];
        this.comparisonComplete = false;
        this.showMismatchDialog = false;
        this.showCompletionDialog = false;
        this.comparisonError = '';
        this.isComparing = false;
        this.isSaving = false;
        this.step = 1;
        localStorage.removeItem('scanData');
    }

    get currentPlaceholder(): string {
        return this.currentInstruction;
    }
}