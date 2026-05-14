import { useState } from "react";

interface ApiKey {
  id: string;
  name: string;
  prefix: string;
  key: string;
  createdAt: string;
  lastUsed: string;
  status: string;
  scopes: string[];
}

const mockKeys: ApiKey[] = [
  { id: "key_001", name: "Production API Key", prefix: "pk_prod", key: "pk_prod_example_key_001_XXXXXXXXXXXX", createdAt: "2026-01-15", lastUsed: "2026-05-14", status: "active", scopes: ["payments:read", "payments:write", "refunds:write"] },
  { id: "key_002", name: "Test API Key", prefix: "pk_test", key: "pk_test_example_key_002_YYYYYYYYYYYY", createdAt: "2026-03-20", lastUsed: "2026-05-13", status: "active", scopes: ["payments:read", "payments:write"] },
  { id: "key_003", name: "Mobile App Key", prefix: "pk_prod", key: "pk_prod_example_key_003_ZZZZZZZZZZZZ", createdAt: "2026-04-01", lastUsed: "2026-05-10", status: "active", scopes: ["payments:read"] },
  { id: "key_004", name: "Old Integration", prefix: "pk_prod", key: "pk_prod_example_key_004_WWWWWWWWWWWW", createdAt: "2025-11-01", lastUsed: "2026-04-28", status: "revoked", scopes: ["payments:read", "payments:write", "refunds:write"] },
];

export default function ApiKeys() {
  const [showKey, setShowKey] = useState<string | null>(null);

  const toggleKey = (id: string) => setShowKey(showKey === id ? null : id);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">API Keys</h1>
          <p className="mt-1 text-sm text-slate-500">Manage your API keys for integrations</p>
        </div>
        <button className="rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg hover:shadow-xl transition">
          + Generate Key
        </button>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="divide-y divide-slate-100">
          {mockKeys.map((key) => (
            <div key={key.id} className="p-6 hover:bg-slate-50">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold text-slate-900">{key.name}</h3>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${key.status === "active" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                      {key.status}
                    </span>
                    <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-mono text-slate-600">{key.prefix}***</span>
                  </div>
                  <div className="mt-2 flex items-center gap-4 text-xs text-slate-500">
                    <span>Created: {key.createdAt}</span>
                    <span>Last used: {key.lastUsed}</span>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {key.scopes.map((scope) => (
                      <span key={scope} className="rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">{scope}</span>
                    ))}
                  </div>
                  <div className="mt-3">
                    {showKey === key.id ? (
                      <div className="flex items-center gap-2">
                        <code className="rounded-lg bg-slate-100 px-3 py-1.5 font-mono text-xs text-slate-700 break-all">{key.key}</code>
                        <button onClick={() => navigator.clipboard.writeText(key.key)} className="text-indigo-600 hover:text-indigo-800">
                          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
                        </button>
                      </div>
                    ) : (
                      <button onClick={() => toggleKey(key.id)} className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                        Show key
                      </button>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 ml-4">
                  {key.status === "active" && (
                    <button className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">Revoke</button>
                  )}
                  <button className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50">Edit</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-xl border border-blue-200 bg-blue-50 p-4">
        <div className="flex items-start gap-3">
          <svg className="mt-0.5 h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <p className="text-sm font-medium text-blue-800">Security Best Practices</p>
            <ul className="mt-1 text-xs text-blue-700 list-disc list-inside space-y-1">
              <li>Never share API keys in client-side code or public repositories</li>
              <li>Rotate keys regularly and revoke unused keys immediately</li>
              <li>Use separate keys for production and test environments</li>
              <li>Assign minimal scopes needed for each integration</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
