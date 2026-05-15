import { toast, Toaster } from 'sonner';

export function ToastProvider({ children }) {
  return (
    <>
      {children}
      <Toaster position="top-right" richColors />
    </>
  );
}

export function useAppToast() {
  return toast;
}