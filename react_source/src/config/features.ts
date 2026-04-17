import { Config } from './env';

export enum FeatureFlag {
    EXPORT_DATA = 'EXPORT_DATA',
    DARK_MODE = 'DARK_MODE',
    NOTIFICATIONS = 'NOTIFICATIONS',
    MULTI_CURRENCY = 'MULTI_CURRENCY',
}

// Default overrides per environment
const ENV_FLAGS: Record<string, Partial<Record<FeatureFlag, boolean>>> = {
    development: {
        [FeatureFlag.EXPORT_DATA]: true,
        [FeatureFlag.DARK_MODE]: true,
        [FeatureFlag.NOTIFICATIONS]: true,
        [FeatureFlag.MULTI_CURRENCY]: true,
    },
    production: {
        [FeatureFlag.EXPORT_DATA]: true,
        [FeatureFlag.DARK_MODE]: true,
        [FeatureFlag.NOTIFICATIONS]: true,
        [FeatureFlag.MULTI_CURRENCY]: true,
    }
};

export const checkFeature = (flag: FeatureFlag): boolean => {
    const envFlags = ENV_FLAGS[Config.ENV] || ENV_FLAGS['development'];
    return envFlags[flag] ?? false;
};

export const useFeature = (flag: FeatureFlag): boolean => {
    return checkFeature(flag);
};
