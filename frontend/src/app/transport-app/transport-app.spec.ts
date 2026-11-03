import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TransportOrderApi } from '../services/transport-order';
import { LogService } from '../services/log';
import { TransportApp } from './transport-app';

describe('TransportApp', () => {
  let component: TransportApp;
  let fixture: ComponentFixture<TransportApp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransportApp],
      providers: [
        {
          provide: TransportOrderApi,
          useValue: {
            findNextOrder: () => of(null),
            scan: () => of({
              id: 1,
              status: 'ACTIVE',
              palletBarcode: '10000000',
              sourceLocation: 'SOURCE',
              destinationLocation: 'DESTINATION',
              loadUnitLocation: 'SOURCE',
            }),
          },
        },
        { provide: LogService, useValue: { action: () => {} } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransportApp);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
