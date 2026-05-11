import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionarBusqueda } from './gestionar-busqueda';

describe('GestionarBusqueda', () => {
  let component: GestionarBusqueda;
  let fixture: ComponentFixture<GestionarBusqueda>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionarBusqueda]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionarBusqueda);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
