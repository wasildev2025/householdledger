/**
 * Supabase Edge Function: set-pin
 *
 * Sets or updates a user's PIN using bcrypt hashing (cost factor 12).
 * Only the profile owner or a household admin can set a PIN.
 *
 * Deploy: supabase functions deploy set-pin
 */

import { serve } from 'https://deno.land/std@0.177.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import * as bcrypt from 'https://deno.land/x/bcrypt@v0.4.1/mod.ts';

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

serve(async (req) => {
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

        // Authenticate the caller
        const authHeader = req.headers.get('Authorization');
        if (!authHeader) {
            return new Response(
                JSON.stringify({ error: 'Unauthorized' }),
                { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // Verify the caller's JWT
        const userClient = createClient(
            Deno.env.get('SUPABASE_URL')!,
            Deno.env.get('SUPABASE_ANON_KEY')!,
            { global: { headers: { Authorization: authHeader } } }
        );

        const { data: { user }, error: authError } = await userClient.auth.getUser();
        if (authError || !user) {
            return new Response(
                JSON.stringify({ error: 'Unauthorized' }),
                { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // Use service role for the actual update
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL')!,
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
        );

        // Verify caller is either the profile owner or a household admin
        const { data: callerProfile } = await supabase
            .from('profiles')
            .select('role, household_id')
            .eq('id', user.id)
            .single();

        const { data: targetProfile } = await supabase
            .from('profiles')
            .select('household_id')
            .eq('id', profileId)
            .single();

        if (!callerProfile || !targetProfile) {
            return new Response(
                JSON.stringify({ error: 'Profile not found' }),
                { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        const isOwner = user.id === profileId;
        const isHouseholdAdmin =
            callerProfile.role === 'admin' &&
            callerProfile.household_id === targetProfile.household_id;

        if (!isOwner && !isHouseholdAdmin) {
            return new Response(
                JSON.stringify({ error: 'Forbidden' }),
                { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // Hash with bcrypt cost factor 12
        const hash = await bcrypt.hash(pin, 12);

        const { error: updateError } = await supabase
            .from('profiles')
            .update({ pin_hash: hash })
            .eq('id', profileId);

        if (updateError) {
            return new Response(
                JSON.stringify({ error: 'Failed to set PIN' }),
                { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        return new Response(
            JSON.stringify({ success: true }),
            { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    } catch (err) {
        return new Response(
            JSON.stringify({ error: 'Internal server error' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    }
});
