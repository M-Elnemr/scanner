import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "./schema";
import { ApiError, type ProblemDetail } from "./errors";

/**
 * `API_BASE_URL` is server-only (never `NEXT_PUBLIC_`): browser code never talks to the backend
 * directly, only through this app's own Route Handlers, which is what keeps the access token out of
 * client JS. In the docker-compose network the backend is reachable at `http://app:8080`; locally
 * it's `http://localhost:8080`.
 */
const BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export function authMiddleware(getToken: () => string | undefined | Promise<string | undefined>): Middleware {
  return {
    async onRequest({ request }) {
      const token = await getToken();
      if (token) {
        request.headers.set("Authorization", `Bearer ${token}`);
      }
      return request;
    },
  };
}

export function createApiClient(getToken?: () => string | undefined | Promise<string | undefined>) {
  const client = createClient<paths>({ baseUrl: BASE_URL });
  if (getToken) {
    client.use(authMiddleware(getToken));
  }
  return client;
}

/** A client with no auth — for the public GET routes (browse, trip detail, reference data). */
export const publicApi = createApiClient();

type Enveloped<T> = { data?: T };
type FetchResult<T> = { data?: Enveloped<T> | null; error?: unknown; response: Response };

/**
 * Every success body on this API is `{ data: T }`; every failure is an RFC 7807 problem, sometimes
 * with a `code` the caller should branch on (see lib/api/errors.ts). This turns an openapi-fetch
 * result into either the unwrapped payload or a thrown {@link ApiError} — the one place that
 * happens, so every call site can just `await unwrap(...)` and catch `ApiError`.
 */
export function unwrap<T>(result: FetchResult<T>): T {
  if (result.error) {
    throw new ApiError(result.error as ProblemDetail);
  }
  if (!result.response.ok) {
    throw new ApiError({ status: result.response.status, detail: result.response.statusText });
  }
  // Some endpoints legitimately return `{ data: null }` (e.g. no active lead) — that is not an error.
  return (result.data?.data ?? null) as T;
}

/** For 204/no-body endpoints (delete, logout) — just surfaces the error, if any. */
export function unwrapVoid(result: { error?: unknown; response: Response }): void {
  if (result.error) {
    throw new ApiError(result.error as ProblemDetail);
  }
  if (!result.response.ok) {
    throw new ApiError({ status: result.response.status, detail: result.response.statusText });
  }
}
