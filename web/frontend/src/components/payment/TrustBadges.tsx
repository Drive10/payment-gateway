export function TrustBadges() {
  return (
    <div className="flex flex-wrap items-center justify-center gap-4 py-4">
      <div className="flex items-center gap-1 text-xs text-gray-500">
        <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
        </svg>
        <span>Secured by TLS</span>
      </div>
      
      <div className="flex items-center gap-1 text-xs text-gray-500">
        <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
        </svg>
        <span>PCI DSS Compliant</span>
      </div>
      
      <div className="flex items-center gap-1 text-xs text-gray-500">
        <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
        </svg>
        <span>Your payment is safe</span>
      </div>
      
      <div className="flex items-center gap-2 ml-4">
        <span className="text-xs text-gray-400">We accept:</span>
        <div className="flex gap-1">
          <div className="w-8 h-5 bg-gray-200 rounded text-xs flex items-center justify-center font-bold text-gray-600">VISA</div>
          <div className="w-8 h-5 bg-gray-200 rounded text-xs flex items-center justify-center font-bold text-gray-600">MC</div>
          <div className="w-8 h-5 bg-gray-200 rounded text-xs flex items-center justify-center font-bold text-gray-600">Amex</div>
          <div className="w-8 h-5 bg-gray-200 rounded text-xs flex items-center justify-center font-bold text-gray-600">UPI</div>
        </div>
      </div>
    </div>
  );
}
