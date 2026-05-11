import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LeadSection } from './lead-section';

describe('LeadSection', () => {
  let component: LeadSection;
  let fixture: ComponentFixture<LeadSection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeadSection]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LeadSection);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
