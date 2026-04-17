export const COLORS_LIGHT = {
    primary: '#0F172A', // Midnight Slate
    secondary: '#334155', // Muted Slate
    accentGreen: '#10B981', // Emerald
    accentTeal: '#0D9488',
    accentBlue: '#3B82F6', // Modern Blue
    textDark: '#0F172A',
    textGrey: '#64748B',
    textLight: '#F8FAFC',
    background: '#F8FAFC',
    card: '#FFFFFF',
    white: '#FFFFFF',
    danger: '#EF4444',
    success: '#10B981',
    warning: '#F59E0B',
    border: '#E2E8F0',
    glass: 'rgba(255, 255, 255, 0.7)',
    glassBorder: 'rgba(255, 255, 255, 0.4)',
    glassShadow: 'rgba(15, 23, 42, 0.05)',

    // Chart Colors
    chart: [
        '#3B82F6', // Blue
        '#10B981', // Emerald
        '#F59E0B', // Amber
        '#EF4444', // Red
        '#8B5CF6', // Violet
        '#06B6D4', // Cyan
        '#64748B', // Slate
    ]
};

export const COLORS_DARK = {
    primary: '#1E293B',
    secondary: '#0F172A',
    accentGreen: '#34D399',
    accentTeal: '#2DD4BF',
    accentBlue: '#60A5FA',
    textDark: '#F8FAFC',
    textGrey: '#94A3B8',
    textLight: '#0F172A',
    background: '#020617', // Deeper Night
    card: '#0F172A',
    white: '#FFFFFF',
    danger: '#F87171',
    success: '#34D399',
    warning: '#FBBF24',
    border: '#1E293B',
    glass: 'rgba(15, 23, 42, 0.7)',
    glassBorder: 'rgba(255, 255, 255, 0.1)',
    glassShadow: 'rgba(0, 0, 0, 0.5)',

    chart: [
        '#60A5FA',
        '#34D399',
        '#FBBF24',
        '#F87171',
        '#A78BFA',
        '#22D3EE',
        '#94A3B8',
    ]
};

export const GRADIENTS = {
    premium: ['#0F172A', '#1E293B'],
    ocean: ['#1e3c72', '#2a5298'],
    sunset: ['#FF512F', '#DD2476'],
    glass: ['rgba(255, 255, 255, 0.1)', 'rgba(255, 255, 255, 0.02)'],
    vibrant: ['#6366F1', '#A855F7', '#EC4899'],
};

export const COLORS = COLORS_LIGHT;

export const SIZES = {
    padding: 24,
    radius: 20,
    h1: 32,
    h2: 24,
    h3: 20,
    body: 16,
    small: 14,
    xSmall: 12,
    margin: 16,
    marginSmall: 8,
    marginLarge: 32,
};

export const LAYOUT = {
    blur: {
        low: 10,
        medium: 20,
        high: 30,
    }
};

export const SHADOWS = {
    small: {
        shadowColor: "#0F172A",
        shadowOffset: {
            width: 0,
            height: 4,
        },
        shadowOpacity: 0.05,
        shadowRadius: 10,
        elevation: 2,
    },
    medium: {
        shadowColor: "#0F172A",
        shadowOffset: {
            width: 0,
            height: 10,
        },
        shadowOpacity: 0.08,
        shadowRadius: 20,
        elevation: 5,
    },
    large: {
        shadowColor: "#0F172A",
        shadowOffset: {
            width: 0,
            height: 20,
        },
        shadowOpacity: 0.12,
        shadowRadius: 30,
        elevation: 10,
    },
    premium: {
        shadowColor: "#3B82F6",
        shadowOffset: {
            width: 0,
            height: 12,
        },
        shadowOpacity: 0.15,
        shadowRadius: 25,
        elevation: 8,
    }
};
