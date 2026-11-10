import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimManagement } from './sim-management';

describe('SimManagement', () => {
  let component: SimManagement;
  let fixture: ComponentFixture<SimManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimManagement],
    }).compileComponents();

    fixture = TestBed.createComponent(SimManagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
