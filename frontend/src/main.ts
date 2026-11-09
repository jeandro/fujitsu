import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient, withFetch, withInterceptors } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { appRoutes } from "./app/app.routes";
import { correlationInterceptor } from "./app/core/interceptors/correlation.interceptor";
import { authInterceptor } from "./app/core/interceptors/auth.interceptor";
import { errorInterceptor } from "./app/core/interceptors/error.interceptor";
import { AppComponent } from "./app/app.component";

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(appRoutes),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationInterceptor,
        authInterceptor,
        errorInterceptor,
      ]),
    ),
  ],
}).catch((err) => console.error(err));
