import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { isAuthenticated } from './auth';

export const useAuthGuard = (redirectTo = '/auth/login') => {
  const router = useRouter();
  
  useEffect(() => {
    if (!isAuthenticated()) {
      router.push(redirectTo);
    }
  }, [isAuthenticated(), router]);
};
