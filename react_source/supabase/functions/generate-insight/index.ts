/**
 * Supabase Edge Function: generate-insight
 *
 * Generates an AI spending insight for a household using Gemini.
 * Caches results for 24 hours per household to minimize API costs.
 *
 * Cost optimization: ~5 calls/user/day → 1 call/household/day = ~80% reduction
 *
 * Deploy: supabase functions deploy generate-insight
 */

import { serve } from 'https://deno.land/std@0.177.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';
const CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

serve(async (req) => {
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders });
    }

    try {
        const { householdId } = await req.json();

        if (!householdId) {
            return new Response(
                JSON.stringify({ error: 'householdId is required' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        const supabase = createClient(
            Deno.env.get('SUPABASE_URL')!,
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
        );

        // --- Check cache ---
        const cutoff = new Date(Date.now() - CACHE_TTL_MS).toISOString();
        const { data: cached } = await supabase
            .from('ai_insights_cache')
            .select('insight')
            .eq('household_id', householdId)
            .gte('generated_at', cutoff)
            .single();

        if (cached) {
            return new Response(
                JSON.stringify(cached.insight),
                { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // --- Get aggregated data (NOT raw transactions — privacy + efficiency) ---
        const { data: summary } = await supabase.rpc('get_monthly_summary', {
            p_household_id: householdId,
        });

        if (!summary) {
            return new Response(
                JSON.stringify({ title: 'No Data', insight: 'No transactions this month.', type: 'tip' }),
                { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        // --- Call Gemini ---
        const geminiKey = Deno.env.get('GEMINI_API_KEY');
        if (!geminiKey) {
            return new Response(
                JSON.stringify({ error: 'AI service not configured' }),
                { status: 503, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        const prompt = `You are a household financial advisor. Based on this monthly spending summary, provide ONE concise insight (max 2 sentences).

Summary:
- Total Income: ${summary.total_income}
- Total Expenses: ${summary.total_expense}
- Staff Transfers: ${summary.total_transfers}
- Personal Expenses: ${summary.personal_expenses}
- Transaction Count: ${summary.transaction_count}

Return ONLY valid JSON: {"title": "short title", "insight": "the insight", "type": "tip|warning|positive"}`;

        const geminiRes = await fetch(`${GEMINI_API_URL}?key=${geminiKey}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: { temperature: 0.7, maxOutputTokens: 200 },
            }),
        });

        if (!geminiRes.ok) {
            return new Response(
                JSON.stringify({ error: 'AI service unavailable' }),
                { status: 503, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            );
        }

        const geminiData = await geminiRes.json();
        const rawText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text ?? '';

        // Parse JSON from response (handle markdown code blocks)
        let insight;
        try {
            const cleanedText = rawText.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
            insight = JSON.parse(cleanedText);
        } catch {
            insight = { title: 'Financial Insight', insight: rawText.slice(0, 200), type: 'tip' };
        }

        // --- Cache the result ---
        await supabase.from('ai_insights_cache').upsert({
            household_id: householdId,
            insight,
            generated_at: new Date().toISOString(),
        });

        return new Response(
            JSON.stringify(insight),
            { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    } catch (err) {
        return new Response(
            JSON.stringify({ error: 'Internal server error' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    }
});
