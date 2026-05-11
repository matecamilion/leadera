import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionDelDia } from './gestion-del-dia';

describe('GestionDelDia', () => {
  let component: GestionDelDia;
  let fixture: ComponentFixture<GestionDelDia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionDelDia]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionDelDia);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
