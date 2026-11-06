import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WorkerDialogList } from './worker-dialog-list';

describe('WorkerDialogList', () => {
  let component: WorkerDialogList;
  let fixture: ComponentFixture<WorkerDialogList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkerDialogList],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkerDialogList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
