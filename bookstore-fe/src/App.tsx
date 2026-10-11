import {
  Container,
  Heading,
  Box,
  Flex,
  Button,
  Text,
} from '@radix-ui/themes';
import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { GlobalErrorProvider } from './context/GlobalErrorContext';
import { useCart } from './hooks/useCart';
import { CartLauncher } from './components/Cart/CartLauncher';
import { AuthView } from './components/Auth/AuthView';
import { BookList } from './components/BookList/BookList';
import type { Book } from './types/bookstore';

function BookstoreApp() {
  const { user, isAuthenticated, login, logout } = useAuth();

  if (!isAuthenticated) {
    return (
      <Container size="2">
        <AuthView onAuthSuccess={login} />
      </Container>
    );
  }

  return (
    <CartProvider>
      <AuthenticatedBookstore username={user?.username ?? ''} onLogout={logout} />
    </CartProvider>
  );
}

function AuthenticatedBookstore({
  username,
  onLogout,
}: {
  username: string;
  onLogout: () => void;
}) {
  const { addToCart } = useCart();

  const handleAddToCart = (book: Book) => {
    void addToCart(book);
  };

  return (
    <Container size="3" p="4" style={{ maxWidth: '960px', margin: '0 auto' }}>
      <Flex
        justify="between"
        align="center"
        py="4"
        mb="5"
        style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.1)' }}
      >
        <Box>
          <Heading as="h1" size="7" weight="bold">
            Online Bookstore
          </Heading>
          <Text size="2" style={{ color: '#94a3b8' }}>
            Logged in as <strong>{username}</strong>
          </Text>
        </Box>

        <Flex align="center" gap="3">
          <CartLauncher />
          <Button variant="outline" color="gray" size="2" onClick={onLogout} style={{ cursor: 'pointer' }}>
            Logout
          </Button>
        </Flex>
      </Flex>

      <Box asChild>
        <main>
          <BookList onAddToCart={handleAddToCart} />
        </main>
      </Box>
    </Container>
  );
}

export default function App() {
  return (
    <GlobalErrorProvider>
      <AuthProvider>
        <BookstoreApp />
      </AuthProvider>
    </GlobalErrorProvider>
  );
}