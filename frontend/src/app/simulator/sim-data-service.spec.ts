import { TestBed } from '@angular/core/testing';
import { SimDataService } from './sim-data-service';

describe('SimDataService', () => {
  let service: SimDataService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SimDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
