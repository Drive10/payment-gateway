import { toast } from 'sonner';

export function ToastProvider({ children }) {
  return (
    <>
      {children}
    </>
  );
}

export function useAppToast() {
  return toast;
}