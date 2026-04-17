import { useEffect } from 'react';
import { AppState } from 'react-native';
import { useAuthStore } from '../features/auth/store';

const IDLE_LOGOUT_MINUTES = 30;

export const useIdleLogout = () => {
    const signOut = useAuthStore((s) => s.signOut);

    useEffect(() => {
        let timeout: NodeJS.Timeout | undefined;

        const resetTimer = () => {
            if (timeout) clearTimeout(timeout);
            timeout = setTimeout(() => {
                signOut();
            }, IDLE_LOGOUT_MINUTES * 60 * 1000);
        };

        resetTimer();

        const sub = AppState.addEventListener('change', (state) => {
            if (state === 'active') {
                resetTimer();
            }
        });

        return () => {
            if (timeout) clearTimeout(timeout);
            sub.remove();
        };
    }, [signOut]);
};
