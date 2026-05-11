import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PropiedadDetalle } from './propiedad-detalle';

describe('PropiedadDetalle', () => {
  let component: PropiedadDetalle;
  let fixture: ComponentFixture<PropiedadDetalle>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PropiedadDetalle]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PropiedadDetalle);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
