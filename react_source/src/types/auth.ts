/**
 * Supabase Auth Types
 * These provide proper typing for Supabase authentication objects
 * Compatible with @supabase/supabase-js types
 */

import type { User, Session } from '@supabase/supabase-js';

// Re-export Supabase types for consistency
export type AuthUser = User;
export type AuthSession = Session;

/**
 * Current User State
 * Represents the logged-in user's context within the app
 */
export interface CurrentUser {
    role: 'admin' | 'servant' | 'member';
    servantId?: string;
    memberId?: string;
}

/**
 * DateTimePicker Event Type
 * Matches @react-native-community/datetimepicker
 */
export interface DateTimePickerEvent {
    type: 'set' | 'dismissed' | 'neutralButtonPressed';
    nativeEvent: {
        timestamp?: number;
        utcOffset?: number;
    };
}
