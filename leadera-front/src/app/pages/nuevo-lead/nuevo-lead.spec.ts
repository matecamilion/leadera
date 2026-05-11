import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NuevoLead } from './nuevo-lead';

describe('NuevoLead', () => {
  let component: NuevoLead;
  let fixture: ComponentFixture<NuevoLead>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NuevoLead]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NuevoLead);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
