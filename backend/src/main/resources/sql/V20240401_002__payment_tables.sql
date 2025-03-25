-- Migration: V20240401_002__payment_tables.sql
-- Description: Creates the core payment module tables
-- Author: Development Team

-- Begin transaction for atomic execution
BEGIN;

-- Version tracking update
INSERT INTO schema_version (version, description, applied_at, applied_by)
VALUES ('V20240401_002', 'Payment tables creation', NOW(), current_user);

-- Create Payment Transactions table
CREATE TABLE payment_transaction (
    transaction_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    account_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    payment_type VARCHAR(32) NOT NULL,
    transaction_reference VARCHAR(128),
    description VARCHAR(255)
);

-- Create Payment Data table for payment method information
CREATE TABLE payment_data (
    payment_data_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    payment_method_id VARCHAR(64) NOT NULL,
    payment_token VARCHAR(128),
    payment_details JSONB,
    created_at TIMESTAMP NOT NULL,
    expiration DATE,
    billing_data JSONB,
    CONSTRAINT fk_payment_data_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES payment_transaction(transaction_id)
        ON DELETE CASCADE
);

-- Create Payment Fees table for fee tracking
CREATE TABLE payment_fees (
    fee_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    fee_type VARCHAR(32) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    description VARCHAR(255),
    fee_reference VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_fees_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES payment_transaction(transaction_id)
        ON DELETE CASCADE
);

-- Create Payment Events table for audit trail
CREATE TABLE payment_events (
    event_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    event_data JSONB,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    correlation_id UUID,
    CONSTRAINT fk_payment_events_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES payment_transaction(transaction_id)
        ON DELETE CASCADE
);

-- Create indexes for efficient querying
-- Payment Transaction indexes
CREATE INDEX idx_pt_org_account ON payment_transaction(organization_id, account_id);
CREATE INDEX idx_pt_status ON payment_transaction(status);
CREATE INDEX idx_pt_created_at ON payment_transaction(created_at);
CREATE INDEX idx_pt_updated_at ON payment_transaction(updated_at);
CREATE INDEX idx_pt_amount ON payment_transaction(amount, currency);
CREATE INDEX idx_pt_merchant ON payment_transaction(merchant_id);
CREATE INDEX idx_pt_payment_type ON payment_transaction(payment_type);
CREATE INDEX idx_pt_org_account_status_created ON payment_transaction(organization_id, account_id, status, created_at);
CREATE INDEX idx_pt_status_amount_currency ON payment_transaction(status, amount, currency);
CREATE INDEX idx_pt_merchant_created_at ON payment_transaction(merchant_id, created_at);

-- Payment Data indexes
CREATE INDEX idx_pd_transaction ON payment_data(transaction_id);
CREATE INDEX idx_pd_payment_method ON payment_data(payment_method_id);
CREATE INDEX idx_pd_payment_method_transaction ON payment_data(payment_method_id, transaction_id);
CREATE INDEX idx_pd_payment_details ON payment_data USING GIN (payment_details);

-- Payment Fees indexes
CREATE INDEX idx_pf_transaction ON payment_fees(transaction_id);
CREATE INDEX idx_pf_fee_type ON payment_fees(fee_type);
CREATE INDEX idx_pf_amount ON payment_fees(amount, currency);

-- Payment Events indexes
CREATE INDEX idx_pe_transaction ON payment_events(transaction_id);
CREATE INDEX idx_pe_event_type ON payment_events(event_type);
CREATE INDEX idx_pe_created_at ON payment_events(created_at);
CREATE INDEX idx_pe_correlation ON payment_events(correlation_id);
CREATE INDEX idx_pe_transaction_event_type_created ON payment_events(transaction_id, event_type, created_at);
CREATE INDEX idx_pe_event_data ON payment_events USING GIN (event_data);

-- Commit transaction
COMMIT;