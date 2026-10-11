import { useState, type FormEvent } from 'react';
import { Card, Box, Heading, Text, TextField, Button, Flex, Callout } from '@radix-ui/themes';
import { type User, type AuthResponse } from '../../types/auth';

interface AuthViewProps {
  onAuthSuccess: (user: User) => void;
}

export function AuthView({ onAuthSuccess }: AuthViewProps) {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const url = isRegister ? 'http://localhost:8080/api/users/register' : 'http://localhost:8080/api/users/login';
    const body = isRegister ? { username, email, password } : { username, password };

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Authentication failed');
      }

      const authData = data as AuthResponse;
      onAuthSuccess({
        username: authData.username,
        email: authData.email,
        token: authData.token,
      });
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box style={{ maxWidth: 420, margin: '60px auto' }}>
      <Card
        size="3"
        style={{
          border: '1px solid rgba(255, 255, 255, 0.12)',
          backgroundColor: '#1e293b',
          padding: '28px',
        }}
      >
        <Heading as="h2" size="6" align="center" mb="2">
          {isRegister ? 'Register' : 'Login'}
        </Heading>        

        {error && (
          <Box mb="4">
            <Callout.Root color="red" role="alert">
              <Callout.Text>{error}</Callout.Text>
            </Callout.Root>
          </Box>
        )}

        <form onSubmit={handleSubmit}>
          <Flex direction="column" gap="4">
            <Box>
              <label
                htmlFor="username"
                style={{ display: 'block', marginBottom: 6, fontSize: '0.9rem', color: '#cbd5e1' }}
              >
                Username
              </label>
              <TextField.Root
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                required
              />
            </Box>

            {isRegister && (
              <Box>
                <label
                  htmlFor="email"
                  style={{ display: 'block', marginBottom: 6, fontSize: '0.9rem', color: '#cbd5e1' }}
                >
                  Email
                </label>
                <TextField.Root
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter email address"
                  required
                />
              </Box>
            )}

            <Box>
              <label
                htmlFor="password"
                style={{ display: 'block', marginBottom: 6, fontSize: '0.9rem', color: '#cbd5e1' }}
              >
                Password
              </label>
              <TextField.Root
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                required
              />
            </Box>

            <Button size="3" type="submit" disabled={loading} style={{ cursor: 'pointer', marginTop: 8 }}>
              {loading ? 'Please wait...' : isRegister ? 'Register' : 'Login'}
            </Button>

            <Button
              type="button"
              variant="ghost"
              size="2"
              onClick={() => {
                setIsRegister(!isRegister);
                setError(null);
              }}
              style={{ cursor: 'pointer', color: '#94a3b8' }}
            >
              {isRegister
                ? 'Already have an account? Login'
                : 'Need an account? Register'}
            </Button>
          </Flex>
        </form>
      </Card>
    </Box>
  );
}