import { TestBed } from '@angular/core/testing';

import { InteraccionService } from './interaccion-service';

describe('InteraccionService', () => {
  let service: InteraccionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(InteraccionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
