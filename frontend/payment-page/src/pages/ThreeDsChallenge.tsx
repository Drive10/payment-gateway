import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

const API_BASE_URL = window.__ENV__?.API_BASE_URL || 'http://localhost:8080';
const API_ROOT = API_BASE_URL.endsWith('/api/v1') ? API_BASE_URL : `${API_BASE_URL}/api/v1`;

export default function ThreeDsChallenge() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState('');

  const transactionId = searchParams.get('transactionId');
  const challengeUrl = searchParams.get('challengeUrl');

  useEffect(() => {
    if (!transactionId || !challengeUrl) {
      setError('Missing required parameters');
      setStatus('error');
      return;
    }

    const simulateChallenge = async () => {
      setTimeout(() => {
        setStatus('challenge');
      }, 1000);
    };

    simulateChallenge();
  }, [transactionId, challengeUrl]);

  const handleVerify = async (authenticationStatus) => {
    setStatus('verifying');

    try {
      const response = await fetch(
        `${API_ROOT}/payments/${transactionId}/verify-3ds`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ authenticationStatus })
        }
      );

      const result = await response.json();

      if (result.success) {
        navigate(`/processing?paymentId=${transactionId}&status=3ds_verified`);
      } else {
        setError(result.error?.message || 'Authentication failed');
        setStatus('error');
      }
    } catch (err) {
      setError('Failed to verify 3DS');
      setStatus('error');
    }
  };

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading 3D Secure...</p>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full">
          <div className="text-center">
            <div className="text-red-500 text-5xl mb-4">⚠️</div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">Authentication Failed</h2>
            <p className="text-gray-600 mb-6">{error || 'An error occurred'}</p>
            <button
              onClick={() => navigate('/')}
              className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition-colors"
            >
              Back to Payment
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full">
        <div className="text-center mb-6">
          <div className="text-4xl mb-4">🔒</div>
          <h2 className="text-xl font-bold text-gray-900">3D Secure Verification</h2>
          <p className="text-gray-600 text-sm mt-2">
            Your bank requires verification for this transaction
          </p>
        </div>

        <div className="space-y-4">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-700">
              This is a simulated 3DS challenge page. In production, your bank would display their own verification UI.
            </p>
          </div>

          <div className="border-t border-gray-200 pt-4">
            <p className="text-sm text-gray-600 mb-4">
              For testing, use these OTPs:
            </p>
            <ul className="text-sm text-gray-500 space-y-1">
              <li><strong>123456</strong> - Verify successfully</li>
              <li><strong>000000</strong> - Verify failed</li>
            </ul>
          </div>

          <div className="flex gap-3 mt-6">
            <button
              onClick={() => handleVerify('SUCCESS')}
              className="flex-1 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
            >
              Verify Success
            </button>
            <button
              onClick={() => handleVerify('FAILED')}
              className="flex-1 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
            >
              Verify Failed
            </button>
          </div>

          <button
            onClick={() => navigate('/')}
            className="w-full mt-3 text-gray-500 text-sm hover:text-gray-700"
          >
            Cancel and Back
          </button>
        </div>
      </div>
    </div>
  );
}
