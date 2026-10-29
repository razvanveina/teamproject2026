import { TestBed } from '@angular/core/testing';
import { UserTable } from './user-table';

describe('UserTable', () => {
  let service: UserTable;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserTable);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
