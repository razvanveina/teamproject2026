import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReceivingApp } from './receiving-app';

describe('ReceivingApp', () => {
  let component: ReceivingApp;
  let fixture: ComponentFixture<ReceivingApp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReceivingApp],
    }).compileComponents();

    fixture = TestBed.createComponent(ReceivingApp);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
