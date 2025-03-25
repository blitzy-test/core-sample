-- Migration: V20240401_003__payment_indexes.sql
-- Description: Creates optimized indexes for the payment module tables
-- Author: Development Team

-- Begin transaction for atomic execution
BEGIN;

-- Version tracking update
INSERT INTO schema_version (version, description, applied_at, applied_by)
VALUES ('V20240401_003', 'Payment indexes creation', NOW(), current_user);

-- Payment Transaction indexes - optimized for complex filtering and sorting
-- Composite index for organization and account filtering with status and date range
CREATE INDEX idx_pt_org_account_status_created ON payment_transaction(organization_id, account_id, status, created_at);

-- Composite index for financial reporting with status and amount filtering
CREATE INDEX idx_pt_status_amount_currency ON payment_transaction(status, amount, currency);

-- Composite index for merchant-specific transaction reporting with date filtering
CREATE INDEX idx_pt_merchant_created_at ON payment_transaction(merchant_id, created_at);

-- Composite index for transaction reference lookups
CREATE INDEX idx_pt_transaction_reference ON payment_transaction(transaction_reference);

-- Composite index for status and updated_at for recent status changes
CREATE INDEX idx_pt_status_updated_at ON payment_transaction(status, updated_at);

-- Payment Data indexes - optimized for payment method queries
-- Composite index for payment method and transaction lookups
CREATE INDEX idx_pd_payment_method_transaction ON payment_data(payment_method_id, transaction_id);

-- GIN index for JSON content searching in payment details
CREATE INDEX idx_pd_payment_details ON payment_data USING GIN (payment_details);

-- Index for expiration date queries
CREATE INDEX idx_pd_expiration ON payment_data(expiration);

-- GIN index for billing data JSON content searching
CREATE INDEX idx_pd_billing_data ON payment_data USING GIN (billing_data);

-- Payment Fees indexes - optimized for fee reporting and analysis
-- Composite index for fee type and amount reporting
CREATE INDEX idx_pf_fee_type_amount ON payment_fees(fee_type, amount, currency);

-- Composite index for fee creation date analysis
CREATE INDEX idx_pf_created_at ON payment_fees(created_at);

-- Composite index for fee reference lookups
CREATE INDEX idx_pf_fee_reference ON payment_fees(fee_reference);

-- Payment Events indexes - optimized for timeline and audit trail queries
-- Composite index for transaction timeline with event type and timestamp
CREATE INDEX idx_pe_transaction_event_type_created ON payment_events(transaction_id, event_type, created_at);

-- Composite index for status transition analysis
CREATE INDEX idx_pe_previous_new_status ON payment_events(previous_status, new_status);

-- Composite index for user activity auditing
CREATE INDEX idx_pe_created_by_created_at ON payment_events(created_by, created_at);

-- GIN index for JSON content searching in event data
CREATE INDEX idx_pe_event_data ON payment_events USING GIN (event_data);

-- Commit transaction
COMMIT;