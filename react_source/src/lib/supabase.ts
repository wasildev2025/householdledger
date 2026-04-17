import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient } from '@supabase/supabase-js';
import { Config } from '../config/env';

const supabaseUrl = Config.SUPABASE_URL;
const supabaseAnonKey = Config.SUPABASE_ANON_KEY;

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
    auth: {
        storage: AsyncStorage,
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: false,
    },
    global: {
        fetch: (url, options = {}) => {
            const controller = new AbortController();
            // 10 second timeout for network calls
            const timeout = setTimeout(() => controller.abort(), 10000);
            return fetch(url, { ...options, signal: controller.signal })
                .finally(() => clearTimeout(timeout));
        }
    }
});
