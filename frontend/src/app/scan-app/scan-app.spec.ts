import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ScanApp } from './scan-app';

describe('ScanApp', () => {
  let component: ScanApp;
  let fixture: ComponentFixture<ScanApp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScanApp],
    }).compileComponents();

    fixture = TestBed.createComponent(ScanApp);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
