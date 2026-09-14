import './globals.css';
import { AuthProvider } from '@/contexts/AuthContext'; 

export const metadata = {
  title: 'Mueblería Edén',
  description: 'Venta de camas, colchones y accesorios de descanso en Santa Cruz de la Sierra.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}