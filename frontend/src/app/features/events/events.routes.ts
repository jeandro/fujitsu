import { Routes } from "@angular/router";
import { EventListPage } from "./event-list/event-list.page";
import { EventFormPage } from "./event-form/event-form.page";
import { EventDetailPage } from "./event-detail/event-detail.page";
import { SagaStatusPage } from "./saga-status/saga-status.page";

export const eventsRoutes: Routes = [
  {
    path: "",
    component: EventListPage,
  },
  {
    path: "novo",
    component: EventFormPage,
  },
  {
    path: ":eventId",
    component: EventDetailPage,
  },
  {
    path: "sagas/:sagaId",
    component: SagaStatusPage,
  },
];
