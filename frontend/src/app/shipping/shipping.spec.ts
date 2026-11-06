import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShippingOut } from './shipping-out';

describe('ShippingOut', () => {
  let component: ShippingOut;
  let fixture: ComponentFixture<ShippingOut>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShippingOut],
    }).compileComponents();

    fixture = TestBed.createComponent(ShippingOut);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
