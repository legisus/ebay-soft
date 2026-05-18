-- Bootstrap script run by the postgres container on first start. Creates each service's
-- DB role + schema; Flyway then runs migrations under that role at service startup.

\set ON_ERROR_STOP on

-- Helper to idempotently create a role.
CREATE OR REPLACE FUNCTION ensure_role(role_name TEXT, role_password TEXT) RETURNS VOID AS $$
DECLARE
  exists_already BOOLEAN;
BEGIN
  SELECT TRUE INTO exists_already FROM pg_roles WHERE rolname = role_name;
  IF NOT FOUND THEN
    EXECUTE format('CREATE ROLE %I WITH LOGIN PASSWORD %L', role_name, role_password);
  END IF;
END
$$ LANGUAGE plpgsql;

SELECT ensure_role('auth_api',       'auth_api');
SELECT ensure_role('ebay_conn_api',  'ebay_conn_api');
SELECT ensure_role('sync_api',       'sync_api');
SELECT ensure_role('accounting_api', 'accounting_api');
SELECT ensure_role('inventory_api',  'inventory_api');
SELECT ensure_role('repricer_api',   'repricer_api');
SELECT ensure_role('analytics_api',  'analytics_api');
SELECT ensure_role('notif_api',      'notif_api');
SELECT ensure_role('billing_api',    'billing_api');
SELECT ensure_role('admin_api',      'admin_api');

-- Each service's schema is created by its own Flyway run (spring.flyway.create-schemas=true).
