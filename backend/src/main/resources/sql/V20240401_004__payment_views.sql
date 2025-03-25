-- Migration: V20240401_004__payment_views.sql
-- Description: Creates database views for common payment reporting scenarios
-- Author: Development Team

-- Begin transaction
BEGIN;

-- Version tracking update
INSERT INTO schema_version (version, description, applied_at, applied_by)
VALUES ('V20240401_004', 'Payment views creation', NOW(), current_user);

-- =============================================
-- Transaction Summary View
-- Aggregates payment transactions by status with count and amount totals
-- Used for dashboard reporting and status distribution analysis
-- =============================================
CREATE OR REPLACE VIEW payment_transaction_summary AS
SELECT 
    organization_id,
    account_id,
    status,
    currency,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount,
    MIN(created_at) AS earliest_transaction,
    MAX(created_at) AS latest_transaction,
    AVG(amount) AS average_amount
FROM 
    payment_transaction
GROUP BY 
    organization_id, account_id, status, currency;

-- =============================================
-- Merchant Transaction View
-- Provides merchant-specific transaction reporting with status distribution
-- Used for merchant performance analysis and settlement reporting
-- =============================================
CREATE OR REPLACE VIEW merchant_transaction_summary AS
SELECT 
    organization_id,
    account_id,
    merchant_id,
    payment_type,
    status,
    currency,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount,
    MIN(created_at) AS earliest_transaction,
    MAX(created_at) AS latest_transaction,
    EXTRACT(EPOCH FROM (MAX(created_at) - MIN(created_at)))/86400 AS date_range_days,
    SUM(amount) / NULLIF(EXTRACT(EPOCH FROM (MAX(created_at) - MIN(created_at)))/86400, 0) AS daily_average_amount
FROM 
    payment_transaction
GROUP BY 
    organization_id, account_id, merchant_id, payment_type, status, currency;

-- =============================================
-- Payment Timeline View
-- Provides chronological view of payment events with transaction context
-- Used for transaction history and audit trail reporting
-- =============================================
CREATE OR REPLACE VIEW payment_timeline_view AS
SELECT 
    pe.event_id,
    pe.transaction_id,
    pt.organization_id,
    pt.account_id,
    pt.merchant_id,
    pe.event_type,
    pe.previous_status,
    pe.new_status,
    pe.created_at AS event_timestamp,
    pe.created_by AS event_actor,
    pt.amount,
    pt.currency,
    pt.payment_type,
    pt.created_at AS transaction_created_at,
    pt.transaction_reference,
    pe.correlation_id,
    pe.event_data
FROM 
    payment_event pe
JOIN 
    payment_transaction pt ON pe.transaction_id = pt.transaction_id
ORDER BY 
    pe.created_at DESC;

-- =============================================
-- Fee Analysis View
-- Aggregates fee information for financial reporting and reconciliation
-- Used for fee tracking, revenue analysis, and financial reporting
-- =============================================
CREATE OR REPLACE VIEW payment_fee_analysis AS
SELECT 
    pf.fee_type,
    pt.organization_id,
    pt.account_id,
    pt.merchant_id,
    pt.payment_type,
    pt.status,
    pf.currency,
    COUNT(*) AS fee_count,
    SUM(pf.amount) AS total_fee_amount,
    AVG(pf.amount) AS average_fee_amount,
    MIN(pf.created_at) AS earliest_fee,
    MAX(pf.created_at) AS latest_fee,
    SUM(pt.amount) AS total_transaction_amount,
    CASE 
        WHEN SUM(pt.amount) = 0 THEN NULL 
        ELSE (SUM(pf.amount) / SUM(pt.amount)) * 100 
    END AS fee_percentage
FROM 
    payment_fee pf
JOIN 
    payment_transaction pt ON pf.transaction_id = pt.transaction_id
GROUP BY 
    pf.fee_type, pt.organization_id, pt.account_id, pt.merchant_id, pt.payment_type, pt.status, pf.currency;

-- =============================================
-- Transaction Status Transition View
-- Analyzes payment status transitions for workflow optimization
-- Used for process analysis and bottleneck identification
-- =============================================
CREATE OR REPLACE VIEW payment_status_transitions AS
WITH status_changes AS (
    SELECT 
        transaction_id,
        previous_status,
        new_status,
        created_at AS transition_time,
        LEAD(created_at) OVER (PARTITION BY transaction_id ORDER BY created_at) AS next_transition_time
    FROM 
        payment_event
    WHERE 
        previous_status IS NOT NULL AND new_status IS NOT NULL
)
SELECT 
    pt.organization_id,
    pt.account_id,
    pt.merchant_id,
    sc.previous_status,
    sc.new_status,
    COUNT(*) AS transition_count,
    AVG(EXTRACT(EPOCH FROM (sc.next_transition_time - sc.transition_time))) AS avg_time_in_status_seconds,
    MIN(EXTRACT(EPOCH FROM (sc.next_transition_time - sc.transition_time))) AS min_time_in_status_seconds,
    MAX(EXTRACT(EPOCH FROM (sc.next_transition_time - sc.transition_time))) AS max_time_in_status_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (sc.next_transition_time - sc.transition_time))) AS median_time_in_status_seconds,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (sc.next_transition_time - sc.transition_time))) AS p95_time_in_status_seconds
FROM 
    status_changes sc
JOIN 
    payment_transaction pt ON sc.transaction_id = pt.transaction_id
WHERE 
    sc.next_transition_time IS NOT NULL
GROUP BY 
    pt.organization_id, pt.account_id, pt.merchant_id, sc.previous_status, sc.new_status;

-- =============================================
-- Daily Transaction Summary View
-- Provides daily aggregation of transaction metrics
-- Used for time-series analysis and trend reporting
-- =============================================
CREATE OR REPLACE VIEW daily_transaction_summary AS
SELECT 
    organization_id,
    account_id,
    DATE_TRUNC('day', created_at) AS transaction_date,
    status,
    currency,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount,
    AVG(amount) AS average_amount,
    MIN(amount) AS min_amount,
    MAX(amount) AS max_amount,
    COUNT(DISTINCT merchant_id) AS merchant_count
FROM 
    payment_transaction
GROUP BY 
    organization_id, account_id, DATE_TRUNC('day', created_at), status, currency
ORDER BY 
    transaction_date DESC;

-- Commit transaction
COMMIT;