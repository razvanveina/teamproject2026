import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DecantingApp } from './decanting-app';

describe('DecantingApp', () => {
  let component: DecantingApp;
  let fixture: ComponentFixture<DecantingApp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecantingApp],
    }).compileComponents();

    fixture = TestBed.createComponent(DecantingApp);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
