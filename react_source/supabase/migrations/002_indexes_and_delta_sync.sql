-- =============================================
-- Database Indexes
-- Household Ledger
-- Migration: 002_indexes
-- =============================================

-- Core lookup indexes for household-scoped queries
CREATE INDEX IF NOT EXISTS idx_transactions_household_date
  ON transactions (household_id, date DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_servant
  ON transactions (servant_id) WHERE servant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_member
  ON transactions (member_id) WHERE member_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_type
  ON transactions (household_id, type);

CREATE INDEX IF NOT EXISTS idx_messages_household_created
  ON messages (household_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_requests_household_status
  ON requests (household_id, status);

CREATE INDEX IF NOT EXISTS idx_servants_household
  ON servants (household_id);

CREATE INDEX IF NOT EXISTS idx_members_household
  ON members (household_id);

CREATE INDEX IF NOT EXISTS idx_dairy_logs_household_date
  ON dairy_logs (household_id, date DESC);

CREATE INDEX IF NOT EXISTS idx_profiles_household
  ON profiles (household_id);

-- Invite code lookups (unique for active codes)
CREATE UNIQUE INDEX IF NOT EXISTS idx_servants_invite_code
  ON servants (invite_code) WHERE invite_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_members_invite_code
  ON members (invite_code) WHERE invite_code IS NOT NULL;

-- Recurring transactions lookup (only if table exists)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'recurring_transactions'
  ) THEN
    CREATE INDEX IF NOT EXISTS idx_recurring_household
      ON recurring_transactions (household_id, active);
  END IF;
END $$;

-- =============================================
-- Delta Sync Support: updated_at columns + triggers
-- =============================================

-- Auto-update timestamp function
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add updated_at to all entity tables (only ones that exist)
DO $$
DECLARE
  tbl TEXT;
BEGIN
  FOR tbl IN
    SELECT t FROM unnest(ARRAY[
      'transactions', 'servants', 'members', 'categories',
      'requests', 'messages', 'dairy_logs', 'recurring_transactions',
      'households', 'profiles'
    ]) AS t
    WHERE EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = 'public' AND table_name = t
    )
  LOOP
    EXECUTE format(
      'ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW()',
      tbl
    );
    -- Create trigger (drop first to be idempotent)
    EXECUTE format(
      'DROP TRIGGER IF EXISTS set_updated_at ON %I',
      tbl
    );
    EXECUTE format(
      'CREATE TRIGGER set_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_timestamp()',
      tbl
    );
  END LOOP;
END;
$$;
