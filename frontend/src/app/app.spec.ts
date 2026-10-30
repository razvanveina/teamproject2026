import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    // The menu is shown only to a logged-in user (AuthService reads the token at start).
    sessionStorage.setItem('token', 'test-token');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
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
    expect(links).toEqual(['Home', 'Dialoguri']);
  });

  it('should hide the menu after logout', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    compiled.querySelector<HTMLButtonElement>('.logout-button')!.click();
    await fixture.whenStable();

    expect(compiled.querySelector('.main-menu')).toBeNull();
  });

  it('should switch between night mode and light mode from the menu', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const toggle = compiled.querySelector<HTMLButtonElement>('.theme-toggle')!;

    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(toggle.textContent).toContain('Mod luminos');

    toggle.click();
    await fixture.whenStable();

    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(toggle.textContent).toContain('Mod întunecat');
  });
});
