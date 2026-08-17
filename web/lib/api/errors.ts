/**
 * The backend's error shape (GlobalExceptionHandler): RFC 7807 problem+json, plus two backend-
 * specific extensions — `code` (a stable machine-readable discriminator, e.g. "ACTIVE_LEAD_EXISTS")
 * and `fieldErrors` (bean-validation 400s). Never formally documented in the OpenAPI spec (springdoc
 * doesn't emit error schemas from a generic @ExceptionHandler), so this is hand-maintained against
 * docs/flutter-integration-brief.md §2 and docs/admin-dashboard-brief.md §1.
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: string;
  fieldErrors?: Record<string, string>;
  /** Present only on 409 ACTIVE_LEAD_EXISTS. */
  activeLead?: {
    leadId: string;
    tripId: string;
    tripTitle: string;
    status: string;
  };
  [key: string]: unknown;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly fieldErrors?: Record<string, string>;
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`);
    this.name = "ApiError";
    this.status = problem.status;
    this.code = problem.code;
    this.fieldErrors = problem.fieldErrors;
    this.problem = problem;
  }

  is(code: string): boolean {
    return this.code === code;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
