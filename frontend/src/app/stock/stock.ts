import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DialogConfigService } from '../services/dialog-config.service';

interface StockLine {
  articleBarcode: string;
  description: string;
  packagingUnit: string;
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
  error = signal('');

  selected = computed(() => this.loadUnits().find(l => l.id === this.selectedId()) ?? null);

  ngOnInit(): void {
    forkJoin({
      loadUnits: this.service.getRows('LoadUnit'),
      items: this.service.getRows('StockItem'),
      articles: this.service.getRows('Article'),
      types: this.service.getRows('PackagingUnitType'),
      locations: this.service.getRows('StorageLocation')
    }).subscribe({
      next: data => {
        const articles = byId(data.articles);
        const types = byId(data.types);
        const locations = byId(data.locations);

        const itemsOf = new Map<number, StockLine[]>();
        for (const item of data.items.map(lowerKeys)) {
          const article = articles.get(Number(item['article_id']));
          const type = types.get(Number(item['packaging_unit_type_id']));
          const loadUnitId = Number(item['load_unit_id']);

          itemsOf.set(loadUnitId, [
            ...(itemsOf.get(loadUnitId) ?? []),
            {
              articleBarcode: String(article?.['barcode'] ?? ''),
              description: String(article?.['description'] ?? ''),
              packagingUnit: type ? `${type['name']} (${type['quantity']} pcs)` : '',
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
}