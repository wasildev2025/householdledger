/**
 * Error Handler Utility
 * Centralized error handling with user-friendly messages
 */

import { Alert } from 'react-native';
import { secureLog } from './securityUtils';

/**
 * Standard error types for categorization
 */
export type ErrorType =
    | 'network'
    | 'auth'
    | 'validation'
    | 'database'
    | 'permission'
    | 'unknown';

/**
 * Error severity levels
 */
export type ErrorSeverity = 'low' | 'medium' | 'high' | 'critical';

/**
 * Error context for structured logging
 */
interface ErrorContext {
    type?: ErrorType;
    severity?: ErrorSeverity;
    context: string;
    showAlert?: boolean;
    customMessage?: string;
}

/**
 * User-friendly error messages by type
 */
const ERROR_MESSAGES: Record<ErrorType, string> = {
    network: 'Unable to connect. Please check your internet connection.',
    auth: 'Authentication failed. Please try logging in again.',
    validation: 'Please check your input and try again.',
    database: 'Unable to save data. Please try again.',
    permission: "You don't have permission to perform this action.",
    unknown: 'Something went wrong. Please try again.',
};

/**
 * Extract user-friendly message from error
 */
const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
        // Check for common error patterns
        const message = error.message.toLowerCase();

        if (message.includes('network') || message.includes('fetch')) {
            return ERROR_MESSAGES.network;
        }
        if (message.includes('unauthorized') || message.includes('auth')) {
            return ERROR_MESSAGES.auth;
        }
        if (message.includes('permission') || message.includes('denied')) {
            return ERROR_MESSAGES.permission;
        }

        // Return the original message if it's meaningful
        if (error.message.length > 0 && error.message.length < 200) {
            return error.message;
        }
    }

    if (typeof error === 'string') {
        return error;
    }

    return ERROR_MESSAGES.unknown;
};

/**
 * Detect error type from error object
 */
const detectErrorType = (error: unknown): ErrorType => {
    if (error instanceof Error) {
        const message = error.message.toLowerCase();

        if (message.includes('network') || message.includes('fetch') || message.includes('timeout')) {
            return 'network';
        }
        if (message.includes('unauthorized') || message.includes('auth') || message.includes('token')) {
            return 'auth';
        }
        if (message.includes('validation') || message.includes('invalid')) {
            return 'validation';
        }
        if (message.includes('database') || message.includes('insert') || message.includes('update')) {
            return 'database';
        }
        if (message.includes('permission') || message.includes('denied') || message.includes('access')) {
            return 'permission';
        }
    }

    return 'unknown';
};

/**
 * Handle error with logging and optional user alert
 * 
 * @param error - The error to handle
 * @param options - Error handling options
 * @returns User-friendly error message
 */
export const handleError = (
    error: unknown,
    options: ErrorContext
): string => {
    const {
        type = detectErrorType(error),
        severity = 'medium',
        context,
        showAlert = true,
        customMessage,
    } = options;

    // Log the error with context
    const logMessage = `[${type.toUpperCase()}] ${context}`;

    switch (severity) {
        case 'critical':
        case 'high':
            secureLog.error(logMessage, error);
            break;
        case 'medium':
            secureLog.warn(logMessage, error);
            break;
        case 'low':
            secureLog.info(logMessage, error);
            break;
    }

    // Get user-friendly message
    const userMessage = customMessage || getErrorMessage(error);

    // Show alert if requested
    if (showAlert) {
        Alert.alert('Error', userMessage);
    }

    return userMessage;
};

/**
 * Wrap an async function with error handling
 */
export const withErrorHandling = <T extends unknown[], R>(
    fn: (...args: T) => Promise<R>,
    context: string
) => {
    return async (...args: T): Promise<R | null> => {
        try {
            return await fn(...args);
        } catch (error) {
            handleError(error, { context });
            return null;
        }
    };
};

/**
 * Try-catch wrapper that doesn't show alert
 */
export const silentErrorHandler = (
    error: unknown,
    context: string
): void => {
    handleError(error, { context, showAlert: false });
};

/**
 * Validation error handler - specialized for form validation
 */
export const handleValidationError = (
    fieldName: string,
    message: string
): void => {
    Alert.alert('Validation Error', `${fieldName}: ${message}`);
};

/**
 * Network error handler - specialized for API calls
 */
export const handleNetworkError = (
    error: unknown,
    context: string
): void => {
    handleError(error, {
        type: 'network',
        severity: 'medium',
        context,
        customMessage: ERROR_MESSAGES.network,
    });
};
