import './globals.css';
import { Inter } from 'next/font/google';
import { Providers } from '@/src/lib/providers';

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
  return (
    <html lang="en" className={inter.className}>
      <body className="antialiased">
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  );
}