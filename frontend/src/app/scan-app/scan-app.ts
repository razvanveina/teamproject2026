import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface ScanLine {
    article: string;
    quantity: number;
}

@Component({
    imports: [CommonModule, FormsModule],
    selector: 'app-scan-app',
    styleUrl: './scan-app.css',
    templateUrl: './scan-app.html',
})
export class ScanApp {
    loadUnit = '';
    article = '';
    quantity = 0;

    lines: ScanLine[] = [];

    step = 1;

    ngOnInit(): void {
        const saved = localStorage.getItem('scanData');

        if (saved) {
            const data = JSON.parse(saved);

            this.loadUnit = data.loadUnit ?? '';
            this.lines = data.lines ?? [];

            if (this.loadUnit) {
                this.step = 2;
        }
        }
    }

    get currentInstruction(): string {

    switch (this.step) {
        case 1:
            return 'Scan Load Unit barcode';

        case 2:
            return 'Scan Article barcode';

        case 3:
            return 'Enter quantity';

        default:
            return '';
    }
  }

  onScan(value: string): void {

    switch (this.step) {
        case 1:
            // Validate Load Unit barcode
            this.loadUnit = value;
            this.step = 2;
            break;

        case 2:
            // Validate Article barcode
            this.article = value;
            this.step = 3;
            break;

        case 3:
            const quantity = Number(value);

            this.lines.push({
                article: this.article,
                quantity: quantity
            });

            this.article = '';
            this.quantity = 0;

            this.step = 2;

            break;
    }
  }

  onCancel() {
    history.back();
  }

  onFinish(): void {
    const data = {
        loadUnit: this.loadUnit,
        lines: this.lines
    };

    localStorage.setItem('scanData', JSON.stringify(data));
  }
  
  get currentPlaceholder(): string {
    return this.currentInstruction;
  }
}