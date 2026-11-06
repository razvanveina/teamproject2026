import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TransportOrderApi } from '../services/transport-order';
import { LogService } from '../services/log';
import { ToteTransport } from './tote-transport';

describe('TransportApp', () => {
  let component: ToteTransport;
  let fixture: ComponentFixture<ToteTransport>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToteTransport],
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

    fixture = TestBed.createComponent(ToteTransport);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
