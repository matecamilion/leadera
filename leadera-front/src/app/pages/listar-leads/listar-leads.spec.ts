import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListarLeads } from './listar-leads';

describe('ListarLeads', () => {
  let component: ListarLeads;
  let fixture: ComponentFixture<ListarLeads>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListarLeads]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListarLeads);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
