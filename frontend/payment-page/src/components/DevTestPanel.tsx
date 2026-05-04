export interface DevTestPanelProps {
  onFillCard?: () => void;
  onSelectUpi?: () => void;
  onSimulateSuccess?: () => void;
  onSimulateFailure?: () => void;
  onSimulateOtp?: () => void;
  onClearStorage?: () => void;
  isVisible?: boolean;
}

export function DevTestPanel({
  onFillCard,
  onSelectUpi,
  onSimulateSuccess,
  onSimulateFailure,
  onSimulateOtp,
  onClearStorage,
  isVisible = true,
}: DevTestPanelProps) {
  if (!isVisible) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 rounded-xl border border-indigo-200 bg-indigo-50/95 p-3 shadow-xl backdrop-blur-sm">
      <p className="text-xs font-bold text-indigo-700">🛠 Dev Test Panel</p>
      <div className="grid grid-cols-2 gap-2">
        <button
          onClick={onFillCard}
          className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-700"
        >
          💳 Fill Dummy Card
        </button>
        <button
          onClick={onSelectUpi}
          className="rounded-lg bg-cyan-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-cyan-700"
        >
          📱 Select UPI
        </button>
        <button
          onClick={onSimulateSuccess}
          className="rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
        >
          ✅ Simulate Success
        </button>
        <button
          onClick={onSimulateFailure}
          className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700"
        >
          ❌ Simulate Failure
        </button>
        <button
          onClick={onSimulateOtp}
          className="col-span-2 rounded-lg bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700"
        >
          🔐 Simulate OTP Verify
        </button>
        <button
          onClick={onClearStorage}
          className="col-span-2 rounded-lg bg-slate-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700"
        >
          🗑️ Clear Storage
        </button>
      </div>
    </div>
  );
}