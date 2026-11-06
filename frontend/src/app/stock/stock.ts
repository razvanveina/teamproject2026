import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DialogConfigService } from '../services/dialog-config.service';

interface StockLine {
    articleBarcode: string;
    description: string;
    packaging: string;
    quantity: number;
}

interface LoadUnitRow {
    id: number;
    barcode: string;
    type: string;
    location: string;
    items: StockLine[];
}

const lowerKeys = (row: Record<string, any>): Record<string, any> =>
    Object.fromEntries(Object.entries(row).map(([key, value]) => [key.toLowerCase(), value]));

const byId = (rows: Record<string, any>[]): Map<number, Record<string, any>> =>
    new Map(rows.map(lowerKeys).map(r => [Number(r['id']), r] as [number, Record<string, any>]));

@Component({
    selector: 'app-stock',
    standalone: true,
    templateUrl: './stock.html',
    styleUrl: './stock.css'
})
export class Stock implements OnInit {
    private service = inject(DialogConfigService);

    loadUnits = signal<LoadUnitRow[]>([]);
    selectedId = signal<number | null>(null);
    barcodeFilter = signal('');
    locationFilter = signal('');
    error = signal('');

    selected = computed(() => this.loadUnits().find(l => l.id === this.selectedId()) ?? null);

    filtered = computed(() => {
        const barcode = this.barcodeFilter().trim().toLowerCase();
        const location = this.locationFilter().trim().toLowerCase();
        return this.loadUnits().filter(
            l => l.barcode.toLowerCase().includes(barcode) && l.location.toLowerCase().includes(location)
        );
    });

    ngOnInit(): void {
        forkJoin({
            loadUnits: this.service.getRows('LoadUnit'),
            items: this.service.getRows('StockItem'),
            packages: this.service.getRows('ArticlePackagingUnitType'),
            packagingTypes: this.service.getRows('PackagingUnitType'),
            articles: this.service.getRows('Article'),
            locations: this.service.getRows('StorageLocation')
        }).subscribe({
            next: data => {
                const packagingTypes = byId(data.packagingTypes);
                const articles = byId(data.articles);
                const locations = byId(data.locations);
                const packages = byId(data.packages);

                const itemsOf = new Map<number, StockLine[]>();
                for (const item of data.items.map(lowerKeys)) {
                    // stock item -> package -> article
                    const pack = packages.get(Number(item['article_packaging_unit_type_id']));
                    const packaging = packages.get(Number(item['article_packaging_unit_type_id']));
                    const article = articles.get(Number(packaging?.['article_id']));
                    const packagingType = packagingTypes.get(Number(packaging?.['packaging_unit_type_id']));
                    const loadUnitId = Number(item['load_unit_id']);

                    itemsOf.set(loadUnitId, [
                        ...(itemsOf.get(loadUnitId) ?? []),
                        {
                            articleBarcode: String(article?.['barcode'] ?? ''),
                            description: String(article?.['description'] ?? ''),
                            packaging: String(packagingType?.['name'] ?? ''),
                            quantity: Number(item['quantity'])
                        }
                    ]);
                }

                this.loadUnits.set(
                    data.loadUnits
                        .map(lowerKeys)
                        .map(r => ({
                            id: Number(r['id']),
                            barcode: String(r['barcode']),
                            type: String(r['type']),
                            location: String(locations.get(Number(r['location_id']))?.['name'] ?? ''),
                            items: itemsOf.get(Number(r['id'])) ?? []
                        }))
                        .sort((a, b) => a.id - b.id)
                );
            },
            error: err => {
                console.error('Could not load the stock', err);
                this.error.set('Could not load the stock.');
            }
        });
    }

    select(loadUnit: LoadUnitRow): void {
        this.selectedId.set(loadUnit.id);
    }

    setBarcodeFilter(event: Event): void {
        this.barcodeFilter.set((event.target as HTMLInputElement).value);
    }

    setLocationFilter(event: Event): void {
        this.locationFilter.set((event.target as HTMLInputElement).value);
    }
}