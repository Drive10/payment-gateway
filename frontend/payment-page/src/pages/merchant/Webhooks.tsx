import { useState, useEffect } from "react";
import { merchantApi } from "../../lib/merchant";

interface DlqEvent {
  id?: string;
  eventId?: string;
  eventType?: string;
  aggregateType?: string;
  payload?: string;
  createdAt?: string;
  errorMessage?: string;
}

export default function Webhooks() {
  const [dlq, setDlq] = useState<DlqEvent[]>([]);
  const [status, setStatus] = useState<{ pending?: number; dlq?: number; processed?: number } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      merchantApi.events.dlq().catch(() => null),
      merchantApi.events.status().catch(() => null),
    ]).then(([dlqResult, statusResult]) => {
      if (dlqResult.status === "fulfilled" && dlqResult.value) {
        setDlq(Array.isArray(dlqResult.value) ? dlqResult.value : []);
      }
      if (statusResult.status === "fulfilled" && statusResult.value) {
        setStatus(statusResult.value);
      }
      setLoading(false);
    });
  }, []);

  const handleReplay = async (eventId: string) => {
    try {
      await merchantApi.events.replay(eventId);
      setDlq((prev) => prev.filter((e) => (e.id || e.eventId) !== eventId));
    } catch {
      // ignore
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Webhooks & Events</h1>
        <p className="mt-1 text-sm text-slate-500">Monitor event delivery and manage dead-lettered events</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-sm text-slate-500">Pending</p>
          <p className="mt-2 text-3xl font-bold text-slate-900">{status?.pending ?? 0}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-sm text-slate-500">Dead Letter Queue</p>
          <p className="mt-2 text-3xl font-bold text-red-600">{status?.dlq ?? dlq.length}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-sm text-slate-500">Processed</p>
          <p className="mt-2 text-3xl font-bold text-green-600">{status?.processed ?? 0}</p>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Dead Letter Queue</h2>
        </div>
        {loading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
          </div>
        ) : dlq.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <svg className="mx-auto h-12 w-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="mt-4 text-sm text-slate-500">No dead-lettered events</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {dlq.map((event) => (
              <div key={event.id || event.eventId} className="flex items-center justify-between px-6 py-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-700">DLQ</span>
                    <span className="font-mono text-xs text-slate-700">{event.eventType || event.aggregateType}</span>
                  </div>
                  {event.errorMessage && (
                    <p className="mt-1 text-xs text-slate-500 truncate max-w-lg">{event.errorMessage}</p>
                  )}
                  {event.createdAt && (
                    <p className="mt-0.5 text-xs text-slate-400">{new Date(event.createdAt).toLocaleString("en-IN")}</p>
                  )}
                </div>
                <button
                  onClick={() => handleReplay(event.id || event.eventId || "")}
                  className="rounded-lg bg-indigo-50 px-3 py-1.5 text-xs font-medium text-indigo-700 hover:bg-indigo-100"
                >
                  Replay
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900 mb-4">Webhook Endpoints</h2>
        <div className="rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center">
          <svg className="mx-auto h-10 w-10 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p className="mt-3 text-sm font-medium text-slate-700">No webhook endpoints configured</p>
          <p className="mt-1 text-xs text-slate-500">Configure webhook URLs to receive real-time payment events</p>
          <button className="mt-4 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">
            + Add Endpoint
          </button>
        </div>
      </div>
    </div>
  );
}
