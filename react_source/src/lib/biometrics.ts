import * as LocalAuthentication from 'expo-local-authentication';
import { secureLog } from './securityUtils';

/**
 * Checks if biometric hardware is available and if any biometrics are enrolled
 */
export async function isBiometricAvailable(): Promise<boolean> {
    try {
        const hasHardware = await LocalAuthentication.hasHardwareAsync();
        const isEnrolled = await LocalAuthentication.isEnrolledAsync();
        return hasHardware && isEnrolled;
    } catch (error) {
        secureLog.error('Error checking biometric availability:', error);
        return false;
    }
}

/**
 * Returns the types of biometric authentication supported by the device
 */
export async function getBiometricType(): Promise<LocalAuthentication.AuthenticationType[]> {
    try {
        return await LocalAuthentication.supportedAuthenticationTypesAsync();
    } catch (error) {
        secureLog.error('Error getting biometric types:', error);
        return [];
    }
}

/**
 * Prompts the user for biometric authentication
 * @param promptMessage Message displayed to the user
 */
export async function authenticateWithBiometrics(promptMessage: string = 'Authenticate to continue'): Promise<boolean> {
    try {
        const result = await LocalAuthentication.authenticateAsync({
            promptMessage,
            fallbackLabel: 'Use PIN',
            disableDeviceFallback: false,
            cancelLabel: 'Cancel',
        });
        return result.success;
    } catch (error) {
        secureLog.error('Biometric authentication error:', error);
        return false;
    }
}
