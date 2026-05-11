import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LeadCard } from './lead-card';

describe('LeadCard', () => {
  let component: LeadCard;
  let fixture: ComponentFixture<LeadCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeadCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LeadCard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
