/**
 * Tests for Security Utilities
 * Tests PIN hashing, secure random generation, rate limiting, and validation
 */

import {
    hashPin,
    verifyPin,
    generateSecureInviteCode,
    validatePin,
    validateName,
    validatePhone,
    validateAmount,
    validateServantData,
    validateMemberData,
    validateImportData,
    checkRateLimit,
    recordFailedAttempt,
    resetRateLimit,
    validateHttpsUrl,
    secureLog,
    anonymizeTransactionData,
} from '../src/lib/securityUtils';

describe('PIN Hashing', () => {
    test('hashPin should return a hash different from the original PIN', async () => {
        const pin = '1234';
        const hash = await hashPin(pin);

        expect(hash).not.toBe(pin);
        expect(hash.length).toBeGreaterThan(0);
    });

    test('hashPin should return consistent hash for same input', async () => {
        const pin = '1234';
        const hash1 = await hashPin(pin);
        const hash2 = await hashPin(pin);

        expect(hash1).toBe(hash2);
    });

    test('hashPin should return different hashes for different PINs', async () => {
        const hash1 = await hashPin('1234');
        const hash2 = await hashPin('5678');

        expect(hash1).not.toBe(hash2);
    });

    test('verifyPin should return true for correct PIN', async () => {
        const pin = '1234';
        const hash = await hashPin(pin);

        const result = await verifyPin(pin, hash);
        expect(result).toBe(true);
    });

    test('verifyPin should return false for incorrect PIN', async () => {
        const hash = await hashPin('1234');

        const result = await verifyPin('5678', hash);
        expect(result).toBe(false);
    });

    test('verifyPin should return false for empty hash', async () => {
        const result = await verifyPin('1234', '');
        expect(result).toBe(false);
    });
});

describe('Secure Invite Code Generation', () => {
    test('generateSecureInviteCode should return 6-character code', () => {
        const code = generateSecureInviteCode();

        expect(code).toMatch(/^[A-Z0-9]{6}$/);
        expect(code.length).toBe(6);
    });

    test('generateSecureInviteCode should generate different codes', () => {
        const codes = new Set();
        for (let i = 0; i < 100; i++) {
            codes.add(generateSecureInviteCode());
        }

        // Should have high uniqueness (allow for some collisions in 100 tries)
        expect(codes.size).toBeGreaterThan(90);
    });
});

describe('PIN Validation', () => {
    test('validatePin should accept valid 4-digit PIN', () => {
        expect(validatePin('1234').isValid).toBe(true);
        expect(validatePin('0000').isValid).toBe(true);
        expect(validatePin('9999').isValid).toBe(true);
    });

    test('validatePin should reject non-numeric PIN', () => {
        const result = validatePin('abcd');
        expect(result.isValid).toBe(false);
        expect(result.errors.length).toBeGreaterThan(0);
    });

    test('validatePin should reject wrong length PIN', () => {
        expect(validatePin('123').isValid).toBe(false);
        expect(validatePin('12345').isValid).toBe(false);
    });

    test('validatePin should accept empty PIN (optional field)', () => {
        // Based on implementation, empty PIN is valid (optional)
        expect(validatePin('').isValid).toBe(true);
    });
});

describe('Name Validation', () => {
    test('validateName should accept valid names', () => {
        expect(validateName('John').isValid).toBe(true);
        expect(validateName('Mary Jane').isValid).toBe(true);
        expect(validateName("O'Brien").isValid).toBe(true);
        expect(validateName('Anne-Marie').isValid).toBe(true);
    });

    test('validateName should reject too short names', () => {
        expect(validateName('A').isValid).toBe(false);
    });

    test('validateName should reject names with numbers', () => {
        expect(validateName('John123').isValid).toBe(false);
    });

    test('validateName should reject empty names', () => {
        expect(validateName('').isValid).toBe(false);
    });
});

describe('Phone Validation', () => {
    test('validatePhone should accept valid phone numbers', () => {
        expect(validatePhone('1234567890').isValid).toBe(true);
        expect(validatePhone('+1-234-567-8900').isValid).toBe(true);
        expect(validatePhone('(123) 456-7890').isValid).toBe(true);
    });

    test('validatePhone should reject too short numbers', () => {
        expect(validatePhone('123456').isValid).toBe(false);
    });

    test('validatePhone should allow empty phone (optional field)', () => {
        expect(validatePhone('').isValid).toBe(true);
    });
});

describe('Amount Validation', () => {
    test('validateAmount should accept valid amounts', () => {
        expect(validateAmount(100).isValid).toBe(true);
        expect(validateAmount('100').isValid).toBe(true);
        expect(validateAmount('0.01').isValid).toBe(true);
        expect(validateAmount(999999).isValid).toBe(true);
    });

    test('validateAmount should accept zero', () => {
        expect(validateAmount(0).isValid).toBe(true);
    });

    test('validateAmount should reject negative', () => {
        expect(validateAmount(-100).isValid).toBe(false);
        expect(validateAmount('-100').isValid).toBe(false);
    });

    test('validateAmount should reject very large amounts', () => {
        expect(validateAmount(9999999999).isValid).toBe(false);
    });

    test('validateAmount should allow empty (optional field)', () => {
        expect(validateAmount('').isValid).toBe(true);
    });
});

describe('Servant Data Validation', () => {
    test('validateServantData should accept valid data', () => {
        const result = validateServantData({
            name: 'John Doe',
            role: 'Cook',
            salary: '5000',
        });
        expect(result.isValid).toBe(true);
    });

    test('validateServantData should reject missing name', () => {
        const result = validateServantData({
            name: '',
            role: 'Cook',
        });
        expect(result.isValid).toBe(false);
    });

    test('validateServantData should reject missing role', () => {
        const result = validateServantData({
            name: 'John',
            role: '',
        });
        expect(result.isValid).toBe(false);
    });

    test('validateServantData should validate PIN if provided', () => {
        const result = validateServantData({
            name: 'John',
            role: 'Cook',
            pin: '12', // Invalid PIN
        });
        expect(result.isValid).toBe(false);
    });
});

describe('Member Data Validation', () => {
    test('validateMemberData should accept valid data', () => {
        const result = validateMemberData({
            name: 'Jane Doe',
        });
        expect(result.isValid).toBe(true);
    });

    test('validateMemberData should validate PIN if provided', () => {
        const result = validateMemberData({
            name: 'Jane',
            pin: 'abcd', // Invalid PIN
        });
        expect(result.isValid).toBe(false);
    });
});

describe('Import Data Validation', () => {
    test('validateImportData should accept valid structure with arrays', () => {
        // Use Object.create(null) to avoid having constructor property
        const cleanData = Object.create(null);
        cleanData.transactions = [];
        cleanData.categories = [];

        const result = validateImportData(cleanData);
        expect(result.isValid).toBe(true);
    });

    test('validateImportData should reject non-object', () => {
        expect(validateImportData('string').isValid).toBe(false);
        expect(validateImportData(null).isValid).toBe(false);
        expect(validateImportData(undefined).isValid).toBe(false);
    });

    test('validateImportData should reject prototype pollution attempts', () => {
        const malicious = JSON.parse('{"__proto__": {"isAdmin": true}}');
        const result = validateImportData(malicious);
        expect(result.isValid).toBe(false);
    });

    test('validateImportData should reject invalid transaction amounts', () => {
        const cleanData = Object.create(null);
        cleanData.transactions = [{ amount: -100 }];

        const result = validateImportData(cleanData);
        expect(result.isValid).toBe(false);
    });
});

describe('Rate Limiting', () => {
    const testKey = 'test-rate-limit';

    beforeEach(() => {
        resetRateLimit(testKey);
    });

    test('checkRateLimit should allow first attempt', () => {
        const result = checkRateLimit(testKey);
        expect(result.isLocked).toBe(false);
    });

    test('recordFailedAttempt should track attempts', () => {
        recordFailedAttempt(testKey);
        recordFailedAttempt(testKey);
        recordFailedAttempt(testKey);

        const result = checkRateLimit(testKey);
        expect(result.isLocked).toBe(false);
        expect(result.attemptsLeft).toBe(2);
    });

    test('should lock after max attempts', () => {
        for (let i = 0; i < 5; i++) {
            recordFailedAttempt(testKey);
        }

        const result = checkRateLimit(testKey);
        expect(result.isLocked).toBe(true);
        expect(result.remainingMs).toBeGreaterThan(0);
    });

    test('resetRateLimit should clear attempts', () => {
        recordFailedAttempt(testKey);
        recordFailedAttempt(testKey);
        resetRateLimit(testKey);

        const result = checkRateLimit(testKey);
        expect(result.attemptsLeft).toBe(5);
    });
});

describe('HTTPS Validation', () => {
    test('validateHttpsUrl should accept HTTPS URLs', () => {
        expect(validateHttpsUrl('https://example.com')).toBe(true);
        expect(validateHttpsUrl('https://api.example.com/v1')).toBe(true);
    });

    test('validateHttpsUrl should reject HTTP URLs', () => {
        expect(validateHttpsUrl('http://example.com')).toBe(false);
    });

    test('validateHttpsUrl should reject invalid URLs', () => {
        expect(validateHttpsUrl('not-a-url')).toBe(false);
        expect(validateHttpsUrl('')).toBe(false);
    });
});

describe('Secure Logging', () => {
    test('secureLog should have all log methods', () => {
        expect(typeof secureLog.info).toBe('function');
        expect(typeof secureLog.warn).toBe('function');
        expect(typeof secureLog.error).toBe('function');
        expect(typeof secureLog.debug).toBe('function');
    });
});

describe('Data Anonymization', () => {
    test('anonymizeTransactionData should anonymize transactions', () => {
        const transactions = [
            { id: '1', amount: 100, description: 'Personal info', type: 'expense' },
            { id: '2', amount: 200, description: 'Secret data', type: 'income' },
        ];

        const result = anonymizeTransactionData(transactions);

        // Should have anonymized IDs
        expect(result[0].id).toBe('txn_0');
        expect(result[1].id).toBe('txn_1');

        // Should not have description field
        expect(result[0].description).toBeUndefined();
        expect(result[1].description).toBeUndefined();
    });

    test('anonymizeTransactionData should preserve key fields', () => {
        const transactions = [
            { id: '1', amount: 100, description: 'Test', type: 'expense', categoryName: 'Food' },
        ];

        const result = anonymizeTransactionData(transactions);

        expect(result[0].amount).toBe(100);
        expect(result[0].type).toBe('expense');
        expect(result[0].category).toBe('Food');
    });
});
