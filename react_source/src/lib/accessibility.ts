/**
 * Accessibility Utility
 * Helper functions and constants for accessibility support
 */

import { AccessibilityInfo, Platform } from 'react-native';

/**
 * Accessibility labels for common UI elements
 */
export const A11Y_LABELS = {
    // Navigation
    BACK_BUTTON: 'Go back',
    CLOSE_BUTTON: 'Close',
    MENU_BUTTON: 'Open menu',
    NOTIFICATIONS_BUTTON: 'View notifications',
    SETTINGS_BUTTON: 'Open settings',

    // Transactions
    ADD_TRANSACTION: 'Add new transaction',
    TRANSACTION_AMOUNT: 'Transaction amount',
    TRANSACTION_DESCRIPTION: 'Transaction description',
    CATEGORY_SELECTOR: 'Select category',
    DATE_PICKER: 'Select date',

    // Forms
    NAME_INPUT: 'Enter name',
    PIN_INPUT: 'Enter 4-digit PIN',
    PHONE_INPUT: 'Enter phone number',
    AMOUNT_INPUT: 'Enter amount',

    // Actions
    SAVE_BUTTON: 'Save',
    DELETE_BUTTON: 'Delete',
    EDIT_BUTTON: 'Edit',
    SUBMIT_BUTTON: 'Submit',
    CANCEL_BUTTON: 'Cancel',

    // Status
    LOADING: 'Loading content',
    ERROR: 'An error occurred',
} as const;

/**
 * Accessibility hints for user guidance
 */
export const A11Y_HINTS = {
    // Transactions
    AMOUNT_INPUT: 'Enter the transaction amount in your currency',
    DESCRIPTION_INPUT: 'Describe the transaction for future reference',
    CATEGORY_SELECTOR: 'Tap to select a category for this transaction',
    DATE_PICKER: 'Tap to change the transaction date',

    // Forms
    PIN_INPUT: 'Enter your 4-digit security PIN',
    INVITE_CODE_INPUT: 'Enter the 6-digit invite code to join a household',

    // Actions
    SCAN_RECEIPT: 'Take a photo of your receipt to auto-fill transaction details',
} as const;

/**
 * Accessibility roles for screen readers
 */
export const A11Y_ROLES = {
    BUTTON: 'button' as const,
    HEADER: 'header' as const,
    LINK: 'link' as const,
    IMAGE: 'image' as const,
    TEXT: 'text' as const,
    ADJUSTABLE: 'adjustable' as const,
    SEARCH: 'search' as const,
    TAB: 'tab' as const,
} as const;

/**
 * Creates accessibility props for interactive elements
 */
export const createAccessibilityProps = (
    label: string,
    hint?: string,
    role: keyof typeof A11Y_ROLES = 'BUTTON'
) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityHint: hint,
    accessibilityRole: A11Y_ROLES[role],
});

/**
 * Creates accessibility props for buttons
 */
export const accessibleButton = (label: string, hint?: string) =>
    createAccessibilityProps(label, hint, 'BUTTON');

/**
 * Creates accessibility props for headers
 */
export const accessibleHeader = (label: string) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityRole: 'header' as const,
});

/**
 * Creates accessibility props for text inputs
 */
export const accessibleInput = (label: string, hint?: string) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityHint: hint,
});

/**
 * Creates accessibility props for toggles (switches)
 */
export const accessibleToggle = (label: string, value: boolean, hint?: string) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityHint: hint,
    accessibilityState: { checked: value },
    accessibilityRole: 'switch' as const,
});

/**
 * Creates accessibility props for links
 */
export const accessibleLink = (label: string, hint?: string) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityHint: hint,
    accessibilityRole: 'link' as const,
});

/**
 * Creates accessibility props for images
 */
export const accessibleImage = (label: string) => ({
    accessible: true,
    accessibilityLabel: label,
    accessibilityRole: 'image' as const,
});

/**
 * Announces a message for screen readers
 */
export const announceForAccessibility = (message: string) => {
    AccessibilityInfo.announceForAccessibility(message);
};

/**
 * Check if screen reader is enabled
 */
export const isScreenReaderEnabled = async (): Promise<boolean> => {
    return AccessibilityInfo.isScreenReaderEnabled();
};

/**
 * Subscribe to screen reader status changes
 */
export const subscribeToScreenReader = (
    callback: (isEnabled: boolean) => void
): (() => void) => {
    const subscription = AccessibilityInfo.addEventListener(
        'screenReaderChanged',
        callback
    );
    return () => subscription.remove();
};

/**
 * Format currency for accessibility
 */
export const formatCurrencyForA11y = (
    amount: number,
    symbol: string
): string => {
    const absAmount = Math.abs(amount);
    const sign = amount < 0 ? 'negative' : '';
    return `${sign} ${symbol} ${absAmount.toLocaleString()}`.trim();
};

/**
 * Format date for accessibility
 */
export const formatDateForA11y = (date: Date | string): string => {
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    });
};
