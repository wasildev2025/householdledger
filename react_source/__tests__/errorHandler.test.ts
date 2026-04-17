/**
 * Tests for Error Handler Utility
 * Tests error type detection, message generation, and handling
 */

import { Alert } from 'react-native';
import {
    handleError,
    withErrorHandling,
    silentErrorHandler,
    handleValidationError,
    handleNetworkError,
} from '../src/lib/errorHandler';

// Mock Alert
jest.mock('react-native', () => ({
    Alert: {
        alert: jest.fn(),
    },
}));

describe('handleError', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should show alert by default', () => {
        handleError(new Error('Test error'), { context: 'Testing' });

        expect(Alert.alert).toHaveBeenCalledWith('Error', expect.any(String));
    });

    test('should not show alert when showAlert is false', () => {
        handleError(new Error('Test error'), {
            context: 'Testing',
            showAlert: false
        });

        expect(Alert.alert).not.toHaveBeenCalled();
    });

    test('should use custom message when provided', () => {
        handleError(new Error('Technical error'), {
            context: 'Testing',
            customMessage: 'User friendly message'
        });

        expect(Alert.alert).toHaveBeenCalledWith('Error', 'User friendly message');
    });

    test('should return user-friendly message', () => {
        const message = handleError(new Error('Test'), {
            context: 'Testing',
            showAlert: false
        });

        expect(typeof message).toBe('string');
        expect(message.length).toBeGreaterThan(0);
    });

    test('should detect network errors', () => {
        const message = handleError(new Error('Network request failed'), {
            context: 'API call',
            showAlert: false,
        });

        expect(message).toContain('connect');
    });

    test('should detect auth errors', () => {
        const message = handleError(new Error('Unauthorized access'), {
            context: 'Auth',
            showAlert: false,
        });

        // The handler returns a user-friendly auth message
        expect(message).toContain('Authentication');
    });

    test('should handle string errors', () => {
        const message = handleError('String error message', {
            context: 'Testing',
            showAlert: false,
        });

        expect(message).toBe('String error message');
    });

    test('should handle unknown error types', () => {
        const message = handleError({ weird: 'object' }, {
            context: 'Testing',
            showAlert: false,
        });

        expect(message).toContain('wrong');
    });
});

describe('withErrorHandling', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should return result on success', async () => {
        const successFn = jest.fn().mockResolvedValue('success');
        const wrappedFn = withErrorHandling(successFn, 'Test');

        const result = await wrappedFn();

        expect(result).toBe('success');
        expect(Alert.alert).not.toHaveBeenCalled();
    });

    test('should handle error and return null on failure', async () => {
        const failFn = jest.fn().mockRejectedValue(new Error('Failed'));
        const wrappedFn = withErrorHandling(failFn, 'Test');

        const result = await wrappedFn();

        expect(result).toBeNull();
        expect(Alert.alert).toHaveBeenCalled();
    });

    test('should pass arguments to wrapped function', async () => {
        const fn = jest.fn().mockResolvedValue('ok');
        const wrappedFn = withErrorHandling(fn, 'Test');

        await wrappedFn('arg1', 'arg2');

        expect(fn).toHaveBeenCalledWith('arg1', 'arg2');
    });
});

describe('silentErrorHandler', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should not show alert', () => {
        silentErrorHandler(new Error('Silent error'), 'Testing');

        expect(Alert.alert).not.toHaveBeenCalled();
    });
});

describe('handleValidationError', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should show validation error with field name', () => {
        handleValidationError('Email', 'Invalid format');

        expect(Alert.alert).toHaveBeenCalledWith(
            'Validation Error',
            'Email: Invalid format'
        );
    });
});

describe('handleNetworkError', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should show network-specific error message', () => {
        handleNetworkError(new Error('Fetch failed'), 'API call');

        expect(Alert.alert).toHaveBeenCalledWith(
            'Error',
            expect.stringContaining('connect')
        );
    });
});
