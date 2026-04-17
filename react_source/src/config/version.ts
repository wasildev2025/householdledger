/**
 * Application Version Configuration
 * 
 * Provides version information for display in the app.
 */

import Constants from 'expo-constants';

interface VersionInfo {
    version: string;
    buildNumber: string;
    name: string;
}

export const getVersionInfo = (): VersionInfo => {
    const expoConfig = Constants.expoConfig;

    return {
        version: expoConfig?.version || '1.0.0',
        buildNumber: expoConfig?.ios?.buildNumber || expoConfig?.android?.versionCode?.toString() || '1',
        name: expoConfig?.name || 'Household Ledger',
    };
};

export const getVersionString = (): string => {
    const { version, buildNumber } = getVersionInfo();
    return `v${version} (${buildNumber})`;
};
