import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetallePropiedad } from './detalle-propiedad';

describe('DetallePropiedad', () => {
  let component: DetallePropiedad;
  let fixture: ComponentFixture<DetallePropiedad>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetallePropiedad]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetallePropiedad);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
