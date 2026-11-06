import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../api.config';
import { forkJoin, Observable, of, throwError } from 'rxjs';
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

interface LoadUnitRow extends BarcodeRow {
    ID: number;
    TYPE: string;
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
    templateUrl: './scan-app.html',
    styleUrl: './scan-app.css'
})

export class ScanApp {

    private readonly http = inject(HttpClient);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly dataUrl = `${API_BASE_URL}/data`;

    loadUnit = '';
    loadUnitType = '';
    private loadUnitId: number | null = null;
    article = '';
    quantity = 0;

    currentArticleDescription = '';
    currentQuantityUnit = '';
    currentQuantityPerUnit = 1;

    lines: ScanLine[] = [];
    quantityDifferences: QuantityDifference[] = [];
    comparisonComplete = false;
    comparisonError = '';
    isComparing = false;

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
                this.http
                    .get<LoadUnitRow[]>(`${this.dataUrl}/LoadUnit`, { params: { BARCODE: barcode } })
                    .subscribe({
                        next: result => {
                            const loadUnit = result.find(row => row.BARCODE === barcode);
                            if (!loadUnit) {
                                alert('Load unit not found');
                                return;
                            }

                            this.loadUnit = loadUnit.BARCODE;
                            this.loadUnitType = loadUnit.TYPE;
                            this.loadUnitId = loadUnit.ID;
                            this.step = 2;
                            this.cdr.markForCheck();
                        },
                        error: () => {
                            alert('Could not verify load unit');
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
                                        this.loadArticlePackagingUnit(
                                            articlePackagingUnit.ARTICLE_ID,
                                            packaging.ID,
                                            articlePackagingUnit
                                        ),
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

                this.lines.push({
                    article: this.article,
                    description: this.currentArticleDescription,
                    quantity,
                    quantityUnit: this.currentQuantityUnit,
                    quantityPerUnit: this.currentQuantityPerUnit
                });

                this.comparisonComplete = false;
                this.quantityDifferences = [];
                this.comparisonError = '';
                this.article = '';
                this.quantity = 0;
                this.currentArticleDescription = '';
                this.currentQuantityUnit = '';
                this.currentQuantityPerUnit = 1;

                this.step = 2;

                break;
        }
    }

    onCancel(): void {
        history.back();
    }

    private loadArticleBarcode(barcode: string): void {
        this.http
            .get<ArticleRow[]>(`${this.dataUrl}/Article`, { params: { BARCODE: barcode } })
            .subscribe({
                next: rows => {
                    const article = rows.find(row => row.BARCODE === barcode);
                    if (!article) {
                        alert('Article or article packaging unit not found');
                        return;
                    }
                    this.loadArticlePackagingUnit(article.ID, null);
                },
                error: () => alert('Could not fetch article')
            });
    }

    private loadArticlePackagingUnit(
        articleId: number,
        scannedPackagingId: number | null,
        scannedPackaging?: ArticlePackagingUnitRecord
    ): void {
        if (this.loadUnitId === null) {
            alert('Load unit has not been selected');
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
                        if (scannedPackaging) {
                            this.loadArticlePackagingUnitDetails(scannedPackaging);
                        } else {
                            alert('No stock items found for this load unit');
                        }
                        return;
                    }

                    forkJoin(
                        stockItems.map(item =>
                            this.http
                                .get<ArticlePackagingUnitRecord>(
                                    `${this.dataUrl}/ArticlePackagingUnitType/${item.ARTICLE_PACKAGING_UNIT_TYPE_ID}`
                                )
                                .pipe(map(packaging => ({ item, packaging })))
                        )
                    ).subscribe({
                        next: stockPackaging => {
                            const matchingArticle = stockPackaging.filter(
                                entry => entry.packaging.ARTICLE_ID === articleId
                            );
                            if (matchingArticle.length === 0) {
                                if (scannedPackaging) {
                                    this.loadArticlePackagingUnitDetails(scannedPackaging);
                                } else {
                                    alert('Article is not on this load unit');
                                }
                                return;
                            }

                            const selectedPackaging =
                                matchingArticle.find(
                                    entry =>
                                        scannedPackagingId !== null &&
                                        entry.item.ARTICLE_PACKAGING_UNIT_TYPE_ID === scannedPackagingId
                                )?.packaging ?? matchingArticle[0].packaging;
                            this.loadArticlePackagingUnitDetails(selectedPackaging);
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
        const data = {
            loadUnit: this.loadUnit,
            lines: this.lines
        };

        localStorage.setItem('scanData', JSON.stringify(data));
        console.log('Saved:', data);

        this.isComparing = true;
        this.comparisonComplete = false;
        this.comparisonError = '';

        this.getSavedStockItems(this.loadUnit)
            .subscribe({
                next: stockItems => {
                    this.compareQuantities(stockItems);
                    this.isComparing = false;
                    this.cdr.markForCheck();
                },
                error: () => {
                    this.comparisonError = 'Could not load stock items for this load unit.';
                    this.isComparing = false;
                    this.cdr.markForCheck();
                }
            });
    }

    private getSavedStockItems(loadUnitBarcode: string): Observable<SavedStockItem[]> {
        return this.http
            .get<LoadUnitRow[]>(`${this.dataUrl}/LoadUnit`, { params: { BARCODE: loadUnitBarcode } })
            .pipe(
                map(rows => rows.find(row => row.BARCODE === loadUnitBarcode)),
                switchMap(loadUnit => {
                    if (!loadUnit) {
                        return throwError(() => new Error('Load unit not found'));
                    }

                    return this.http
                        .get<StockItemRow[]>(`${this.dataUrl}/StockItem`, {
                            params: { LOAD_UNIT_ID: String(loadUnit.ID) }
                        })
                        .pipe(
                            map(rows => rows.filter(row => String(row.LOAD_UNIT_ID) === String(loadUnit.ID))),
                            switchMap(stockItems => {
                                if (stockItems.length === 0) {
                                    return of([]);
                                }
                                return forkJoin(stockItems.map(item => this.getSavedStockItem(item)));
                            })
                        );
                })
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
        this.comparisonComplete = true;
    }

    get currentPlaceholder(): string {
        return this.currentInstruction;
    }
}