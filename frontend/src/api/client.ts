const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("token");
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers ?? {}),
  };

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new ApiError(response.status, text || response.statusText);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PUT", body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
  // No Content-Type set here (unlike request()'s default) — the browser fills in the
  // multipart boundary itself, which it can only do if the caller doesn't set one.
  upload: <T>(path: string, formData: FormData) => {
    const token = localStorage.getItem("token");
    return fetch(`${BASE_URL}${path}`, {
      method: "POST",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    }).then(async (response) => {
      if (!response.ok) {
        const text = await response.text().catch(() => "");
        throw new ApiError(response.status, text || response.statusText);
      }
      return (await response.json()) as T;
    });
  },
};

// Browsers' native EventSource can't send an Authorization header, and /v1/kitchen/**
// requires a JWT — so this reads the SSE stream manually via fetch() instead, which
// does support custom headers.
export async function streamServerSentEvents<T>(
  path: string,
  onEvent: (data: T) => void,
  signal: AbortSignal,
): Promise<void> {
  const token = localStorage.getItem("token");
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    signal,
  });

  if (!response.ok || !response.body) {
    throw new ApiError(response.status, "Could not open the event stream");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    const events = buffer.split("\n\n");
    buffer = events.pop() ?? "";

    for (const rawEvent of events) {
      const dataLines = rawEvent.split("\n").filter((line) => line.startsWith("data:"));
      if (dataLines.length === 0) continue;
      const json = dataLines.map((line) => line.slice(5).trim()).join("");
      try {
        onEvent(JSON.parse(json) as T);
      } catch {
        // ignore a malformed chunk rather than killing the whole stream
      }
    }
  }
}

export { BASE_URL };
