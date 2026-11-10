import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimPicking } from './sim-picking';

describe('SimPicking', () => {
  let component: SimPicking;
  let fixture: ComponentFixture<SimPicking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimPicking],
    }).compileComponents();

    fixture = TestBed.createComponent(SimPicking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
