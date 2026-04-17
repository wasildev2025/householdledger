import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import AppNavigator from './src/navigation/AppNavigator';
import { useAuthStore } from './src/features/auth/store';
import { useMessagesStore } from './src/features/messaging/store';
import { useRequestsStore } from './src/features/requests/store';
import { useTransactionStore } from './src/features/transactions/store';
import { usePeopleStore } from './src/features/people/store';
import { useCategoryStore } from './src/features/categories/store';
import { useDairyStore } from './src/features/dairy/store';
import { useOfflineStore } from './src/shared/store/offline';
import { supabase } from './src/lib/supabase';
import { secureLog } from './src/lib/securityUtils';
import { networkMonitor } from './src/lib/network';
import { InteractionManager } from 'react-native';
import * as SplashScreen from 'expo-splash-screen';
import * as Sentry from '@sentry/react-native';
import ErrorBoundary from './src/components/ErrorBoundary';

Sentry.init({
  dsn: process.env.EXPO_PUBLIC_SENTRY_DSN || '',
  environment: __DEV__ ? 'development' : 'production',
  tracesSampleRate: 0.1,
  attachScreenshot: true,
});

// Keep the native splash screen visible until we are ready to render.
// This means the OS-level splash (from app.json) stays up during JS parse +
// auth check — no white flash, no JS-rendered splash needed.
SplashScreen.preventAutoHideAsync().catch(() => {
  // Already hidden — safe to ignore
});

function App() {
  const [isAppReady, setIsAppReady] = React.useState(false);

  const setSession = useAuthStore((state) => state.setSession);
  const loadProfile = useAuthStore((state) => state.loadProfile);
  const setIsOffline = useOfflineStore((state) => state.setIsOffline);

  // ─── Subscription lifecycle helpers ───────────────────────────────────────

  const setupRealtimeSubscriptions = React.useCallback(() => {
    // Always clean up first to prevent duplicate channels on TOKEN_REFRESHED
    cleanupSubscriptions();
    useTransactionStore.getState().setupRealtimeSubscription();
    usePeopleStore.getState().setupRealtimeSubscription();
    useCategoryStore.getState().setupRealtimeSubscription();
    useDairyStore.getState().setupRealtimeSubscription();
    useMessagesStore.getState().setupRealtimeSubscription();
    useRequestsStore.getState().setupRealtimeSubscription();
  }, []);

  const cleanupSubscriptions = () => {
    useTransactionStore.getState().cleanupSubscription();
    usePeopleStore.getState().cleanupSubscription();
    useCategoryStore.getState().cleanupSubscription();
    useDairyStore.getState().cleanupSubscription();
    useMessagesStore.getState().cleanupSubscription();
    useRequestsStore.getState().cleanupSubscription();
  };

  // ─── App Initialization ────────────────────────────────────────────────────

  React.useEffect(() => {
    let isMounted = true;

    const initApp = async () => {
      try {
        // 1. Check for an existing session (async, ~100-200ms on device)
        const { data: { session }, error } = await supabase.auth.getSession();
        if (error) throw error;

        if (session && isMounted) {
          setSession(session);

          // 2. Load profile + initial transaction page in parallel
          await Promise.all([
            loadProfile(),
            useTransactionStore.getState().fetchPage(true),
          ]);

          // 3. Defer WebSocket subscriptions until after first render
          //    so they don't compete with the main thread drawing the UI
          InteractionManager.runAfterInteractions(() => {
            if (isMounted) setupRealtimeSubscriptions();
          });
        }
      } catch (err) {
        secureLog.error('App init failed:', err);
        // On any fatal auth error, sign out to a clean state
        await supabase.auth.signOut().catch(() => { });
        setSession(null);
      } finally {
        if (isMounted) {
          // 4. Ready — hide native splash screen and render the navigator
          setIsAppReady(true);
          await SplashScreen.hideAsync().catch(() => { });
        }
      }
    };

    initApp();

    // ─── Auth state listener ───────────────────────────────────────────────
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      secureLog.debug('Auth event:', event);

      // Use a microtask to avoid blocking the Supabase event handler thread
      Promise.resolve().then(async () => {
        if (!isMounted) return;

        if (event === 'SIGNED_IN' || event === 'TOKEN_REFRESHED') {
          if (session) {
            setSession(session);
            await loadProfile();
            // Cleanup old channels before opening new ones
            InteractionManager.runAfterInteractions(() => {
              if (isMounted) setupRealtimeSubscriptions();
            });
          }
        } else if (event === 'SIGNED_OUT' || event === 'USER_UPDATED') {
          cleanupSubscriptions();
          setSession(session);
        }
      });
    });

    // ─── Network listener ──────────────────────────────────────────────────
    const unsubscribeNetwork = networkMonitor.subscribe(isConnected => {
      setIsOffline(!isConnected);
    });

    return () => {
      isMounted = false;
      subscription.unsubscribe();
      cleanupSubscriptions();
      unsubscribeNetwork();
    };
  }, []);

  // While initializing: the OS native splash is showing — render nothing.
  // This avoids the old pattern of rendering a JS splash (which itself had
  // its own loading time and race conditions).
  if (!isAppReady) {
    return null;
  }

  return (
    <ErrorBoundary>
      <SafeAreaProvider>
        <ActionSheetProvider>
          <AppNavigator />
        </ActionSheetProvider>
        <StatusBar style="auto" />
      </SafeAreaProvider>
    </ErrorBoundary>
  );
}

export default Sentry.wrap(App);
