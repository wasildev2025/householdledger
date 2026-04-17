/**
 * Security Utilities for Household Ledger
 * 
 * Provides secure implementations for:
 * - PIN hashing and verification
 * - Secure random number generation
 * - Input validation
 * - Environment-aware logging
 */

import 'react-native-get-random-values';
import { Config } from '../config/env';

// ============================================
// SECURE RANDOM GENERATION
// ============================================

/**
 * Generates a cryptographically secure random invite code.
 * Uses crypto.getRandomValues() instead of Math.random()
 * 
 * @returns A 6-character alphanumeric code
 */
export const generateSecureInviteCode = (): string => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Excluded confusing chars: I, O, 0, 1
    const codeLength = 6;
    const randomValues = new Uint8Array(codeLength);
    crypto.getRandomValues(randomValues);

    let code = '';
    for (let i = 0; i < codeLength; i++) {
        code += chars[randomValues[i] % chars.length];
    }
    return code;
};

// ============================================
// PIN HASHING (Client-side)
// ============================================

/**
 * Hashes a PIN using SHA-256.
 * Note: For production, use a server-side hash with bcrypt/Argon2.
 * This provides basic protection against plaintext storage.
 * 
 * @param pin - The plaintext PIN
 * @returns A hex-encoded SHA-256 hash
 */
export const hashPin = async (pin: string): Promise<string> => {
    if (!pin) return '';

    // Add a static salt for basic additional security
    const salted = `household_ledger_${pin}_v1`;

    // Use SubtleCrypto API for hashing
    const encoder = new TextEncoder();
    const data = encoder.encode(salted);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);

    // Convert to hex string
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    return hashHex;
};

/**
 * Verifies a PIN against its hash.
 * 
 * @param pin - The plaintext PIN to verify
 * @param hash - The stored hash
 * @returns True if the PIN matches
 */
export const verifyPin = async (pin: string, hash: string): Promise<boolean> => {
    if (!pin || !hash) return false;
    const inputHash = await hashPin(pin);
    return inputHash === hash;
};

// ============================================
// INPUT VALIDATION
// ============================================

export interface ValidationResult {
    isValid: boolean;
    errors: string[];
}

/**
 * Validates a PIN code.
 * Must be exactly 4 digits.
 */
export const validatePin = (pin: string): ValidationResult => {
    const errors: string[] = [];

    if (pin && !/^\d{4}$/.test(pin)) {
        errors.push('PIN must be exactly 4 digits');
    }

    return { isValid: errors.length === 0, errors };
};

/**
 * Validates a name field.
 */
export const validateName = (name: string, fieldName: string = 'Name'): ValidationResult => {
    const errors: string[] = [];

    if (!name || !name.trim()) {
        errors.push(`${fieldName} is required`);
    } else if (name.trim().length < 2) {
        errors.push(`${fieldName} must be at least 2 characters`);
    } else if (name.trim().length > 100) {
        errors.push(`${fieldName} must be less than 100 characters`);
    } else if (!/^[a-zA-Z\s\-'.]+$/.test(name.trim())) {
        errors.push(`${fieldName} contains invalid characters`);
    }

    return { isValid: errors.length === 0, errors };
};

/**
 * Validates a phone number.
 */
export const validatePhone = (phone: string): ValidationResult => {
    const errors: string[] = [];

    if (phone && !/^[\d\s\-+().]{7,20}$/.test(phone)) {
        errors.push('Please enter a valid phone number');
    }

    return { isValid: errors.length === 0, errors };
};

/**
 * Validates a numeric amount (salary, budget, etc).
 */
export const validateAmount = (amount: string | number, fieldName: string = 'Amount'): ValidationResult => {
    const errors: string[] = [];
    const numValue = typeof amount === 'string' ? parseFloat(amount) : amount;

    if (amount !== '' && amount !== undefined) {
        if (isNaN(numValue)) {
            errors.push(`${fieldName} must be a valid number`);
        } else if (numValue < 0) {
            errors.push(`${fieldName} cannot be negative`);
        } else if (numValue > 999999999) {
            errors.push(`${fieldName} is too large`);
        }
    }

    return { isValid: errors.length === 0, errors };
};

/**
 * Validates servant/staff data.
 */
export const validateServantData = (data: {
    name: string;
    role: string;
    phoneNumber?: string;
    salary?: string;
    budget?: string;
    pin?: string;
}): ValidationResult => {
    const errors: string[] = [];

    const nameResult = validateName(data.name, 'Full name');
    errors.push(...nameResult.errors);

    if (!data.role || !data.role.trim()) {
        errors.push('Role is required');
    }

    if (data.phoneNumber) {
        const phoneResult = validatePhone(data.phoneNumber);
        errors.push(...phoneResult.errors);
    }

    if (data.salary) {
        const salaryResult = validateAmount(data.salary, 'Salary');
        errors.push(...salaryResult.errors);
    }

    if (data.budget) {
        const budgetResult = validateAmount(data.budget, 'Budget');
        errors.push(...budgetResult.errors);
    }

    if (data.pin) {
        const pinResult = validatePin(data.pin);
        errors.push(...pinResult.errors);
    }

    return { isValid: errors.length === 0, errors };
};

/**
 * Validates member data.
 */
export const validateMemberData = (data: {
    name: string;
    pin?: string;
}): ValidationResult => {
    const errors: string[] = [];

    const nameResult = validateName(data.name, 'Name');
    errors.push(...nameResult.errors);

    if (data.pin) {
        const pinResult = validatePin(data.pin);
        errors.push(...pinResult.errors);
    }

    return { isValid: errors.length === 0, errors };
};

// ============================================
// IMPORT DATA VALIDATION
// ============================================

/**
 * Validates imported JSON data structure.
 */
export const validateImportData = (data: any): ValidationResult => {
    const errors: string[] = [];

    if (!data || typeof data !== 'object') {
        errors.push('Invalid data format');
        return { isValid: false, errors };
    }

    // Check for malicious prototype pollution
    if ('__proto__' in data || 'constructor' in data || 'prototype' in data) {
        errors.push('Invalid data structure detected');
        return { isValid: false, errors };
    }

    // Validate arrays have correct structure
    if (data.servants && !Array.isArray(data.servants)) {
        errors.push('Invalid servants data');
    }

    if (data.transactions && !Array.isArray(data.transactions)) {
        errors.push('Invalid transactions data');
    }

    if (data.categories && !Array.isArray(data.categories)) {
        errors.push('Invalid categories data');
    }

    if (data.members && !Array.isArray(data.members)) {
        errors.push('Invalid members data');
    }

    // Validate amounts are non-negative
    if (data.transactions) {
        for (const t of data.transactions) {
            if (typeof t.amount !== 'number' || t.amount < 0) {
                errors.push('Transaction amounts must be non-negative numbers');
                break;
            }
        }
    }

    return { isValid: errors.length === 0, errors };
};

// ============================================
// SECURE LOGGING
// ============================================

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

/**
 * Environment-aware logger that suppresses logs in production.
 */
export const secureLog = {
    debug: (message: string, ...args: any[]) => {
        if (Config.IS_DEV) {
            console.log(`[DEBUG] ${message}`, ...sanitizeLogArgs(args));
        }
    },

    info: (message: string, ...args: any[]) => {
        if (Config.IS_DEV) {
            console.log(`[INFO] ${message}`, ...sanitizeLogArgs(args));
        }
    },

    warn: (message: string, ...args: any[]) => {
        if (Config.IS_DEV) {
            console.warn(`[WARN] ${message}`, ...sanitizeLogArgs(args));
        }
    },

    error: (message: string, ...args: any[]) => {
        // Errors are logged in dev, but sanitized in production
        if (Config.IS_DEV) {
            console.error(`[ERROR] ${message}`, ...sanitizeLogArgs(args));
        } else {
            // In production, only log minimal error info
            console.error(`[ERROR] ${message}`);
        }
    },
};

/**
 * Sanitizes log arguments to remove sensitive data.
 */
const sanitizeLogArgs = (args: any[]): any[] => {
    return args.map(arg => {
        if (typeof arg === 'object' && arg !== null) {
            const sanitized = { ...arg };
            // Remove sensitive fields
            const sensitiveFields = ['pin', 'password', 'token', 'apiKey', 'secret', 'key'];
            for (const field of sensitiveFields) {
                if (field in sanitized) {
                    sanitized[field] = '[REDACTED]';
                }
            }
            return sanitized;
        }
        return arg;
    });
};

// ============================================
// URL VALIDATION
// ============================================

/**
 * Validates that a URL uses HTTPS.
 */
export const validateHttpsUrl = (url: string): boolean => {
    if (!url) return false;
    try {
        const parsed = new URL(url);
        return parsed.protocol === 'https:';
    } catch {
        return false;
    }
};

// ============================================
// DATA ANONYMIZATION FOR AI
// ============================================

/**
 * Anonymizes transaction data before sending to AI service.
 * Removes potentially identifying information.
 */
export const anonymizeTransactionData = (transactions: any[]): any[] => {
    return transactions.map((t, index) => ({
        id: `txn_${index}`,
        amount: t.amount,
        type: t.type,
        category: t.categoryName || 'Unknown',
        // Remove description, dates, and identifiers
    }));
};

// ============================================
// RATE LIMITING
// ============================================

interface RateLimitState {
    attempts: number;
    lastAttempt: number;
    lockedUntil: number | null;
}

const rateLimitStore: Map<string, RateLimitState> = new Map();

/**
 * Checks if an action is rate limited.
 * 
 * @param key - Unique identifier for the rate limit (e.g., user ID)
 * @param maxAttempts - Maximum attempts before lockout
 * @param lockoutMs - Lockout duration in milliseconds
 * @returns Object with isLocked status and remaining lockout time
 */
export const checkRateLimit = (
    key: string,
    maxAttempts: number = 5,
    lockoutMs: number = 30000
): { isLocked: boolean; remainingMs: number; attemptsLeft: number } => {
    const now = Date.now();
    let state = rateLimitStore.get(key);

    if (!state) {
        state = { attempts: 0, lastAttempt: now, lockedUntil: null };
        rateLimitStore.set(key, state);
    }

    // Check if currently locked
    if (state.lockedUntil && now < state.lockedUntil) {
        return {
            isLocked: true,
            remainingMs: state.lockedUntil - now,
            attemptsLeft: 0
        };
    }

    // Reset if lockout has expired
    if (state.lockedUntil && now >= state.lockedUntil) {
        state.attempts = 0;
        state.lockedUntil = null;
    }

    return {
        isLocked: false,
        remainingMs: 0,
        attemptsLeft: maxAttempts - state.attempts
    };
};

/**
 * Records a failed attempt and applies lockout if needed.
 */
export const recordFailedAttempt = (
    key: string,
    maxAttempts: number = 5,
    lockoutMs: number = 30000
): void => {
    const now = Date.now();
    let state = rateLimitStore.get(key);

    if (!state) {
        state = { attempts: 0, lastAttempt: now, lockedUntil: null };
        rateLimitStore.set(key, state);
    }

    state.attempts += 1;
    state.lastAttempt = now;

    if (state.attempts >= maxAttempts) {
        state.lockedUntil = now + lockoutMs;
    }

    rateLimitStore.set(key, state);
};

/**
 * Resets rate limit for a key (e.g., after successful login).
 */
export const resetRateLimit = (key: string): void => {
    rateLimitStore.delete(key);
};
