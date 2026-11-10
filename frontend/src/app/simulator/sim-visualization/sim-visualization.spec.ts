import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimVisualization } from './sim-visualization';

describe('SimVisualization', () => {
  let component: SimVisualization;
  let fixture: ComponentFixture<SimVisualization>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimVisualization],
    }).compileComponents();

    fixture = TestBed.createComponent(SimVisualization);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
