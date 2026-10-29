import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('starts in night mode when nothing was chosen and the system has no light preference', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
  });

  it('remembers the choice for the next visit', () => {
    TestBed.inject(ThemeService).toggle();
    expect(localStorage.getItem('theme')).toBe('light');

    TestBed.resetTestingModule();
    expect(TestBed.inject(ThemeService).theme()).toBe('light');
  });

  it('writes the theme on the <html> element', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.tick();
    expect(document.documentElement.dataset['theme']).toBe('dark');

    service.toggle();
    TestBed.tick();
    expect(document.documentElement.dataset['theme']).toBe('light');
  });
});
