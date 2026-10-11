export class ApiError extends Error {
  readonly status: number | null;

  constructor(message: string, status: number | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

interface ApiErrorEventDetail {
  message: string;
  status: number | null;
}

function dispatchApiError(error: ApiError): void {
  window.dispatchEvent(
    new CustomEvent<ApiErrorEventDetail>('bookstore:api-error', {
      detail: { message: error.message, status: error.status },
    }),
  );
}

async function getErrorMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json();
    if (typeof body === 'object' && body !== null) {
      const fields = body as Record<string, unknown>;
      for (const key of ['message', 'error', 'detail', 'title']) {
        const value = fields[key];
        if (typeof value === 'string' && value.trim()) {
          return value;
        }
      }
    }
  } catch {
    // Proxies and gateways may return plain text or empty error responses.
  }
  return `The request failed (HTTP ${response.status}).`;
}

export async function apiFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(input, {
      ...init,
      credentials: init.credentials ?? 'include',
    });
  } catch {
    const error = new ApiError(
      'Unable to reach the bookstore. Check your connection and try again.',
      null,
    );
    dispatchApiError(error);
    throw error;
  }

  if (response.ok) return response;

  const requestUrl =
    input instanceof Request
      ? input.url
      : input instanceof URL
        ? input.pathname
        : input;
  const isLoginRequest =
    typeof requestUrl === 'string' && requestUrl.includes('/api/users/login');
  const message =
    response.status === 401 && !isLoginRequest
      ? 'Your session has expired. Please sign in again.'
      : await getErrorMessage(response);
  const error = new ApiError(message, response.status);
  dispatchApiError(error);

  if (response.status === 401 && !isLoginRequest) {
    window.dispatchEvent(new Event('bookstore:unauthorized'));
  }

  throw error;
}
