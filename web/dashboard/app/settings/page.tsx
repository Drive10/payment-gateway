'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import axios from 'axios';
import { API_BASE_URL, API_GATEWAY_URL } from '@/src/lib/constants';
import { getAuthToken } from '@/src/lib/auth';
import { useToast } from '@/src/lib/toast';

interface User {
  userId: string;
  email: string;
  role: string;
}

export default function SettingsPage() {
  const router = useRouter();
  const toast = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('profile');

  const [profile, setProfile] = useState({
    name: 'Demo User',
    email: 'demo@payflow.com',
    company: 'Demo Company',
    timezone: 'UTC',
  });

  const [notifications, setNotifications] = useState({
    emailAlerts: true,
    paymentSuccess: true,
    paymentFailed: true,
    weeklyReport: false,
  });

  const [apiKeys, setApiKeys] = useState({
    publicKey: 'pk_live_####################',
    secretKey: 'sk_live_####################',
  });

  const handleSaveProfile = async () => {
    setIsLoading(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      toast.success('Profile updated successfully');
    } catch {
      toast.error('Failed to update profile');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveNotifications = async () => {
    setIsLoading(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      toast.success('Notification preferences saved');
    } catch {
      toast.error('Failed to save preferences');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegenerateKeys = async () => {
    if (!confirm('Are you sure? This will invalidate your current API keys.')) return;
    
    setIsLoading(true);
    try {
      const newSecretKey = 'sk_live_' + Math.random().toString(36).substring(2, 15);
      setApiKeys((prev) => ({ ...prev, secretKey: newSecretKey }));
      toast.success('API keys regenerated');
    } catch {
      toast.error('Failed to regenerate keys');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Settings</h1>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {['profile', 'notifications', 'api'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab === 'profile'
                ? 'Profile'
                : tab === 'notifications'
                ? 'Notifications'
                : 'API Keys'}
            </button>
          ))}
        </nav>
      </div>

      {/* Profile Tab */}
      {activeTab === 'profile' && (
        <div className="bg-white shadow rounded-lg p-6">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSaveProfile();
            }}
            className="space-y-6"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Full Name
                </label>
                <input
                  type="text"
                  value={profile.name}
                  onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  type="email"
                  value={profile.email}
                  onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Company Name
                </label>
                <input
                  type="text"
                  value={profile.company}
                  onChange={(e) => setProfile({ ...profile, company: e.target.value })}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Timezone
                </label>
                <select
                  value={profile.timezone}
                  onChange={(e) => setProfile({ ...profile, timezone: e.target.value })}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="UTC">UTC</option>
                  <option value="EST">EST (UTC-5)</option>
                  <option value="PST">PST (UTC-8)</option>
                  <option value="IST">IST (UTC+5:30)</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isLoading}
                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
              >
                {isLoading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Notifications Tab */}
      {activeTab === 'notifications' && (
        <div className="bg-white shadow rounded-lg p-6">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSaveNotifications();
            }}
            className="space-y-6"
          >
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-gray-900">Email Alerts</p>
                  <p className="text-sm text-gray-500">Receive email for important events</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifications.emailAlerts}
                  onChange={(e) =>
                    setNotifications({ ...notifications, emailAlerts: e.target.checked })
                  }
                  className="h-4 w-4 text-indigo-600"
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-gray-900">Payment Success</p>
                  <p className="text-sm text-gray-500">Notify when payments succeed</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifications.paymentSuccess}
                  onChange={(e) =>
                    setNotifications({ ...notifications, paymentSuccess: e.target.checked })
                  }
                  className="h-4 w-4 text-indigo-600"
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-gray-900">Payment Failed</p>
                  <p className="text-sm text-gray-500">Notify when payments fail</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifications.paymentFailed}
                  onChange={(e) =>
                    setNotifications({ ...notifications, paymentFailed: e.target.checked })
                  }
                  className="h-4 w-4 text-indigo-600"
                />
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-gray-900">Weekly Report</p>
                  <p className="text-sm text-gray-500">Receive weekly summary</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifications.weeklyReport}
                  onChange={(e) =>
                    setNotifications({ ...notifications, weeklyReport: e.target.checked })
                  }
                  className="h-4 w-4 text-indigo-600"
                />
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isLoading}
                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:opacity-50"
              >
                {isLoading ? 'Saving...' : 'Save Preferences'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* API Keys Tab */}
      {activeTab === 'api' && (
        <div className="bg-white shadow rounded-lg p-6 space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Public Key
            </label>
            <input
              type="text"
              value={apiKeys.publicKey}
              readOnly
              className="block w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-500"
            />
            <p className="text-xs text-gray-500 mt-1">Use this in your frontend code</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Secret Key
            </label>
            <input
              type="password"
              value={apiKeys.secretKey}
              readOnly
              className="block w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-500"
            />
            <p className="text-xs text-gray-500 mt-1">Keep this secret. Never expose in client code</p>
          </div>

          <div className="flex justify-between pt-4">
            <button
              onClick={() => navigator.clipboard.writeText(apiKeys.publicKey)}
              className="px-4 py-2 text-indigo-600 hover:text-indigo-700"
            >
              Copy Public Key
            </button>
            <button
              onClick={handleRegenerateKeys}
              disabled={isLoading}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50"
            >
              Regenerate Keys
            </button>
          </div>
        </div>
      )}
    </div>
  );
}