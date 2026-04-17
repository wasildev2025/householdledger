/**
 * Tests for Validation Constants
 * Tests validation patterns and message constants
 */

import { VALIDATION, VALIDATION_MESSAGES } from '../src/constants/validation';

describe('VALIDATION Constants', () => {
    describe('PIN Validation', () => {
        test('PIN_LENGTH should be 4', () => {
            expect(VALIDATION.PIN_LENGTH).toBe(4);
        });

        test('PIN_PATTERN should match 4-digit strings', () => {
            expect(VALIDATION.PIN_PATTERN.test('1234')).toBe(true);
            expect(VALIDATION.PIN_PATTERN.test('0000')).toBe(true);
            expect(VALIDATION.PIN_PATTERN.test('9999')).toBe(true);
        });

        test('PIN_PATTERN should reject non-4-digit strings', () => {
            expect(VALIDATION.PIN_PATTERN.test('123')).toBe(false);
            expect(VALIDATION.PIN_PATTERN.test('12345')).toBe(false);
            expect(VALIDATION.PIN_PATTERN.test('abcd')).toBe(false);
            expect(VALIDATION.PIN_PATTERN.test('')).toBe(false);
        });
    });

    describe('Invite Code Validation', () => {
        test('INVITE_CODE_PATTERN should match 6-digit strings', () => {
            expect(VALIDATION.INVITE_CODE_PATTERN.test('123456')).toBe(true);
            expect(VALIDATION.INVITE_CODE_PATTERN.test('000000')).toBe(true);
        });

        test('INVITE_CODE_PATTERN should reject non-6-digit strings', () => {
            expect(VALIDATION.INVITE_CODE_PATTERN.test('12345')).toBe(false);
            expect(VALIDATION.INVITE_CODE_PATTERN.test('1234567')).toBe(false);
            expect(VALIDATION.INVITE_CODE_PATTERN.test('abcdef')).toBe(false);
        });
    });

    describe('Name Validation', () => {
        test('NAME_PATTERN should match valid names', () => {
            expect(VALIDATION.NAME_PATTERN.test('John')).toBe(true);
            expect(VALIDATION.NAME_PATTERN.test('Mary Jane')).toBe(true);
            expect(VALIDATION.NAME_PATTERN.test("O'Brien")).toBe(true);
            expect(VALIDATION.NAME_PATTERN.test('Anne-Marie')).toBe(true);
        });

        test('NAME_PATTERN should reject invalid names', () => {
            expect(VALIDATION.NAME_PATTERN.test('John123')).toBe(false);
            expect(VALIDATION.NAME_PATTERN.test('John@Doe')).toBe(false);
            expect(VALIDATION.NAME_PATTERN.test('123')).toBe(false);
        });

        test('Name length limits should be reasonable', () => {
            expect(VALIDATION.MIN_NAME_LENGTH).toBe(2);
            expect(VALIDATION.MAX_NAME_LENGTH).toBe(100);
        });
    });

    describe('Phone Validation', () => {
        test('PHONE_PATTERN should match valid phone numbers', () => {
            expect(VALIDATION.PHONE_PATTERN.test('1234567890')).toBe(true);
            expect(VALIDATION.PHONE_PATTERN.test('+1-234-567-8900')).toBe(true);
            expect(VALIDATION.PHONE_PATTERN.test('(123) 456-7890')).toBe(true);
            expect(VALIDATION.PHONE_PATTERN.test('+92 300 1234567')).toBe(true);
        });

        test('PHONE_PATTERN should reject too short numbers', () => {
            expect(VALIDATION.PHONE_PATTERN.test('123456')).toBe(false);
        });
    });

    describe('Household Name Validation', () => {
        test('HOUSEHOLD_NAME_PATTERN should match valid household names', () => {
            expect(VALIDATION.HOUSEHOLD_NAME_PATTERN.test('Smith Family')).toBe(true);
            expect(VALIDATION.HOUSEHOLD_NAME_PATTERN.test('Home 123')).toBe(true);
            expect(VALIDATION.HOUSEHOLD_NAME_PATTERN.test("O'Brien's")).toBe(true);
        });

        test('Household name length limits should be reasonable', () => {
            expect(VALIDATION.MIN_HOUSEHOLD_NAME_LENGTH).toBe(2);
            expect(VALIDATION.MAX_HOUSEHOLD_NAME_LENGTH).toBe(50);
        });
    });

    describe('Amount Validation', () => {
        test('Amount limits should be reasonable', () => {
            expect(VALIDATION.MIN_AMOUNT).toBe(0);
            expect(VALIDATION.MAX_AMOUNT).toBe(99999999);
        });
    });

    describe('Description Validation', () => {
        test('MAX_DESCRIPTION_LENGTH should allow reasonable length', () => {
            expect(VALIDATION.MAX_DESCRIPTION_LENGTH).toBe(500);
        });
    });

    describe('Quantity Validation', () => {
        test('Quantity limits should be reasonable', () => {
            expect(VALIDATION.MIN_QUANTITY).toBe(0);
            expect(VALIDATION.MAX_QUANTITY).toBe(100);
        });
    });

    describe('Price Validation', () => {
        test('Price limits should be reasonable', () => {
            expect(VALIDATION.MIN_PRICE).toBe(0);
            expect(VALIDATION.MAX_PRICE).toBe(10000);
        });
    });
});

describe('VALIDATION_MESSAGES', () => {
    test('should have PIN messages', () => {
        expect(VALIDATION_MESSAGES.PIN_REQUIRED).toBeDefined();
        expect(VALIDATION_MESSAGES.PIN_INVALID).toBeDefined();
    });

    test('should have name messages', () => {
        expect(VALIDATION_MESSAGES.NAME_REQUIRED).toBeDefined();
        expect(VALIDATION_MESSAGES.NAME_TOO_SHORT).toBeDefined();
        expect(VALIDATION_MESSAGES.NAME_TOO_LONG).toBeDefined();
        expect(VALIDATION_MESSAGES.NAME_INVALID_CHARS).toBeDefined();
    });

    test('should have amount messages', () => {
        expect(VALIDATION_MESSAGES.AMOUNT_REQUIRED).toBeDefined();
        expect(VALIDATION_MESSAGES.AMOUNT_INVALID).toBeDefined();
        expect(VALIDATION_MESSAGES.AMOUNT_TOO_SMALL).toBeDefined();
        expect(VALIDATION_MESSAGES.AMOUNT_TOO_LARGE).toBeDefined();
    });

    test('should have household messages', () => {
        expect(VALIDATION_MESSAGES.HOUSEHOLD_NAME_REQUIRED).toBeDefined();
        expect(VALIDATION_MESSAGES.HOUSEHOLD_NAME_TOO_SHORT).toBeDefined();
        expect(VALIDATION_MESSAGES.HOUSEHOLD_NAME_TOO_LONG).toBeDefined();
    });

    test('should have invite code messages', () => {
        expect(VALIDATION_MESSAGES.INVITE_CODE_REQUIRED).toBeDefined();
        expect(VALIDATION_MESSAGES.INVITE_CODE_INVALID).toBeDefined();
    });

    test('should have quantity/price messages', () => {
        expect(VALIDATION_MESSAGES.QUANTITY_INVALID).toBeDefined();
        expect(VALIDATION_MESSAGES.QUANTITY_TOO_SMALL).toBeDefined();
        expect(VALIDATION_MESSAGES.QUANTITY_TOO_LARGE).toBeDefined();
        expect(VALIDATION_MESSAGES.PRICE_INVALID).toBeDefined();
    });

    test('messages should be non-empty strings', () => {
        Object.values(VALIDATION_MESSAGES).forEach(message => {
            expect(typeof message).toBe('string');
            expect(message.length).toBeGreaterThan(0);
        });
    });

    test('messages should include dynamic values where appropriate', () => {
        expect(VALIDATION_MESSAGES.NAME_TOO_SHORT).toContain(
            VALIDATION.MIN_NAME_LENGTH.toString()
        );
        expect(VALIDATION_MESSAGES.NAME_TOO_LONG).toContain(
            VALIDATION.MAX_NAME_LENGTH.toString()
        );
    });
});
