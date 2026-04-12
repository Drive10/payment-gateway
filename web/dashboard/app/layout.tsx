import './globals.css';
import { Inter } from 'next/font/google';
import { Providers } from '@/src/lib/providers';
import { useAuthGuard } from '@/src/lib/authGuard';

const inter = Inter({ subsets: ['latin'] });

export const metadata = {
  title: 'PayFlow Dashboard',
  description: 'Payment monitoring dashboard for PayFlow',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  useAuthGuard('/auth/login');
  
  return (
    <html lang="en" className={inter.className}>
      <body className="antialiased">{children}</body>
    </html>
  );
}
