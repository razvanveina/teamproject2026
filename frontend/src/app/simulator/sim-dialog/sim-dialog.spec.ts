import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimulatorDialog } from './sim-dialog';

describe('WorkerDialogList', () => {
  let component: SimulatorDialog;
  let fixture: ComponentFixture<SimulatorDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimulatorDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(SimulatorDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
