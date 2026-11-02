import { TestBed } from '@angular/core/testing';
import { ReceivingOrderService } from './receiving-order-service';

describe('ReceivingOrder', () => {
  let service: ReceivingOrderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReceivingOrderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
