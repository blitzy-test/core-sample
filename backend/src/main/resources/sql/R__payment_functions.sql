-- =============================================
-- R__payment_functions.sql
-- =============================================
-- Description: Repeatable migration script that creates or replaces database functions
--              for payment processing workflows. These functions implement complex payment
--              business logic at the database level, ensuring consistent transaction
--              processing across all application components.
-- Author: Development Team
-- =============================================

-- Begin transaction for atomic execution
BEGIN;

-- =============================================
-- Payment Status Transition Functions
-- =============================================

-- Function to validate payment status transitions
CREATE OR REPLACE FUNCTION payment_validate_status_transition(
    p_current_status VARCHAR(32),
    p_new_status VARCHAR(32)
) RETURNS BOOLEAN AS $$
DECLARE
    is_valid BOOLEAN := FALSE;
BEGIN
    -- Define valid status transitions
    CASE p_current_status
        WHEN 'CREATED' THEN
            is_valid := p_new_status IN ('PROCESSING', 'AUTHORIZED', 'FAILED', 'CANCELLED');
        WHEN 'PROCESSING' THEN
            is_valid := p_new_status IN ('AUTHORIZED', 'FAILED', 'CANCELLED');
        WHEN 'AUTHORIZED' THEN
            is_valid := p_new_status IN ('CAPTURED', 'PARTIALLY_CAPTURED', 'CANCELLED', 'EXPIRED');
        WHEN 'PARTIALLY_CAPTURED' THEN
            is_valid := p_new_status IN ('CAPTURED', 'PARTIALLY_REFUNDED', 'REFUNDED');
        WHEN 'CAPTURED' THEN
            is_valid := p_new_status IN ('PARTIALLY_REFUNDED', 'REFUNDED', 'SETTLED');
        WHEN 'PARTIALLY_REFUNDED' THEN
            is_valid := p_new_status IN ('REFUNDED', 'SETTLED');
        WHEN 'REFUNDED' THEN
            is_valid := p_new_status IN ('SETTLED');
        WHEN 'SETTLED' THEN
            is_valid := FALSE; -- Terminal state, no further transitions
        WHEN 'FAILED' THEN
            is_valid := FALSE; -- Terminal state, no further transitions
        WHEN 'CANCELLED' THEN
            is_valid := FALSE; -- Terminal state, no further transitions
        WHEN 'EXPIRED' THEN
            is_valid := FALSE; -- Terminal state, no further transitions
        ELSE
            is_valid := FALSE; -- Unknown status, no valid transitions
    END CASE;
    
    RETURN is_valid;
END;
$$ LANGUAGE plpgsql;

-- Function to update payment transaction status with validation
CREATE OR REPLACE FUNCTION payment_update_transaction_status(
    p_transaction_id UUID,
    p_new_status VARCHAR(32),
    p_updated_by VARCHAR(128),
    p_correlation_id UUID DEFAULT NULL,
    p_event_data JSONB DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_current_status VARCHAR(32);
    v_is_valid BOOLEAN;
    v_event_id UUID;
BEGIN
    -- Get current status
    SELECT status INTO v_current_status
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Validate status transition
    v_is_valid := payment_validate_status_transition(v_current_status, p_new_status);
    
    IF NOT v_is_valid THEN
        RAISE EXCEPTION 'Invalid status transition from % to %', v_current_status, p_new_status;
    END IF;
    
    -- Update transaction status
    UPDATE payment_transaction
    SET 
        status = p_new_status,
        updated_at = NOW()
    WHERE transaction_id = p_transaction_id;
    
    -- Record status change event
    INSERT INTO payment_event(
        event_id,
        transaction_id,
        event_type,
        previous_status,
        new_status,
        event_data,
        created_at,
        created_by,
        correlation_id
    ) VALUES (
        gen_random_uuid(),
        p_transaction_id,
        'STATUS_CHANGE',
        v_current_status,
        p_new_status,
        COALESCE(p_event_data, '{}'::jsonb),
        NOW(),
        p_updated_by,
        p_correlation_id
    ) RETURNING event_id INTO v_event_id;
    
    RETURN TRUE;
EXCEPTION
    WHEN OTHERS THEN
        -- Log error event
        INSERT INTO payment_event(
            event_id,
            transaction_id,
            event_type,
            previous_status,
            new_status,
            event_data,
            created_at,
            created_by,
            correlation_id
        ) VALUES (
            gen_random_uuid(),
            p_transaction_id,
            'STATUS_CHANGE_ERROR',
            v_current_status,
            p_new_status,
            jsonb_build_object(
                'error', SQLERRM,
                'details', p_event_data
            ),
            NOW(),
            p_updated_by,
            p_correlation_id
        );
        
        RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Payment Amount Calculation Functions
-- =============================================

-- Function to calculate available amount for capture
CREATE OR REPLACE FUNCTION payment_calculate_available_capture_amount(
    p_transaction_id UUID
) RETURNS DECIMAL(19,4) AS $$
DECLARE
    v_total_amount DECIMAL(19,4);
    v_captured_amount DECIMAL(19,4);
    v_available_amount DECIMAL(19,4);
BEGIN
    -- Get total transaction amount
    SELECT amount INTO v_total_amount
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Get already captured amount
    SELECT COALESCE(SUM(amount), 0) INTO v_captured_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'CAPTURE'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Calculate available amount
    v_available_amount := v_total_amount - v_captured_amount;
    
    -- Ensure non-negative result
    RETURN GREATEST(0, v_available_amount);
END;
$$ LANGUAGE plpgsql;

-- Function to calculate available amount for refund
CREATE OR REPLACE FUNCTION payment_calculate_available_refund_amount(
    p_transaction_id UUID
) RETURNS DECIMAL(19,4) AS $$
DECLARE
    v_captured_amount DECIMAL(19,4);
    v_refunded_amount DECIMAL(19,4);
    v_available_amount DECIMAL(19,4);
BEGIN
    -- Get total captured amount
    SELECT COALESCE(SUM(amount), 0) INTO v_captured_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'CAPTURE'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Get already refunded amount
    SELECT COALESCE(SUM(amount), 0) INTO v_refunded_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'REFUND'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Calculate available amount
    v_available_amount := v_captured_amount - v_refunded_amount;
    
    -- Ensure non-negative result
    RETURN GREATEST(0, v_available_amount);
END;
$$ LANGUAGE plpgsql;

-- Function to validate capture amount
CREATE OR REPLACE FUNCTION payment_validate_capture_amount(
    p_transaction_id UUID,
    p_capture_amount DECIMAL(19,4)
) RETURNS BOOLEAN AS $$
DECLARE
    v_available_amount DECIMAL(19,4);
BEGIN
    -- Get available amount for capture
    v_available_amount := payment_calculate_available_capture_amount(p_transaction_id);
    
    -- Validate capture amount
    RETURN p_capture_amount > 0 AND p_capture_amount <= v_available_amount;
END;
$$ LANGUAGE plpgsql;

-- Function to validate refund amount
CREATE OR REPLACE FUNCTION payment_validate_refund_amount(
    p_transaction_id UUID,
    p_refund_amount DECIMAL(19,4)
) RETURNS BOOLEAN AS $$
DECLARE
    v_available_amount DECIMAL(19,4);
BEGIN
    -- Get available amount for refund
    v_available_amount := payment_calculate_available_refund_amount(p_transaction_id);
    
    -- Validate refund amount
    RETURN p_refund_amount > 0 AND p_refund_amount <= v_available_amount;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Payment Audit Trail Functions
-- =============================================

-- Function to record payment capture event
CREATE OR REPLACE FUNCTION payment_record_capture_event(
    p_transaction_id UUID,
    p_amount DECIMAL(19,4),
    p_currency CHAR(3),
    p_created_by VARCHAR(128),
    p_correlation_id UUID DEFAULT NULL,
    p_event_data JSONB DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_event_id UUID;
    v_current_status VARCHAR(32);
    v_new_status VARCHAR(32);
    v_total_amount DECIMAL(19,4);
    v_captured_amount DECIMAL(19,4);
    v_event_data JSONB;
BEGIN
    -- Get current status and total amount
    SELECT status, amount INTO v_current_status, v_total_amount
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Validate transaction is in a capturable state
    IF v_current_status NOT IN ('AUTHORIZED', 'PARTIALLY_CAPTURED') THEN
        RAISE EXCEPTION 'Transaction is not in a capturable state. Current status: %', v_current_status;
    END IF;
    
    -- Validate capture amount
    IF NOT payment_validate_capture_amount(p_transaction_id, p_amount) THEN
        RAISE EXCEPTION 'Invalid capture amount: %. Available amount: %', 
            p_amount, payment_calculate_available_capture_amount(p_transaction_id);
    END IF;
    
    -- Calculate total captured amount including this capture
    SELECT COALESCE(SUM(amount), 0) + p_amount INTO v_captured_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'CAPTURE'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Determine new status based on captured amount
    IF v_captured_amount >= v_total_amount THEN
        v_new_status := 'CAPTURED';
    ELSE
        v_new_status := 'PARTIALLY_CAPTURED';
    END IF;
    
    -- Prepare event data
    v_event_data := COALESCE(p_event_data, '{}'::jsonb) || jsonb_build_object(
        'amount', p_amount,
        'currency', p_currency,
        'status', 'SUCCESS',
        'captured_amount', v_captured_amount,
        'total_amount', v_total_amount
    );
    
    -- Record capture event
    INSERT INTO payment_event(
        event_id,
        transaction_id,
        event_type,
        previous_status,
        new_status,
        event_data,
        created_at,
        created_by,
        correlation_id
    ) VALUES (
        gen_random_uuid(),
        p_transaction_id,
        'CAPTURE',
        v_current_status,
        v_new_status,
        v_event_data,
        NOW(),
        p_created_by,
        p_correlation_id
    ) RETURNING event_id INTO v_event_id;
    
    -- Update transaction status
    UPDATE payment_transaction
    SET 
        status = v_new_status,
        updated_at = NOW()
    WHERE transaction_id = p_transaction_id;
    
    RETURN v_event_id;
EXCEPTION
    WHEN OTHERS THEN
        -- Log error event
        INSERT INTO payment_event(
            event_id,
            transaction_id,
            event_type,
            previous_status,
            new_status,
            event_data,
            created_at,
            created_by,
            correlation_id
        ) VALUES (
            gen_random_uuid(),
            p_transaction_id,
            'CAPTURE_ERROR',
            v_current_status,
            v_current_status, -- Status doesn't change on error
            jsonb_build_object(
                'error', SQLERRM,
                'amount', p_amount,
                'currency', p_currency,
                'status', 'FAILED',
                'details', COALESCE(p_event_data, '{}'::jsonb)
            ),
            NOW(),
            p_created_by,
            p_correlation_id
        );
        
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- Function to record payment refund event
CREATE OR REPLACE FUNCTION payment_record_refund_event(
    p_transaction_id UUID,
    p_amount DECIMAL(19,4),
    p_currency CHAR(3),
    p_created_by VARCHAR(128),
    p_correlation_id UUID DEFAULT NULL,
    p_event_data JSONB DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_event_id UUID;
    v_current_status VARCHAR(32);
    v_new_status VARCHAR(32);
    v_captured_amount DECIMAL(19,4);
    v_refunded_amount DECIMAL(19,4);
    v_event_data JSONB;
BEGIN
    -- Get current status
    SELECT status INTO v_current_status
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Validate transaction is in a refundable state
    IF v_current_status NOT IN ('CAPTURED', 'PARTIALLY_CAPTURED', 'PARTIALLY_REFUNDED') THEN
        RAISE EXCEPTION 'Transaction is not in a refundable state. Current status: %', v_current_status;
    END IF;
    
    -- Validate refund amount
    IF NOT payment_validate_refund_amount(p_transaction_id, p_amount) THEN
        RAISE EXCEPTION 'Invalid refund amount: %. Available amount: %', 
            p_amount, payment_calculate_available_refund_amount(p_transaction_id);
    END IF;
    
    -- Get total captured amount
    SELECT COALESCE(SUM(amount), 0) INTO v_captured_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'CAPTURE'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Calculate total refunded amount including this refund
    SELECT COALESCE(SUM(amount), 0) + p_amount INTO v_refunded_amount
    FROM payment_event
    WHERE transaction_id = p_transaction_id
    AND event_type = 'REFUND'
    AND event_data->>'status' = 'SUCCESS';
    
    -- Determine new status based on refunded amount
    IF v_refunded_amount >= v_captured_amount THEN
        v_new_status := 'REFUNDED';
    ELSE
        v_new_status := 'PARTIALLY_REFUNDED';
    END IF;
    
    -- Prepare event data
    v_event_data := COALESCE(p_event_data, '{}'::jsonb) || jsonb_build_object(
        'amount', p_amount,
        'currency', p_currency,
        'status', 'SUCCESS',
        'refunded_amount', v_refunded_amount,
        'captured_amount', v_captured_amount
    );
    
    -- Record refund event
    INSERT INTO payment_event(
        event_id,
        transaction_id,
        event_type,
        previous_status,
        new_status,
        event_data,
        created_at,
        created_by,
        correlation_id
    ) VALUES (
        gen_random_uuid(),
        p_transaction_id,
        'REFUND',
        v_current_status,
        v_new_status,
        v_event_data,
        NOW(),
        p_created_by,
        p_correlation_id
    ) RETURNING event_id INTO v_event_id;
    
    -- Update transaction status
    UPDATE payment_transaction
    SET 
        status = v_new_status,
        updated_at = NOW()
    WHERE transaction_id = p_transaction_id;
    
    RETURN v_event_id;
EXCEPTION
    WHEN OTHERS THEN
        -- Log error event
        INSERT INTO payment_event(
            event_id,
            transaction_id,
            event_type,
            previous_status,
            new_status,
            event_data,
            created_at,
            created_by,
            correlation_id
        ) VALUES (
            gen_random_uuid(),
            p_transaction_id,
            'REFUND_ERROR',
            v_current_status,
            v_current_status, -- Status doesn't change on error
            jsonb_build_object(
                'error', SQLERRM,
                'amount', p_amount,
                'currency', p_currency,
                'status', 'FAILED',
                'details', COALESCE(p_event_data, '{}'::jsonb)
            ),
            NOW(),
            p_created_by,
            p_correlation_id
        );
        
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- Function to record generic payment event
CREATE OR REPLACE FUNCTION payment_record_event(
    p_transaction_id UUID,
    p_event_type VARCHAR(32),
    p_created_by VARCHAR(128),
    p_correlation_id UUID DEFAULT NULL,
    p_event_data JSONB DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_event_id UUID;
    v_current_status VARCHAR(32);
BEGIN
    -- Get current status
    SELECT status INTO v_current_status
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Record event
    INSERT INTO payment_event(
        event_id,
        transaction_id,
        event_type,
        previous_status,
        new_status,
        event_data,
        created_at,
        created_by,
        correlation_id
    ) VALUES (
        gen_random_uuid(),
        p_transaction_id,
        p_event_type,
        v_current_status,
        v_current_status, -- Status doesn't change for generic events
        COALESCE(p_event_data, '{}'::jsonb),
        NOW(),
        p_created_by,
        p_correlation_id
    ) RETURNING event_id INTO v_event_id;
    
    RETURN v_event_id;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Payment Validation Functions
-- =============================================

-- Function to validate payment transaction data
CREATE OR REPLACE FUNCTION payment_validate_transaction(
    p_organization_id UUID,
    p_account_id UUID,
    p_amount DECIMAL(19,4),
    p_currency CHAR(3),
    p_merchant_id VARCHAR(64),
    p_payment_type VARCHAR(32)
) RETURNS JSONB AS $$
DECLARE
    v_errors JSONB := '{}'::jsonb;
    v_valid_currency BOOLEAN;
    v_valid_payment_type BOOLEAN;
BEGIN
    -- Validate amount
    IF p_amount <= 0 THEN
        v_errors := v_errors || jsonb_build_object('amount', 'Amount must be greater than zero');
    END IF;
    
    -- Validate currency (ISO 4217 code)
    SELECT EXISTS (
        SELECT 1 FROM (
            VALUES ('USD'), ('EUR'), ('GBP'), ('CAD'), ('AUD'), ('JPY'), ('CNY'), ('INR'), ('BRL'), ('MXN')
            -- Add more currencies as needed
        ) AS valid_currencies(code)
        WHERE code = p_currency
    ) INTO v_valid_currency;
    
    IF NOT v_valid_currency THEN
        v_errors := v_errors || jsonb_build_object('currency', 'Invalid currency code');
    END IF;
    
    -- Validate payment type
    SELECT EXISTS (
        SELECT 1 FROM (
            VALUES ('CREDIT_CARD'), ('DEBIT_CARD'), ('BANK_TRANSFER'), ('WALLET'), ('CRYPTO')
            -- Add more payment types as needed
        ) AS valid_payment_types(type)
        WHERE type = p_payment_type
    ) INTO v_valid_payment_type;
    
    IF NOT v_valid_payment_type THEN
        v_errors := v_errors || jsonb_build_object('payment_type', 'Invalid payment type');
    END IF;
    
    -- Validate organization and account existence (placeholder - would need actual tables)
    -- In a real implementation, you would check if these IDs exist in their respective tables
    
    -- Validate merchant ID format (simple check for non-empty)
    IF p_merchant_id IS NULL OR LENGTH(TRIM(p_merchant_id)) = 0 THEN
        v_errors := v_errors || jsonb_build_object('merchant_id', 'Merchant ID cannot be empty');
    END IF;
    
    -- Return validation results
    RETURN jsonb_build_object(
        'is_valid', jsonb_object_length(v_errors) = 0,
        'errors', v_errors
    );
END;
$$ LANGUAGE plpgsql;

-- Function to check if a transaction can be settled
CREATE OR REPLACE FUNCTION payment_can_settle_transaction(
    p_transaction_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_current_status VARCHAR(32);
    v_can_settle BOOLEAN := FALSE;
BEGIN
    -- Get current status
    SELECT status INTO v_current_status
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Check if transaction can be settled
    IF v_current_status IN ('CAPTURED', 'PARTIALLY_CAPTURED', 'PARTIALLY_REFUNDED', 'REFUNDED') THEN
        v_can_settle := TRUE;
    END IF;
    
    RETURN v_can_settle;
END;
$$ LANGUAGE plpgsql;

-- Function to check if a transaction can be expired
CREATE OR REPLACE FUNCTION payment_can_expire_transaction(
    p_transaction_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_current_status VARCHAR(32);
    v_created_at TIMESTAMP;
    v_can_expire BOOLEAN := FALSE;
    v_expiration_hours INTEGER := 24; -- Configurable expiration time in hours
BEGIN
    -- Get current status and creation time
    SELECT status, created_at INTO v_current_status, v_created_at
    FROM payment_transaction
    WHERE transaction_id = p_transaction_id;
    
    -- Check if transaction can be expired
    -- Only authorized transactions that are older than expiration time can expire
    IF v_current_status = 'AUTHORIZED' AND 
       (NOW() - v_created_at) > (v_expiration_hours * INTERVAL '1 hour') THEN
        v_can_expire := TRUE;
    END IF;
    
    RETURN v_can_expire;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Payment Reporting Functions
-- =============================================

-- Function to get transaction summary by status
CREATE OR REPLACE FUNCTION payment_get_transaction_summary_by_status(
    p_organization_id UUID DEFAULT NULL,
    p_account_id UUID DEFAULT NULL,
    p_start_date TIMESTAMP DEFAULT NULL,
    p_end_date TIMESTAMP DEFAULT NULL
) RETURNS TABLE (
    status VARCHAR(32),
    count BIGINT,
    total_amount DECIMAL(19,4)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pt.status,
        COUNT(*) AS count,
        SUM(pt.amount) AS total_amount
    FROM payment_transaction pt
    WHERE (p_organization_id IS NULL OR pt.organization_id = p_organization_id)
    AND (p_account_id IS NULL OR pt.account_id = p_account_id)
    AND (p_start_date IS NULL OR pt.created_at >= p_start_date)
    AND (p_end_date IS NULL OR pt.created_at <= p_end_date)
    GROUP BY pt.status
    ORDER BY pt.status;
END;
$$ LANGUAGE plpgsql;

-- Function to get transaction timeline for a specific transaction
CREATE OR REPLACE FUNCTION payment_get_transaction_timeline(
    p_transaction_id UUID
) RETURNS TABLE (
    event_id UUID,
    event_type VARCHAR(32),
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    event_data JSONB,
    created_at TIMESTAMP,
    created_by VARCHAR(128)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pe.event_id,
        pe.event_type,
        pe.previous_status,
        pe.new_status,
        pe.event_data,
        pe.created_at,
        pe.created_by
    FROM payment_event pe
    WHERE pe.transaction_id = p_transaction_id
    ORDER BY pe.created_at ASC;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate transaction fee summary
CREATE OR REPLACE FUNCTION payment_calculate_fee_summary(
    p_transaction_id UUID
) RETURNS JSONB AS $$
DECLARE
    v_total_fees DECIMAL(19,4) := 0;
    v_fee_summary JSONB := '{}'::jsonb;
    v_fee_record RECORD;
BEGIN
    -- Calculate total fees and breakdown by fee type
    FOR v_fee_record IN (
        SELECT fee_type, SUM(amount) AS fee_amount
        FROM payment_fee
        WHERE transaction_id = p_transaction_id
        GROUP BY fee_type
    ) LOOP
        v_total_fees := v_total_fees + v_fee_record.fee_amount;
        v_fee_summary := v_fee_summary || jsonb_build_object(v_fee_record.fee_type, v_fee_record.fee_amount);
    END LOOP;
    
    -- Add total to the summary
    v_fee_summary := v_fee_summary || jsonb_build_object('total', v_total_fees);
    
    RETURN v_fee_summary;
END;
$$ LANGUAGE plpgsql;

-- Commit transaction
COMMIT;