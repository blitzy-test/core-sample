package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the PaymentLifecycleService interface that manages payment state transitions.
 * This class handles the execution of state transitions according to defined rules, validates
 * transition requests, reports on transaction status, and coordinates with the event service
 * for proper event recording.
 */
public class PaymentLifecycleServiceImpl implements PaymentLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentLifecycleServiceImpl.class);
    
    private final PaymentEventService eventService;
    private final PaymentTransactionService transactionService;
    
    /**
     * Constructs a new PaymentLifecycleServiceImpl with required dependencies.
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
        
        // Record the event before updating the transaction
        PaymentEvent event = eventService.recordStatusChangeEvent(transaction, newStatus, userId);
        
        // If metadata is provided, record it as a custom event
        if (metadata != null && !metadata.isEmpty()) {
            eventService.recordCustomEvent(transaction, "STATE_TRANSITION_METADATA", userId, metadata);
        }
        
        // Update the transaction status
        PaymentStatus previousStatus = transaction.getStatus();
        transaction.updateStatus(newStatus);
        
        logger.info("Transaction {} state changed from {} to {} by user {}", 
            transaction.getTransactionId(), previousStatus, newStatus, userId);
        
        return transaction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidStateTransition(PaymentTransaction transaction, PaymentStatus newStatus) {
        if (transaction == null || newStatus == null) {
            return false;
        }
        
        return isValidStateTransition(transaction.getStatus(), newStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidStateTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        // Same state is always valid (idempotent operation)
        if (currentStatus == newStatus) {
            return true;
        }
        
        // Use the built-in transition validation from the PaymentStatus enum
        return currentStatus.canTransitionTo(newStatus);
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
        
        // If it's a final state, no transitions are possible
        if (currentStatus.isFinalState()) {
            return Collections.emptyList();
        }
        
        // Check each possible status to see if it's a valid transition
        return Stream.of(PaymentStatus.values())
            .filter(status -> currentStatus.canTransitionTo(status))
            .filter(status -> status != currentStatus) // Exclude the current status
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInFinalState(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return transaction.isInFinalState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCapture(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return transaction.canCapture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRefund(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return transaction.canRefund();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canVoid(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        return transaction.canVoid();
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
        
        // Verify that the transaction is in the CREATED state
        verifyTransactionState(transaction, PaymentStatus.CREATED);
        
        // Record the processing event
        Map<String, String> metadata = new HashMap<>();
        metadata.put("initiatedBy", userId);
        metadata.put("initiatedAt", Instant.now().toString());
        
        eventService.recordProcessingEvent(transaction, userId, metadata);
        
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
        
        // Verify that the transaction is in the PROCESSING state
        verifyTransactionState(transaction, PaymentStatus.PROCESSING);
        
        // Record metadata for the authorization
        Map<String, String> metadata = new HashMap<>();
        metadata.put("authorizedBy", userId);
        metadata.put("authorizedAt", Instant.now().toString());
        
        if (authorizationCode != null && !authorizationCode.trim().isEmpty()) {
            metadata.put("authorizationCode", authorizationCode);
        }
        
        // Record a custom event for the authorization details
        eventService.recordCustomEvent(transaction, "AUTHORIZATION", userId, metadata);
        
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
        
        // Verify that the transaction is in the AUTHORIZED state
        verifyTransactionState(transaction, PaymentStatus.AUTHORIZED);
        
        // Record the capture event
        boolean isPartial = captureAmount != null && 
                           !captureAmount.equals(transaction.getAmount().toString());
        
        eventService.recordCaptureEvent(transaction, userId, 
                                      captureAmount != null ? captureAmount : transaction.getAmount().toString(), 
                                      isPartial);
        
        // Record metadata for the capture
        Map<String, String> metadata = new HashMap<>();
        metadata.put("capturedBy", userId);
        metadata.put("capturedAt", Instant.now().toString());
        
        if (captureAmount != null) {
            metadata.put("captureAmount", captureAmount);
        }
        
        if (captureReference != null && !captureReference.trim().isEmpty()) {
            metadata.put("captureReference", captureReference);
        }
        
        // Execute the state transition
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
        
        // Verify that the transaction is in the CAPTURED state
        verifyTransactionState(transaction, PaymentStatus.CAPTURED);
        
        // Record the refund event
        boolean isPartial = refundAmount != null && 
                           !refundAmount.equals(transaction.getAmount().toString());
        
        eventService.recordRefundEvent(transaction, userId, 
                                     refundAmount != null ? refundAmount : transaction.getAmount().toString(), 
                                     refundReason != null ? refundReason : "No reason provided", 
                                     isPartial);
        
        // Record metadata for the refund
        Map<String, String> metadata = new HashMap<>();
        metadata.put("refundedBy", userId);
        metadata.put("refundedAt", Instant.now().toString());
        
        if (refundAmount != null) {
            metadata.put("refundAmount", refundAmount);
        }
        
        if (refundReason != null && !refundReason.trim().isEmpty()) {
            metadata.put("refundReason", refundReason);
        }
        
        if (refundReference != null && !refundReference.trim().isEmpty()) {
            metadata.put("refundReference", refundReference);
        }
        
        // Execute the state transition
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
        
        // Verify that the transaction is in the AUTHORIZED state
        verifyTransactionState(transaction, PaymentStatus.AUTHORIZED);
        
        // Record the void event
        eventService.recordVoidEvent(transaction, userId, 
                                   voidReason != null ? voidReason : "No reason provided");
        
        // Record metadata for the void
        Map<String, String> metadata = new HashMap<>();
        metadata.put("voidedBy", userId);
        metadata.put("voidedAt", Instant.now().toString());
        
        if (voidReason != null && !voidReason.trim().isEmpty()) {
            metadata.put("voidReason", voidReason);
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
        
        // Record the error event
        eventService.recordErrorEvent(transaction, userId, 
                                    errorCode != null ? errorCode : "UNKNOWN_ERROR", 
                                    errorMessage != null ? errorMessage : "Unknown error occurred");
        
        // Record metadata for the failure
        Map<String, String> metadata = new HashMap<>();
        metadata.put("failedAt", Instant.now().toString());
        metadata.put("reportedBy", userId);
        
        if (errorCode != null && !errorCode.trim().isEmpty()) {
            metadata.put("errorCode", errorCode);
        }
        
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            metadata.put("errorMessage", errorMessage);
        }
        
        // Execute the state transition
        return executeStateTransition(transaction, PaymentStatus.FAILED, userId, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentStatus getCurrentStatus(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        return transaction.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PaymentEvent> getLifecycleHistory(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        // Verify that the transaction exists
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Get the complete timeline of events for this transaction
        return eventService.buildTransactionTimeline(transactionId);
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
            return false;
        }
        
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
        
        // Verify that the transaction exists
        PaymentTransaction transaction = transactionService.getTransactionById(transactionId);
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        
        // Get the complete timeline of events for this transaction
        List<PaymentEvent> events = eventService.buildTransactionTimeline(transactionId);
        
        // Create the summary map
        Map<String, Object> summary = new HashMap<>();
        
        // Add basic transaction information
        summary.put("transactionId", transaction.getTransactionId());
        summary.put("currentStatus", transaction.getStatus().name());
        summary.put("statusDisplayName", transaction.getStatus().getDisplayName());
        summary.put("statusDescription", transaction.getStatus().getDescription());
        summary.put("isInFinalState", transaction.isInFinalState());
        summary.put("createdAt", transaction.getCreatedAt());
        summary.put("lastUpdatedAt", transaction.getUpdatedAt());
        summary.put("totalLifecycleTimeMs", 
            Duration.between(transaction.getCreatedAt(), transaction.getUpdatedAt()).toMillis());
        
        // Calculate time spent in each state
        Map<String, Long> timeInStates = calculateTimeInStates(events);
        summary.put("timeInStates", timeInStates);
        
        // Extract key lifecycle events
        Map<String, Object> keyEvents = extractKeyLifecycleEvents(events);
        summary.put("keyEvents", keyEvents);
        
        // Add event counts
        summary.put("totalEvents", events.size());
        summary.put("statusChangeEvents", 
            events.stream().filter(e -> "STATUS_CHANGE".equals(e.getEventType())).count());
        summary.put("errorEvents", 
            events.stream().filter(e -> "ERROR".equals(e.getEventType())).count());
        
        // Add valid next states
        List<String> validNextStates = getValidNextStates(transaction).stream()
            .map(PaymentStatus::name)
            .collect(Collectors.toList());
        summary.put("validNextStates", validNextStates);
        
        // Add operation capabilities
        summary.put("canCapture", canCapture(transaction));
        summary.put("canRefund", canRefund(transaction));
        summary.put("canVoid", canVoid(transaction));
        
        return summary;
    }
    
    /**
     * Calculates the time spent in each state based on the event timeline.
     *
     * @param events The chronologically ordered list of events
     * @return A map of state names to duration in milliseconds
     */
    private Map<String, Long> calculateTimeInStates(List<PaymentEvent> events) {
        Map<String, Long> timeInStates = new HashMap<>();
        
        // Filter to only status change events and sort by timestamp
        List<PaymentEvent> statusChanges = events.stream()
            .filter(e -> "STATUS_CHANGE".equals(e.getEventType()))
            .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
            .collect(Collectors.toList());
        
        if (statusChanges.isEmpty()) {
            return timeInStates;
        }
        
        // Process each status change event
        for (int i = 0; i < statusChanges.size() - 1; i++) {
            PaymentEvent current = statusChanges.get(i);
            PaymentEvent next = statusChanges.get(i + 1);
            
            String status = current.getNewStatus();
            long durationMs = Duration.between(current.getCreatedAt(), next.getCreatedAt()).toMillis();
            
            timeInStates.put(status, durationMs);
        }
        
        // Handle the last state (still current)
        PaymentEvent lastChange = statusChanges.get(statusChanges.size() - 1);
        String lastStatus = lastChange.getNewStatus();
        long lastDurationMs = Duration.between(lastChange.getCreatedAt(), Instant.now()).toMillis();
        
        timeInStates.put(lastStatus, lastDurationMs);
        
        return timeInStates;
    }
    
    /**
     * Extracts key lifecycle events from the event timeline.
     *
     * @param events The chronologically ordered list of events
     * @return A map of event types to event details
     */
    private Map<String, Object> extractKeyLifecycleEvents(List<PaymentEvent> events) {
        Map<String, Object> keyEvents = new HashMap<>();
        
        // Define the key event types we're interested in
        Set<String> keyEventTypes = new HashSet<>(Arrays.asList(
            "TRANSACTION_CREATED", 
            "PROCESSING_INITIATED", 
            "CAPTURE_INITIATED", 
            "REFUND_INITIATED", 
            "ERROR"
        ));
        
        // Group events by type and take the most recent of each type
        Map<String, List<PaymentEvent>> eventsByType = events.stream()
            .filter(e -> keyEventTypes.contains(e.getEventType()))
            .collect(Collectors.groupingBy(PaymentEvent::getEventType));
        
        // For each key event type, extract the most recent event
        for (Map.Entry<String, List<PaymentEvent>> entry : eventsByType.entrySet()) {
            String eventType = entry.getKey();
            List<PaymentEvent> typeEvents = entry.getValue();
            
            if (!typeEvents.isEmpty()) {
                // Sort by timestamp descending and take the first (most recent)
                PaymentEvent mostRecent = typeEvents.stream()
                    .sorted(Comparator.comparing(PaymentEvent::getCreatedAt).reversed())
                    .findFirst()
                    .orElse(null);
                
                if (mostRecent != null) {
                    Map<String, Object> eventDetails = new HashMap<>();
                    eventDetails.put("timestamp", mostRecent.getCreatedAt());
                    eventDetails.put("actor", mostRecent.getCreatedBy());
                    eventDetails.put("eventId", mostRecent.getEventId());
                    
                    if (mostRecent.getEventData() != null) {
                        eventDetails.put("data", mostRecent.getEventData());
                    }
                    
                    keyEvents.put(eventType, eventDetails);
                }
            }
        }
        
        return keyEvents;
    }
}