import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserTableService } from './user-table';

describe('UserTableService', () => {
  let service: UserTableService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserTableService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load the users from the backend', () => {
    let count = 0;
    service.getUsers().subscribe((users) => (count = users.length));

    http.expectOne('http://localhost:8080/api/users').flush([{ id: 1, name: 'Ana', email: 'ana@example.com' }]);

    expect(count).toBe(1);
  });
});
