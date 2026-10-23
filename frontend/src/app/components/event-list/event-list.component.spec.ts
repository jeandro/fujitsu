import { TestBed } from "@angular/core/testing";
import { of, throwError, Subject } from "rxjs";
import { HttpErrorResponse } from "@angular/common/http";
import { EventListComponent } from "./event-list.component";
import { EventService } from "../../services/event.service";
import { FactoryEvent } from "../../models/event.model";
describe("EventListComponent", () => {
  let service: jasmine.SpyObj<EventService>;
  beforeEach(() => {
    service = jasmine.createSpyObj("EventService", ["getEvents"], {
      eventCreated$: new Subject<void>(),
    });
    TestBed.configureTestingModule({
      imports: [EventListComponent],
      providers: [{ provide: EventService, useValue: service }],
    });
  });
  it("não confunde indisponibilidade com lista vazia e permite tentar novamente", () => {
    service.getEvents.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );
    const f = TestBed.createComponent(EventListComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Não foi possível conectar");
    expect(f.nativeElement.textContent).not.toContain("Nenhum evento");
    service.getEvents.and.returnValue(of([]));
    f.componentInstance.loadEvents();
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Nenhum evento");
  });
  it("agenda atualização quando cadastro ocorre durante consulta", () => {
    const pending = new Subject<FactoryEvent[]>();
    service.getEvents.and.returnValues(pending, of([]));
    const f = TestBed.createComponent(EventListComponent);
    f.detectChanges();
    f.componentInstance.loadEvents();
    pending.next([]);
    pending.complete();
    expect(service.getEvents).toHaveBeenCalledTimes(2);
    expect(f.componentInstance.loading).toBeFalse();
  });
});
