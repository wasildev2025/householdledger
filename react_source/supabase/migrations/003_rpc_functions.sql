-- =============================================
-- Server-side Aggregation RPCs
-- Household Ledger
-- Migration: 003_rpc_functions
-- =============================================

-- Monthly summary: replaces client-side balance calculations
CREATE OR REPLACE FUNCTION get_monthly_summary(
  p_household_id UUID,
  p_month DATE DEFAULT CURRENT_DATE
)
RETURNS JSON AS $$
DECLARE
  result JSON;
  month_start TIMESTAMPTZ;
  month_end TIMESTAMPTZ;
BEGIN
  month_start := DATE_TRUNC('month', p_month);
  month_end := DATE_TRUNC('month', p_month) + INTERVAL '1 month';

  SELECT json_build_object(
    'total_income', COALESCE(SUM(CASE WHEN type = 'income' THEN amount END), 0),
    'total_expense', COALESCE(SUM(CASE WHEN type = 'expense' THEN amount END), 0),
    'total_transfers', COALESCE(SUM(CASE WHEN type = 'transfer' THEN amount END), 0),
    'personal_expenses', COALESCE(SUM(CASE WHEN type = 'expense' AND servant_id IS NULL AND member_id IS NULL THEN amount END), 0),
    'staff_expenses', COALESCE(SUM(CASE WHEN type = 'expense' AND servant_id IS NOT NULL THEN amount END), 0),
    'member_expenses', COALESCE(SUM(CASE WHEN type = 'expense' AND member_id IS NOT NULL THEN amount END), 0),
    'transaction_count', COUNT(*)
  ) INTO result
  FROM transactions
  WHERE household_id = p_household_id
    AND date >= month_start
    AND date < month_end;

  RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Per-staff/member wallet summary for a given month
CREATE OR REPLACE FUNCTION get_wallet_summary(
  p_household_id UUID,
  p_person_id UUID,
  p_person_type TEXT, -- 'servant' or 'member'
  p_month DATE DEFAULT CURRENT_DATE
)
RETURNS JSON AS $$
DECLARE
  result JSON;
  month_start TIMESTAMPTZ;
  month_end TIMESTAMPTZ;
BEGIN
  month_start := DATE_TRUNC('month', p_month);
  month_end := DATE_TRUNC('month', p_month) + INTERVAL '1 month';

  IF p_person_type = 'servant' THEN
    SELECT json_build_object(
      'allocation', COALESCE(SUM(CASE WHEN type = 'transfer' AND servant_id = p_person_id THEN amount END), 0),
      'utilization', COALESCE(SUM(CASE WHEN type = 'expense' AND servant_id = p_person_id THEN amount END), 0)
    ) INTO result
    FROM transactions
    WHERE household_id = p_household_id
      AND date >= month_start
      AND date < month_end;
  ELSE
    SELECT json_build_object(
      'allocation', COALESCE(SUM(CASE WHEN type = 'transfer' AND member_id = p_person_id THEN amount END), 0),
      'utilization', COALESCE(SUM(CASE WHEN type = 'expense' AND member_id = p_person_id THEN amount END), 0)
    ) INTO result
    FROM transactions
    WHERE household_id = p_household_id
      AND date >= month_start
      AND date < month_end;
  END IF;

  RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- AI insights cache table
CREATE TABLE IF NOT EXISTS ai_insights_cache (
  household_id UUID PRIMARY KEY REFERENCES households(id) ON DELETE CASCADE,
  insight JSONB NOT NULL,
  generated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE ai_insights_cache ENABLE ROW LEVEL SECURITY;

CREATE POLICY "ai_cache_select_household"
  ON ai_insights_cache FOR SELECT
  USING (household_id = get_my_household_id());

-- PIN hash column on profiles (for server-side verification)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS pin_hash TEXT;
