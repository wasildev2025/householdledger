-- =============================================
-- Row-Level Security Policies
-- Household Ledger
-- Migration: 001_rls_policies
-- =============================================

-- Helper: Get current user's household_id
CREATE OR REPLACE FUNCTION get_my_household_id()
RETURNS uuid AS $$
  SELECT household_id FROM profiles WHERE id = auth.uid()
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Helper: Check if current user is admin of their household
CREATE OR REPLACE FUNCTION is_household_admin()
RETURNS boolean AS $$
  SELECT EXISTS (
    SELECT 1 FROM profiles
    WHERE id = auth.uid() AND role = 'admin'
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- =============================================
-- PROFILES
-- =============================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_select_own"
  ON profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "profiles_update_own"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

-- Allow insert during signup (auth trigger or client)
CREATE POLICY "profiles_insert_own"
  ON profiles FOR INSERT
  WITH CHECK (auth.uid() = id);

-- =============================================
-- HOUSEHOLDS
-- =============================================
ALTER TABLE households ENABLE ROW LEVEL SECURITY;

CREATE POLICY "households_select_member"
  ON households FOR SELECT
  USING (
    id IN (SELECT household_id FROM profiles WHERE id = auth.uid())
  );

CREATE POLICY "households_insert_owner"
  ON households FOR INSERT
  WITH CHECK (owner_id = auth.uid());

CREATE POLICY "households_update_owner"
  ON households FOR UPDATE
  USING (owner_id = auth.uid());

-- =============================================
-- SERVANTS (STAFF)
-- =============================================
ALTER TABLE servants ENABLE ROW LEVEL SECURITY;

CREATE POLICY "servants_select_household"
  ON servants FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "servants_insert_admin"
  ON servants FOR INSERT
  WITH CHECK (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "servants_update_admin"
  ON servants FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "servants_delete_admin"
  ON servants FOR DELETE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- MEMBERS (FAMILY)
-- =============================================
ALTER TABLE members ENABLE ROW LEVEL SECURITY;

CREATE POLICY "members_select_household"
  ON members FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "members_insert_admin"
  ON members FOR INSERT
  WITH CHECK (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "members_update_admin"
  ON members FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "members_delete_admin"
  ON members FOR DELETE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- CATEGORIES
-- =============================================
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "categories_select_household"
  ON categories FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "categories_insert_admin"
  ON categories FOR INSERT
  WITH CHECK (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "categories_update_admin"
  ON categories FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "categories_delete_admin"
  ON categories FOR DELETE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- TRANSACTIONS
-- =============================================
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "transactions_select_household"
  ON transactions FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "transactions_insert_household"
  ON transactions FOR INSERT
  WITH CHECK (household_id = get_my_household_id());

CREATE POLICY "transactions_update_admin"
  ON transactions FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "transactions_delete_admin"
  ON transactions FOR DELETE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- REQUESTS (MONEY REQUESTS)
-- =============================================
ALTER TABLE requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "requests_select_household"
  ON requests FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "requests_insert_household"
  ON requests FOR INSERT
  WITH CHECK (household_id = get_my_household_id());

-- Only admins can approve/reject requests
CREATE POLICY "requests_update_admin"
  ON requests FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- MESSAGES
-- =============================================
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "messages_select_household"
  ON messages FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "messages_insert_household"
  ON messages FOR INSERT
  WITH CHECK (
    household_id = get_my_household_id()
    AND sender_id = auth.uid()
  );

-- =============================================
-- DAIRY LOGS
-- =============================================
ALTER TABLE dairy_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "dairy_logs_select_household"
  ON dairy_logs FOR SELECT
  USING (household_id = get_my_household_id());

CREATE POLICY "dairy_logs_insert_household"
  ON dairy_logs FOR INSERT
  WITH CHECK (household_id = get_my_household_id());

CREATE POLICY "dairy_logs_update_admin"
  ON dairy_logs FOR UPDATE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

CREATE POLICY "dairy_logs_delete_admin"
  ON dairy_logs FOR DELETE
  USING (
    household_id = get_my_household_id()
    AND is_household_admin()
  );

-- =============================================
-- RECURRING TRANSACTIONS (only if table exists)
-- =============================================
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'recurring_transactions'
  ) THEN
    ALTER TABLE recurring_transactions ENABLE ROW LEVEL SECURITY;

    IF NOT EXISTS (
      SELECT 1 FROM pg_policies WHERE tablename = 'recurring_transactions' AND policyname = 'recurring_select_household'
    ) THEN
      CREATE POLICY "recurring_select_household"
        ON recurring_transactions FOR SELECT
        USING (household_id = get_my_household_id());
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM pg_policies WHERE tablename = 'recurring_transactions' AND policyname = 'recurring_insert_admin'
    ) THEN
      CREATE POLICY "recurring_insert_admin"
        ON recurring_transactions FOR INSERT
        WITH CHECK (household_id = get_my_household_id() AND is_household_admin());
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM pg_policies WHERE tablename = 'recurring_transactions' AND policyname = 'recurring_update_admin'
    ) THEN
      CREATE POLICY "recurring_update_admin"
        ON recurring_transactions FOR UPDATE
        USING (household_id = get_my_household_id() AND is_household_admin());
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM pg_policies WHERE tablename = 'recurring_transactions' AND policyname = 'recurring_delete_admin'
    ) THEN
      CREATE POLICY "recurring_delete_admin"
        ON recurring_transactions FOR DELETE
        USING (household_id = get_my_household_id() AND is_household_admin());
    END IF;
  END IF;
END $$;

