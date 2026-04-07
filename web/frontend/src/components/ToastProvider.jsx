import { useState } from 'react';
import { toast } from 'sonner';

export function ToastProvider({ children }) {
  return (
    <>
      {children}
      {/* Toaster is automatically added by sonner when using toast() */}
    </>
  );
}

// Custom hook for easy toast usage
export function useAppToast() {
  return toast;
}