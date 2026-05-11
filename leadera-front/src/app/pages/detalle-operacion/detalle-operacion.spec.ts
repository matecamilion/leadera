import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetalleOperacion } from './detalle-operacion';

describe('DetalleOperacion', () => {
  let component: DetalleOperacion;
  let fixture: ComponentFixture<DetalleOperacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetalleOperacion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetalleOperacion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
