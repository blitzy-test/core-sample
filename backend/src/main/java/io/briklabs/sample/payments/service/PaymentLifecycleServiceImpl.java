package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the PaymentLifecycleService interface that manages payment state transitions.
 * This class handles the execution of state transitions according to defined rules, validates
 * transition requests, reports on transaction status, and coordinates with the event service
 * for proper event recording.
 */
public class PaymentLifecycleServiceImpl implements PaymentLifecycleService {

    private final PaymentEventService eventService;
    private final PaymentTransactionService transactionService;

    /**
     * Creates a new PaymentLifecycleServiceImpl with the required dependencies.
     *
     * @param eventService The event service for recording lifecycle events
     * @param transactionService The transaction service for retrieving and updating transactions
     */
    public PaymentLifecycleServiceImpl(PaymentEventService eventService, PaymentTransactionService transactionService) {
        this.eventService = eventService;
        this.transactionService = transactionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction executeStateTransition(PaymentTransaction transaction, PaymentStatus newStatus, 
                                                   String userId, Map<String, String> metadata) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        // Validate the state transition
        if (!isValidStateTransition(transaction, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid state transition from %s to %s for transaction %s",
                            transaction.getStatus(), newStatus, transaction.getTransactionId()));
        }

        // Prepare event data
        String eventData = buildEventDataJson(metadata);
        
        // Record the status change event
        PaymentStatus previousStatus = transaction.getStatus();
        eventService.recordStatusChangeEvent(
                transaction.getTransactionId(),
                previousStatus,
                newStatus,
                eventData,
                userId
        );

        // Update the transaction status
        transaction.updateStatus(newStatus);
        
        // Return the updated transaction
        return transaction;
    }

    /**
     * Builds a JSON string from a metadata map for event recording.
     *
     * @param metadata The metadata map to convert
     * @return A JSON string representation of the metadata
     */
    private String buildEventDataJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidStateTransition(PaymentTransaction transaction, PaymentStatus newStatus) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        return isValidStateTransition(transaction.getStatus(), newStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidStateTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current status cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        // If the statuses are the same, it's not a valid transition
        if (currentStatus == newStatus) {
            return false;
        }
        
        // Define valid transitions based on current state
        switch (currentStatus) {
            case PENDING:
                // Pending can transition to authorized, declined, failed, or processing
                return newStatus == PaymentStatus.AUTHORIZED || 
                       newStatus == PaymentStatus.DECLINED || 
                       newStatus == PaymentStatus.FAILED ||
                       newStatus == PaymentStatus.PROCESSING;
                
            case PROCESSING:
                // Processing can transition to authorized, declined, or failed
                return newStatus == PaymentStatus.AUTHORIZED || 
                       newStatus == PaymentStatus.DECLINED || 
                       newStatus == PaymentStatus.FAILED;
                
            case AUTHORIZED:
                // Authorized can transition to captured, partially_captured, voided, or failed
                return newStatus == PaymentStatus.CAPTURED || 
                       newStatus == PaymentStatus.PARTIALLY_CAPTURED || 
                       newStatus == PaymentStatus.VOIDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case PARTIALLY_CAPTURED:
                // Partially captured can transition to captured, partially_refunded, or failed
                return newStatus == PaymentStatus.CAPTURED || 
                       newStatus == PaymentStatus.PARTIALLY_REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case CAPTURED:
                // Captured can transition to refunded, partially_refunded, or failed
                return newStatus == PaymentStatus.REFUNDED || 
                       newStatus == PaymentStatus.PARTIALLY_REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case PARTIALLY_REFUNDED:
                // Partially refunded can transition to refunded or failed
                return newStatus == PaymentStatus.REFUNDED || 
                       newStatus == PaymentStatus.FAILED;
                
            case REFUNDED:
            case VOIDED:
            case DECLINED:
            case FAILED:
                // Terminal states - no further transitions allowed
                return false;
                
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentStatus> getValidNextStates(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return getValidNextStates(transaction.getStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentStatus> getValidNextStates(PaymentStatus currentStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current status cannot be null");
        }
        
        List<PaymentStatus> validStates = new ArrayList<>();
        
        // Check each possible status to see if it's a valid transition
        for (PaymentStatus status : PaymentStatus.values()) {
            if (isValidStateTransition(currentStatus, status)) {
                validStates.add(status);
            }
        }
        
        return validStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInFinalState(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        PaymentStatus status = transaction.getStatus();
        
        // Define terminal states
        return status == PaymentStatus.REFUNDED ||
               status == PaymentStatus.VOIDED ||
               status == PaymentStatus.DECLINED ||
               status == PaymentStatus.FAILED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCapture(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        PaymentStatus status = transaction.getStatus();
        
        // Only authorized and partially_captured transactions can be captured
        return status == PaymentStatus.AUTHORIZED || 
               status == PaymentStatus.PARTIALLY_CAPTURED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRefund(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        PaymentStatus status = transaction.getStatus();
        
        // Only captured and partially_refunded transactions can be refunded
        return status == PaymentStatus.CAPTURED || 
               status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canVoid(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        // Only authorized transactions can be voided
        return transaction.getStatus() == PaymentStatus.AUTHORIZED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction initiateProcessing(PaymentTransaction transaction, String userId) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Verify that the transaction is in PENDING state
        verifyTransactionState(transaction, PaymentStatus.PENDING);
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "processing_initiated");
        metadata.put("timestamp", Instant.now().toString());
        
        // Execute the state transition
        return executeStateTransition(transaction, PaymentStatus.PROCESSING, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction markAsAuthorized(PaymentTransaction transaction, String userId, String authorizationCode) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Verify that the transaction is in a valid state for authorization
        if (transaction.getStatus() != PaymentStatus.PENDING && 
            transaction.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be authorized from state %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "authorization");
        metadata.put("timestamp", Instant.now().toString());
        
        if (authorizationCode != null && !authorizationCode.trim().isEmpty()) {
            metadata.put("authorization_code", authorizationCode);
        }
        
        // Execute the state transition
        return executeStateTransition(transaction, PaymentStatus.AUTHORIZED, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction markAsCaptured(PaymentTransaction transaction, String userId, 
                                           String captureAmount, String captureReference) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Verify that the transaction can be captured
        if (!canCapture(transaction)) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be captured from state %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "capture");
        metadata.put("timestamp", Instant.now().toString());
        
        if (captureAmount != null && !captureAmount.trim().isEmpty()) {
            metadata.put("capture_amount", captureAmount);
            
            // Determine if this is a partial capture based on the amount
            try {
                double amount = Double.parseDouble(captureAmount);
                double transactionAmount = transaction.getAmount().doubleValue();
                
                if (amount < transactionAmount) {
                    // This is a partial capture
                    return executeStateTransition(transaction, PaymentStatus.PARTIALLY_CAPTURED, userId, metadata);
                }
            } catch (NumberFormatException e) {
                // If we can't parse the amount, assume it's a full capture
            }
        }
        
        if (captureReference != null && !captureReference.trim().isEmpty()) {
            metadata.put("capture_reference", captureReference);
        }
        
        // Execute the state transition to CAPTURED
        return executeStateTransition(transaction, PaymentStatus.CAPTURED, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction markAsRefunded(PaymentTransaction transaction, String userId, 
                                           String refundAmount, String refundReason, String refundReference) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Verify that the transaction can be refunded
        if (!canRefund(transaction)) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be refunded from state %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "refund");
        metadata.put("timestamp", Instant.now().toString());
        
        if (refundAmount != null && !refundAmount.trim().isEmpty()) {
            metadata.put("refund_amount", refundAmount);
            
            // Determine if this is a partial refund based on the amount
            try {
                double amount = Double.parseDouble(refundAmount);
                double transactionAmount = transaction.getAmount().doubleValue();
                
                if (amount < transactionAmount) {
                    // This is a partial refund
                    return executeStateTransition(transaction, PaymentStatus.PARTIALLY_REFUNDED, userId, metadata);
                }
            } catch (NumberFormatException e) {
                // If we can't parse the amount, assume it's a full refund
            }
        }
        
        if (refundReason != null && !refundReason.trim().isEmpty()) {
            metadata.put("refund_reason", refundReason);
        }
        
        if (refundReference != null && !refundReference.trim().isEmpty()) {
            metadata.put("refund_reference", refundReference);
        }
        
        // Execute the state transition to REFUNDED
        return executeStateTransition(transaction, PaymentStatus.REFUNDED, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction markAsVoided(PaymentTransaction transaction, String userId, String voidReason) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Verify that the transaction can be voided
        if (!canVoid(transaction)) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be voided from state %s",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "void");
        metadata.put("timestamp", Instant.now().toString());
        
        if (voidReason != null && !voidReason.trim().isEmpty()) {
            metadata.put("void_reason", voidReason);
        }
        
        // Execute the state transition
        return executeStateTransition(transaction, PaymentStatus.VOIDED, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentTransaction markAsFailed(PaymentTransaction transaction, String userId, 
                                         String errorCode, String errorMessage) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // Create metadata for the event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("action", "failure");
        metadata.put("timestamp", Instant.now().toString());
        
        if (errorCode != null && !errorCode.trim().isEmpty()) {
            metadata.put("error_code", errorCode);
        }
        
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            metadata.put("error_message", errorMessage);
        }
        
        // Execute the state transition
        // Note: Almost any state can transition to FAILED
        if (isValidStateTransition(transaction, PaymentStatus.FAILED)) {
            return executeStateTransition(transaction, PaymentStatus.FAILED, userId, metadata);
        } else {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot transition from %s to FAILED",
                            transaction.getTransactionId(), transaction.getStatus()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentStatus getCurrentStatus(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        // Retrieve the transaction from the service
        Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(transactionId);
        
        if (!transactionOpt.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transactionOpt.get().getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentEvent> getLifecycleHistory(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        // Retrieve all events for the transaction
        return eventService.getEventsByTransactionId(transactionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifyTransactionState(PaymentTransaction transaction, PaymentStatus expectedStatus) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (expectedStatus == null) {
            throw new IllegalArgumentException("Expected status cannot be null");
        }
        
        if (transaction.getStatus() != expectedStatus) {
            throw new IllegalStateException(
                    String.format("Transaction %s is in state %s, expected %s",
                            transaction.getTransactionId(), transaction.getStatus(), expectedStatus));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateTransactionState(PaymentTransaction transaction, PaymentStatus... allowedStatuses) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (allowedStatuses == null || allowedStatuses.length == 0) {
            throw new IllegalArgumentException("At least one allowed status must be provided");
        }
        
        // Check if the transaction's status is in the list of allowed statuses
        for (PaymentStatus status : allowedStatuses) {
            if (transaction.getStatus() == status) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getLifecycleSummary(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        // Retrieve the transaction
        Optional<PaymentTransaction> transactionOpt = transactionService.getTransactionById(transactionId);
        
        if (!transactionOpt.isPresent()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        PaymentTransaction transaction = transactionOpt.get();
        
        // Retrieve all events for the transaction
        List<PaymentEvent> events = eventService.getEventsByTransactionId(transactionId);
        
        // Create the summary map
        Map<String, Object> summary = new HashMap<>();
        
        // Add basic transaction information
        summary.put("transactionId", transactionId.toString());
        summary.put("currentStatus", transaction.getStatus().toString());
        summary.put("amount", transaction.getAmount().toString());
        summary.put("currency", transaction.getCurrency());
        summary.put("createdAt", transaction.getCreatedAt().toString());
        summary.put("updatedAt", transaction.getUpdatedAt().toString());
        
        // Add lifecycle capabilities
        summary.put("canCapture", canCapture(transaction));
        summary.put("canRefund", canRefund(transaction));
        summary.put("canVoid", canVoid(transaction));
        summary.put("isInFinalState", isInFinalState(transaction));
        summary.put("validNextStates", getValidNextStates(transaction).stream()
                .map(Enum::toString)
                .collect(Collectors.toList()));
        
        // Calculate time spent in each state
        Map<String, Duration> timeInStates = calculateTimeInStates(events);
        Map<String, String> formattedTimeInStates = new HashMap<>();
        
        for (Map.Entry<String, Duration> entry : timeInStates.entrySet()) {
            formattedTimeInStates.put(entry.getKey(), formatDuration(entry.getValue()));
        }
        
        summary.put("timeInStates", formattedTimeInStates);
        
        // Add key events
        List<Map<String, Object>> keyEvents = extractKeyEvents(events);
        summary.put("keyEvents", keyEvents);
        
        // Add event counts by type
        Map<String, Long> eventCounts = eventService.getEventCountByType(transactionId);
        summary.put("eventCounts", eventCounts);
        
        return summary;
    }

    /**
     * Calculates the time spent in each state based on status change events.
     *
     * @param events List of payment events for a transaction
     * @return Map of state names to durations
     */
    private Map<String, Duration> calculateTimeInStates(List<PaymentEvent> events) {
        Map<String, Duration> timeInStates = new HashMap<>();
        Map<String, Instant> stateStartTimes = new HashMap<>();
        
        // Filter to status change events and sort chronologically
        List<PaymentEvent> statusEvents = events.stream()
                .filter(e -> "STATUS_CHANGE".equals(e.getEventType()))
                .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
                .collect(Collectors.toList());
        
        if (statusEvents.isEmpty()) {
            return timeInStates;
        }
        
        // Initialize with the first state
        PaymentEvent firstEvent = statusEvents.get(0);
        String currentState = firstEvent.getNewStatus();
        stateStartTimes.put(currentState, firstEvent.getCreatedAt());
        
        // Process subsequent state changes
        for (int i = 1; i < statusEvents.size(); i++) {
            PaymentEvent event = statusEvents.get(i);
            String previousState = event.getPreviousStatus();
            String newState = event.getNewStatus();
            Instant stateStart = stateStartTimes.get(previousState);
            Instant stateEnd = event.getCreatedAt();
            
            // Calculate duration in this state
            if (stateStart != null) {
                Duration duration = Duration.between(stateStart, stateEnd);
                
                // Add to existing duration or set new duration
                if (timeInStates.containsKey(previousState)) {
                    duration = timeInStates.get(previousState).plus(duration);
                }
                
                timeInStates.put(previousState, duration);
            }
            
            // Update current state and start time
            currentState = newState;
            stateStartTimes.put(currentState, stateEnd);
        }
        
        // Calculate duration for the current state (from last change to now)
        Instant stateStart = stateStartTimes.get(currentState);
        if (stateStart != null) {
            Duration duration = Duration.between(stateStart, Instant.now());
            
            // Add to existing duration or set new duration
            if (timeInStates.containsKey(currentState)) {
                duration = timeInStates.get(currentState).plus(duration);
            }
            
            timeInStates.put(currentState, duration);
        }
        
        return timeInStates;
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration The duration to format
     * @return A formatted string representation of the duration
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        long seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
        
        StringBuilder formatted = new StringBuilder();
        
        if (days > 0) {
            formatted.append(days).append("d ");
        }
        
        if (hours > 0 || days > 0) {
            formatted.append(hours).append("h ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            formatted.append(minutes).append("m ");
        }
        
        formatted.append(seconds).append("s");
        
        return formatted.toString();
    }

    /**
     * Extracts key events from the full event list for summary display.
     *
     * @param events List of payment events for a transaction
     * @return List of simplified event data for key events
     */
    private List<Map<String, Object>> extractKeyEvents(List<PaymentEvent> events) {
        // Define key event types to include
        Set<String> keyEventTypes = Stream.of(
                "STATUS_CHANGE", "ERROR", "CAPTURE", "REFUND", "VOID"
        ).collect(Collectors.toSet());
        
        // Filter to key events and sort chronologically
        return events.stream()
                .filter(e -> keyEventTypes.contains(e.getEventType()))
                .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
                .map(this::simplifyEvent)
                .collect(Collectors.toList());
    }

    /**
     * Simplifies an event into a map of key information for summary display.
     *
     * @param event The event to simplify
     * @return A map containing simplified event data
     */
    private Map<String, Object> simplifyEvent(PaymentEvent event) {
        Map<String, Object> simplified = new HashMap<>();
        
        simplified.put("eventId", event.getEventId().toString());
        simplified.put("eventType", event.getEventType());
        simplified.put("timestamp", event.getCreatedAt().toString());
        simplified.put("createdBy", event.getCreatedBy());
        
        if ("STATUS_CHANGE".equals(event.getEventType())) {
            simplified.put("previousStatus", event.getPreviousStatus());
            simplified.put("newStatus", event.getNewStatus());
        }
        
        // Include a subset of event data if available
        if (event.getEventData() != null && !event.getEventData().isEmpty()) {
            simplified.put("data", event.getEventData());
        }
        
        return simplified;
    }
}