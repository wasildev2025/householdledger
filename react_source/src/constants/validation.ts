/**
 * Validation Constants
 * Centralized constants for input validation across the application
 */

export const VALIDATION = {
    // PIN validation
    PIN_LENGTH: 4,
    PIN_PATTERN: /^\d{4}$/,

    // Invite codes
    INVITE_CODE_LENGTH: 6,
    INVITE_CODE_PATTERN: /^\d{6}$/,

    // Name validation
    MIN_NAME_LENGTH: 2,
    MAX_NAME_LENGTH: 100,
    NAME_PATTERN: /^[a-zA-Z\s\-']+$/,

    // Description validation
    MAX_DESCRIPTION_LENGTH: 500,

    // Amount validation
    MIN_AMOUNT: 0,
    MAX_AMOUNT: 99999999, // Maximum reasonable transaction amount

    // Phone validation
    PHONE_PATTERN: /^[+]?[\d\s\-()]{7,20}$/,

    // Household name
    MIN_HOUSEHOLD_NAME_LENGTH: 2,
    MAX_HOUSEHOLD_NAME_LENGTH: 50,
    HOUSEHOLD_NAME_PATTERN: /^[a-zA-Z0-9\s\-']+$/,

    // Dairy quantities
    MIN_QUANTITY: 0,
    MAX_QUANTITY: 100,

    // Prices
    MIN_PRICE: 0,
    MAX_PRICE: 10000,
} as const;

/**
 * Validation error messages
 */
export const VALIDATION_MESSAGES = {
    // PIN
    PIN_REQUIRED: 'PIN is required',
    PIN_INVALID: 'PIN must be exactly 4 digits',

    // Name
    NAME_REQUIRED: 'Name is required',
    NAME_TOO_SHORT: `Name must be at least ${VALIDATION.MIN_NAME_LENGTH} characters`,
    NAME_TOO_LONG: `Name must be at most ${VALIDATION.MAX_NAME_LENGTH} characters`,
    NAME_INVALID_CHARS: 'Name contains invalid characters',

    // Amount
    AMOUNT_REQUIRED: 'Amount is required',
    AMOUNT_INVALID: 'Please enter a valid amount',
    AMOUNT_TOO_SMALL: 'Amount must be greater than 0',
    AMOUNT_TOO_LARGE: `Amount cannot exceed ${VALIDATION.MAX_AMOUNT.toLocaleString()}`,

    // Description
    DESCRIPTION_TOO_LONG: `Description must be at most ${VALIDATION.MAX_DESCRIPTION_LENGTH} characters`,

    // Phone
    PHONE_INVALID: 'Please enter a valid phone number',

    // Household
    HOUSEHOLD_NAME_REQUIRED: 'Household name is required',
    HOUSEHOLD_NAME_TOO_SHORT: `Household name must be at least ${VALIDATION.MIN_HOUSEHOLD_NAME_LENGTH} characters`,
    HOUSEHOLD_NAME_TOO_LONG: `Household name must be at most ${VALIDATION.MAX_HOUSEHOLD_NAME_LENGTH} characters`,
    HOUSEHOLD_NAME_INVALID_CHARS: 'Household name contains invalid characters',

    // Invite code
    INVITE_CODE_REQUIRED: 'Invite code is required',
    INVITE_CODE_INVALID: 'Please enter a valid 6-digit invite code',

    // Quantity
    QUANTITY_INVALID: 'Please enter a valid quantity',
    QUANTITY_TOO_SMALL: 'Quantity cannot be negative',
    QUANTITY_TOO_LARGE: `Quantity cannot exceed ${VALIDATION.MAX_QUANTITY}`,

    // Price
    PRICE_INVALID: 'Please enter a valid price',
    PRICE_TOO_SMALL: 'Price cannot be negative',
    PRICE_TOO_LARGE: `Price cannot exceed ${VALIDATION.MAX_PRICE}`,

    // Category
    CATEGORY_REQUIRED: 'Please select a category',

    // Staff
    STAFF_REQUIRED: 'Please select who this is for',

    // Role
    ROLE_REQUIRED: 'Role is required',
} as const;
