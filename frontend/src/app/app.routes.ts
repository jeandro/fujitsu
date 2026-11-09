import { Routes } from "@angular/router";

export const appRoutes: Routes = [
  {
    path: "events",
    loadChildren: () =>
      import("./features/events/events.routes").then((m) => m.eventsRoutes),
  },
  {
    path: "",
    pathMatch: "full",
    redirectTo: "events",
  },
  {
    path: "**",
    redirectTo: "events",
  },
];
