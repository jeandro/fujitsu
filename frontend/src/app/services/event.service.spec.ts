import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { EventService } from "./event.service";
import { EventType } from "../models/event.model";
describe("EventService", () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it("envia POST e consulta GET pela URL relativa", () => {
    const service = TestBed.inject(EventService),
      http = TestBed.inject(HttpTestingController);
    const event = {
      eventId: "X",
      eventType: EventType.PRODUCTION,
      description: "d",
      timestamp: "2026-05-20T13:00:00Z",
    };
    service.registerEvent(event).subscribe();
    const post = http.expectOne("/api/events");
    expect(post.request.method).toBe("POST");
    expect(post.request.body).toEqual(event);
    post.flush(event);
    service.getEvents().subscribe();
    const get = http.expectOne("/api/events");
    expect(get.request.method).toBe("GET");
    get.flush([event]);
  });
});
