import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NuevaInteraccion } from './nueva-interaccion';

describe('NuevaInteraccion', () => {
  let component: NuevaInteraccion;
  let fixture: ComponentFixture<NuevaInteraccion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NuevaInteraccion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NuevaInteraccion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
