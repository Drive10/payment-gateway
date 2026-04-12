import { ReactNode, PropsWithChildren } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider as SonnerToastProvider } from 'sonner';

const queryClient = new QueryClient();

interface ProvidersProps extends PropsWithChildren {
  className?: string;
}

export const Providers = ({ children, className }: ProvidersProps) => {
  return (
    <QueryClientProvider client={queryClient}>
      <SonnerToastProvider>
        <div className={className}>{children}</div>
      </SonnerToastProvider>
    </QueryClientProvider>
  );
};
