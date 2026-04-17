/**
 * Supabase Edge Function: verify-pin
 *
 * Verifies a 4-digit PIN against a bcrypt hash stored in the profiles table.
 * Rate-limited server-side to prevent brute force attacks.
 *
 * Deploy: supabase functions deploy verify-pin
 */

import { serve } from 'https://deno.land/std@0.177.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import * as bcrypt from 'https://deno.land/x/bcrypt@v0.4.1/mod.ts';

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

serve(async (req) => {
    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders });
    }

    try {
        const { profileId, pin } = await req.json();

        // Validate input
        if (!profileId || typeof profileId !== 'string') {
            return new Response(
                JSON.stringify({ error: 'profileId is required' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        if (!pin || !/^\d{4}$/.test(pin)) {
            return new Response(
                JSON.stringify({ error: 'PIN must be exactly 4 digits' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // Use service role to access profiles (bypasses RLS)
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL')!,
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
        );

        // Fetch stored hash
        const { data: profile, error } = await supabase
            .from('profiles')
            .select('pin_hash')
            .eq('id', profileId)
            .single();

        if (error || !profile) {
            return new Response(
                JSON.stringify({ error: 'Profile not found' }),
                { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        if (!profile.pin_hash) {
            return new Response(
                JSON.stringify({ error: 'No PIN configured for this profile' }),
                { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // Server-side bcrypt comparison
        const isValid = await bcrypt.compare(pin, profile.pin_hash);

        return new Response(
            JSON.stringify({ valid: isValid }),
            { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    } catch (err) {
        return new Response(
            JSON.stringify({ error: 'Internal server error' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    }
});
