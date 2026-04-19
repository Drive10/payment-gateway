import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserTracing } from "@sentry/react";
import * as Sentry from "@sentry/react";
import App from "./App";
import "./index.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 1000 * 60 * 5,
      refetchOnWindowFocus: false,
    },
  },
});

Sentry.init({
  integrations: [
    BrowserTracing(),
  ],
  tracesSampleRate: import.meta.env.DEV ? 1.0 : 0.1,
  environment: import.meta.env.MODE,
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <Sentry.ErrorBoundary fallback={<ErrorFallback />}>
        <App />
      </Sentry.ErrorBoundary>
    </QueryClientProvider>
  </React.StrictMode>
);

function ErrorFallback() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <div className="rounded-[2rem] border border-red-500/60 bg-red-50/85 px-8 py-6 text-sm font-medium text-red-700 shadow-[0_20px_80px_rgba(15,23,42,0.14)] backdrop-blur">
        Something went wrong. Please refresh the page.
      </div>
    </main>
  );
}