import { API_URL, ENV, SUPABASE_URL, SUPABASE_ANON_KEY } from '@env';

/**
 * Validates that a URL uses HTTPS protocol.
 * @param url - URL to validate
 * @param name - Name of the config for logging
 */
const validateHttpsUrl = (url: string | undefined, name: string): string => {
    if (!url) return '';

    // In development, allow localhost URLs
    if (url.includes('localhost') || url.includes('127.0.0.1')) {
        return url;
    }

    // Warn if URL doesn't use HTTPS
    if (url && !url.startsWith('https://')) {
        console.warn(`Security Warning: ${name} should use HTTPS. Current: ${url}`);
    }

    return url;
};

export const Config = {
    API_URL: validateHttpsUrl(API_URL, 'API_URL') || 'https://localhost:3000',
    ENV: (ENV || 'development') as 'development' | 'staging' | 'production',
    SUPABASE_URL: validateHttpsUrl(SUPABASE_URL, 'SUPABASE_URL') || '',
    SUPABASE_ANON_KEY: SUPABASE_ANON_KEY || '',
    IS_DEV: ENV === 'development' || !ENV,
    IS_PROD: ENV === 'production',
};

// Runtime validation to ensure critical security configs
if (Config.IS_PROD) {
    if (!Config.SUPABASE_URL.startsWith('https://')) {
        throw new Error('Production requires HTTPS for Supabase URL');
    }
}
