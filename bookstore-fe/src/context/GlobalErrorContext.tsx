import { useEffect, useState, type ReactNode } from 'react';
import { Box, Button, Callout, Flex } from '@radix-ui/themes';

interface GlobalError {
  message: string;
}

interface ApiErrorEvent extends CustomEvent<GlobalError> {
  detail: GlobalError;
}

export function GlobalErrorProvider({ children }: { children: ReactNode }) {
  const [error, setError] = useState<GlobalError | null>(null);

  useEffect(() => {
    const handleApiError = (event: Event) => {
      setError((event as ApiErrorEvent).detail);
    };

    window.addEventListener('bookstore:api-error', handleApiError);
    return () => {
      window.removeEventListener('bookstore:api-error', handleApiError);
    };
  }, []);

  return (
    <>
      {error && (
        <Box
          role="region"
          aria-label="Application notifications"
          style={{
            position: 'fixed',
            top: 16,
            left: 16,
            right: 16,
            zIndex: 1000,
            maxWidth: 720,
            margin: '0 auto',
          }}
        >
          <Callout.Root color="red" role="alert">
            <Flex align="center" justify="between" gap="3">
              <Callout.Text>{error.message}</Callout.Text>
              <Button
                size="1"
                variant="soft"
                color="red"
                aria-label="Dismiss error"
                onClick={() => setError(null)}
              >
                Dismiss
              </Button>
            </Flex>
          </Callout.Root>
        </Box>
      )}
      {children}
    </>
  );
}
