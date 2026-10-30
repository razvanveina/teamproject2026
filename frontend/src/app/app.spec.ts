import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    // The menu is shown only to a logged-in user (AuthService reads the token at start).
    sessionStorage.setItem('token', 'test-token');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([{ path: 'login', children: [] }]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the menu links', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const links = [...compiled.querySelectorAll('.main-menu a')].map((link) => link.textContent?.trim());
    expect(links).toEqual(['Home']);
  });

  it('should hide the menu after logout', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    compiled.querySelector<HTMLButtonElement>('.logout-button')!.click();
    await fixture.whenStable();

    expect(compiled.querySelector('.main-menu')).toBeNull();
  });


});
