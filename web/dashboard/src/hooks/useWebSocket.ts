import { useEffect, useState, useCallback } from 'react';
import { JWT_TOKEN_KEY } from '@/src/lib/constants';

interface WebSocketMessage {
  type: string;
  data: any;
}

export const useWebSocket = () => {
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem(JWT_TOKEN_KEY);
    const wsUrl = `${process.env.NEXT_PUBLIC_WS_URL}?token=${token}`;
    
    const ws = new WebSocket(wsUrl);
    
    ws.onopen = () => {
      setIsConnected(true);
      setError(null);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        setMessages((prev) => [...prev, data].slice(-50)); // Keep last 50 messages
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e);
      }
    };

    ws.onerror = (event) => {
      setError('WebSocket connection error');
      console.error('WebSocket error:', event);
    };

    ws.onclose = () => {
      setIsConnected(false);
    };

    return () => {
      ws.close();
    };
  }, []);

  const sendMessage = useCallback((data: any) => {
    // In a real implementation, we would send data through the WebSocket
    // This is just a placeholder
    console.log('Would send message:', data);
  }, []);

  return { messages, isConnected, error, sendMessage };
};
