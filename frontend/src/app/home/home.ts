import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DialogConfigService } from '../services/dialog-config.service';

interface Slot {
  name: string;
  aisle: number;
  x: number;
  y: number;
  z: number;
  loadUnitId: number | null;
}

interface StockLine {
  barcode: string;
  description: string;
  quantity: number;
}

const range = (n: number) => Array.from({ length: n }, (_, i) => i + 1);

// convertim cheile unui obiect in lower case, pentru a nu mai fi nevoie sa facem asta de fiecare data cand citim un rand din backend
const lowerKeys = (row: Record<string, any>): Record<string, any> =>
  Object.fromEntries(Object.entries(row).map(([key, value]) => [key.toLowerCase(), value]));

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.html',
  styleUrl: './home.css',
  host: { '(document:keydown.escape)': 'closePopup()' }
})
export class Home implements OnInit {
  private service = inject(DialogConfigService);

  private slots = signal<Slot[]>([]);
  selectedAisle = signal<number | null>(null);
  selectedSlot = signal<Slot | null>(null);
  stock = signal<StockLine[]>([]);
  loadingStock = signal(false);
  stockError = signal(false);

  aisles = computed(() => [...new Set(this.slots().map(s => s.aisle))].sort((a, b) => a - b));

  grid = computed(() => {
    const aisle = this.selectedAisle();
    if (aisle === null) {
      return null;
    }

    const slots = this.slots().filter(s => s.aisle === aisle);

    const slotAt = new Map<string, Slot>();
    for (const slot of slots) {
      slotAt.set(`${slot.z}-${slot.y}-${slot.x}`, slot);
    }

    const maxX = Math.max(...slots.map(s => s.x));
    const maxY = Math.max(...slots.map(s => s.y));
    const sides = [...new Set(slots.map(s => s.z))].sort((a, b) => a - b);

    return {
      columns: range(maxX),
      sides: sides.map(z => ({
        z,
        label: z === 1 ? 'Left side' : z === 2 ? 'Right side' : 'Side',
        rows: range(maxY).map(y => ({
          y,
          cells: range(maxX).map(x => slotAt.get(`${z}-${y}-${x}`) ?? null)
        }))
      }))
    };
  });

  ngOnInit(): void {
    this.service.getRows('StorageLocation').subscribe({
      next: rows => {
        const slots = rows
          .map(lowerKeys)
          .map(r => ({
            name: String(r['name']),
            aisle: Number(r['aisle']),
            x: Number(r['x']),
            y: Number(r['y']),
            z: Number(r['z']),
            loadUnitId: r['load_unit_id'] == null ? null : Number(r['load_unit_id'])
          }))
          // transit location-urile nu au culoar si locatiile de picking au culoarul 0
          // filtram doar locatiile de depozitare cu un culoar existent
          .filter(slot => slot.aisle > 0);

        this.slots.set(slots);
        this.selectedAisle.set(this.aisles()[0] ?? null);
      },
      error: err => console.error('Could not load the storage locations', err)
    });
  }

  selectAisle(event: Event): void {
    this.selectedAisle.set(Number((event.target as HTMLSelectElement).value));
  }

  openSlot(slot: Slot): void {
    this.selectedSlot.set(slot);
    this.stock.set([]);
    this.stockError.set(false);

    if (slot.loadUnitId === null) {
      this.loadingStock.set(false);
      return;
    }

    this.loadingStock.set(true);
    forkJoin({
      items: this.service.getRows('StockItem'),
      articles: this.service.getRows('Article')
    }).subscribe({
      next: ({ items, articles }) => {
        if (this.selectedSlot() !== slot) {
          return;
        }

        const articleById = new Map<number, Record<string, any>>();
        for (const article of articles.map(lowerKeys)) {
          articleById.set(Number(article['id']), article);
        }

        this.stock.set(
          items
            .map(lowerKeys)
            .filter(item => Number(item['load_unit_id']) === slot.loadUnitId)
            .map(item => {
              const article = articleById.get(Number(item['article_id']));
              return {
                barcode: String(article?.['barcode'] ?? ''),
                description: String(article?.['description'] ?? ''),
                quantity: Number(item['quantity'])
              };
            })
        );
        this.loadingStock.set(false);
      },
      error: err => {
        console.error('cannot load the stock', err);
        if (this.selectedSlot() === slot) {
          this.stockError.set(true);
          this.loadingStock.set(false);
        }
      }
    });
  }

  closePopup(): void {
    this.selectedSlot.set(null);
  }
}