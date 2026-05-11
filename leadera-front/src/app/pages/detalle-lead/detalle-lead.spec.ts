import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetalleLead } from './detalle-lead';

describe('DetalleLead', () => {
  let component: DetalleLead;
  let fixture: ComponentFixture<DetalleLead>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalleLead]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetalleLead);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
