package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.exception.ConnectionException;
import io.briklabs.sample.payments.data.exception.QueryExecutionException;
import io.briklabs.sample.payments.data.exception.ResourceNotFoundException;
import io.briklabs.sample.payments.data.exception.ValidationException;
import io.briklabs.sample.payments.data.query.PaymentFilterParams;
import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentTransaction;
import io.briklabs.sample.payments.model.PaymentTransaction.PaymentStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the PaymentEventService interface that manages payment lifecycle event tracking.
 * <p>
 * This class handles the recording of events at each transaction lifecycle stage, constructs
 * event timelines, and provides comprehensive event history for audit and reporting purposes.
 * It's essential for maintaining a complete audit trail of payment operations.
 * </p>
 */
public class PaymentEventServiceImpl implements PaymentEventService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventServiceImpl.class);
    
    private final PaymentEventDAO eventDAO;
    
    /**
     * Creates a new PaymentEventServiceImpl with the provided DAO factory.
     *
     * @param daoFactory The factory for creating data access objects
     */
    public PaymentEventServiceImpl(PaymentDAOFactory daoFactory) {
        this.eventDAO = daoFactory.getPaymentEventDAO();
    }

    @Override
    public PaymentEvent recordEvent(UUID transactionId, String eventType, String eventData, String createdBy) {
        return recordEvent(transactionId, eventType, eventData, createdBy, null);
    }

    @Override
    public PaymentEvent recordEvent(UUID transactionId, String eventType, String eventData, 
                                   String createdBy, UUID correlationId) {
        validateRequiredParameters(transactionId, eventType, createdBy);
        
        try {
            PaymentEvent event = PaymentEvent.createEvent(
                transactionId,
                eventType,
                eventData != null ? eventData : "{}",
                createdBy,
                correlationId
            );
            
            logger.debug("Recording payment event: type={}, transactionId={}, correlationId={}", 
                        eventType, transactionId, correlationId);
            
            return eventDAO.create(event);
        } catch (ValidationException | ConnectionException | QueryExecutionException e) {
            logger.error("Failed to record payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record payment event", e);
        }
    }

    @Override
    public PaymentEvent recordStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                              PaymentStatus newStatus, String eventData, String createdBy) {
        return recordStatusChangeEvent(transactionId, previousStatus, newStatus, eventData, createdBy, null);
    }

    @Override
    public PaymentEvent recordStatusChangeEvent(UUID transactionId, PaymentStatus previousStatus, 
                                              PaymentStatus newStatus, String eventData, 
                                              String createdBy, UUID correlationId) {
        validateRequiredParameters(transactionId, createdBy);
        
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        try {
            PaymentEvent event = PaymentEvent.createStatusChangeEvent(
                transactionId,
                previousStatus != null ? previousStatus.name() : null,
                newStatus.name(),
                eventData != null ? eventData : "{}",
                createdBy,
                correlationId
            );
            
            logger.debug("Recording status change event: {} -> {}, transactionId={}", 
                        previousStatus, newStatus, transactionId);
            
            return eventDAO.create(event);
        } catch (ValidationException | ConnectionException | QueryExecutionException e) {
            logger.error("Failed to record status change event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record status change event", e);
        }
    }

    @Override
    public PaymentEvent recordErrorEvent(UUID transactionId, String errorMessage, 
                                       String errorDetails, String createdBy) {
        return recordErrorEvent(transactionId, errorMessage, errorDetails, createdBy, null);
    }

    @Override
    public PaymentEvent recordErrorEvent(UUID transactionId, String errorMessage, 
                                       String errorDetails, String createdBy, UUID correlationId) {
        validateRequiredParameters(transactionId, createdBy);
        
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        
        try {
            PaymentEvent event = PaymentEvent.createErrorEvent(
                transactionId,
                errorMessage,
                errorDetails != null ? errorDetails : "{}",
                createdBy,
                correlationId
            );
            
            logger.debug("Recording error event: {}, transactionId={}", errorMessage, transactionId);
            
            return eventDAO.create(event);
        } catch (ValidationException | ConnectionException | QueryExecutionException e) {
            logger.error("Failed to record error event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record error event", e);
        }
    }

    @Override
    public PaymentEvent recordTransactionCreatedEvent(PaymentTransaction transaction, String createdBy) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        UUID transactionId = transaction.getTransactionId();
        validateRequiredParameters(transactionId, createdBy);
        
        // Create JSON representation of transaction details
        String eventData = String.format(
            "{\"amount\":\"%s\",\"currency\":\"%s\",\"merchantId\":\"%s\",\"paymentType\":\"%s\"}",
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getMerchantId(),
            transaction.getPaymentType()
        );
        
        return recordEvent(transactionId, "TRANSACTION_CREATED", eventData, createdBy);
    }

    @Override
    public PaymentEvent recordCaptureEvent(UUID transactionId, String captureAmount, 
                                         String captureReference, boolean isPartialCapture, String createdBy) {
        validateRequiredParameters(transactionId, createdBy);
        
        if (captureAmount == null || captureAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Capture amount cannot be null or empty");
        }
        
        // Create JSON representation of capture details
        String eventData = String.format(
            "{\"amount\":\"%s\",\"reference\":\"%s\",\"isPartial\":%b}",
            captureAmount,
            captureReference != null ? captureReference : "",
            isPartialCapture
        );
        
        String eventType = isPartialCapture ? "PARTIAL_CAPTURE" : "CAPTURE";
        
        logger.debug("Recording capture event: amount={}, isPartial={}, transactionId={}", 
                    captureAmount, isPartialCapture, transactionId);
        
        return recordEvent(transactionId, eventType, eventData, createdBy);
    }

    @Override
    public PaymentEvent recordRefundEvent(UUID transactionId, String refundAmount, String refundReason,
                                        String refundReference, boolean isPartialRefund, String createdBy) {
        validateRequiredParameters(transactionId, createdBy);
        
        if (refundAmount == null || refundAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Refund amount cannot be null or empty");
        }
        
        // Create JSON representation of refund details
        String eventData = String.format(
            "{\"amount\":\"%s\",\"reason\":\"%s\",\"reference\":\"%s\",\"isPartial\":%b}",
            refundAmount,
            refundReason != null ? refundReason : "",
            refundReference != null ? refundReference : "",
            isPartialRefund
        );
        
        String eventType = isPartialRefund ? "PARTIAL_REFUND" : "REFUND";
        
        logger.debug("Recording refund event: amount={}, isPartial={}, transactionId={}", 
                    refundAmount, isPartialRefund, transactionId);
        
        return recordEvent(transactionId, eventType, eventData, createdBy);
    }

    @Override
    public PaymentEvent recordVoidEvent(UUID transactionId, String voidReason, String createdBy) {
        validateRequiredParameters(transactionId, createdBy);
        
        // Create JSON representation of void details
        String eventData = String.format(
            "{\"reason\":\"%s\"}",
            voidReason != null ? voidReason : ""
        );
        
        logger.debug("Recording void event: transactionId={}", transactionId);
        
        return recordEvent(transactionId, "VOID", eventData, createdBy);
    }

    @Override
    public List<PaymentEvent> getEventsByTransactionId(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            return eventDAO.findByTransactionId(transactionId);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by transaction ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by transaction ID", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByType(UUID transactionId, String eventType) {
        validateRequiredParameters(transactionId, eventType);
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            return eventDAO.findByEventType(eventType, filterParams)
                .stream()
                .filter(event -> event.getTransactionId().equals(transactionId))
                .collect(Collectors.toList());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by type: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by type", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByTypes(UUID transactionId, List<String> eventTypes) {
        validateRequiredParameters(transactionId);
        
        if (eventTypes == null || eventTypes.isEmpty()) {
            throw new IllegalArgumentException("Event types list cannot be null or empty");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            return eventDAO.findByEventTypeIn(eventTypes, filterParams)
                .stream()
                .filter(event -> event.getTransactionId().equals(transactionId))
                .collect(Collectors.toList());
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by types: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by types", e);
        }
    }

    @Override
    public List<PaymentEvent> getStatusChangeEvents(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            return eventDAO.findStatusChangeEvents(transactionId, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve status change events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve status change events", e);
        }
    }

    @Override
    public List<PaymentEvent> getErrorEvents(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            return eventDAO.findErrorEvents(transactionId, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve error events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve error events", e);
        }
    }

    @Override
    public PaymentEvent getMostRecentEvent(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            return eventDAO.findMostRecentEvent(transactionId);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve most recent event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve most recent event", e);
        }
    }

    @Override
    public PaymentEvent getMostRecentEventByType(UUID transactionId, String eventType) {
        validateRequiredParameters(transactionId, eventType);
        
        try {
            return eventDAO.findMostRecentEventByType(transactionId, eventType);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve most recent event by type: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve most recent event by type", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByTimeRange(Instant startTime, Instant endTime, int offset, int limit) {
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("At least one of startTime or endTime must be provided");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByCreatedAtBetween(startTime, endTime, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by time range: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by time range", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByCreator(String createdBy, int offset, int limit) {
        validateRequiredParameters(createdBy);
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByCreatedBy(createdBy, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by creator: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by creator", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByCorrelationId(UUID correlationId) {
        if (correlationId == null) {
            throw new IllegalArgumentException("Correlation ID cannot be null");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            return eventDAO.findByCorrelationId(correlationId, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by correlation ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by correlation ID", e);
        }
    }

    @Override
    public List<Map<String, Object>> getTransactionTimeline(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            List<PaymentEvent> events = eventDAO.getTransactionTimeline(transactionId);
            return buildTimelineFromEvents(events);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve transaction timeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transaction timeline", e);
        }
    }

    @Override
    public List<Map<String, Object>> getFilteredTimeline(UUID transactionId, List<String> eventTypes,
                                                      Instant startTime, Instant endTime) {
        validateRequiredParameters(transactionId);
        
        try {
            List<PaymentEvent> allEvents = eventDAO.getTransactionTimeline(transactionId);
            List<PaymentEvent> filteredEvents = allEvents.stream()
                .filter(event -> {
                    boolean matchesType = eventTypes == null || eventTypes.isEmpty() || 
                                         eventTypes.contains(event.getEventType());
                    boolean afterStart = startTime == null || 
                                        !event.getCreatedAt().isBefore(startTime);
                    boolean beforeEnd = endTime == null || 
                                       !event.getCreatedAt().isAfter(endTime);
                    
                    return matchesType && afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
            
            return buildTimelineFromEvents(filteredEvents);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve filtered timeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve filtered timeline", e);
        }
    }

    @Override
    public Map<String, Long> getEventCountByType(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            return eventDAO.countEventsByType(transactionId);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve event count by type: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve event count by type", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByOrganization(UUID organizationId, int offset, int limit) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withOrganizationId(organizationId)
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByOrganizationId(organizationId, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by organization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by organization", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByAccount(UUID organizationId, UUID accountId, int offset, int limit) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withOrganizationId(organizationId)
                .withAccountId(accountId)
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByOrganizationIdAndAccountId(organizationId, accountId, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by account", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByNewStatus(PaymentStatus status, int offset, int limit) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByNewStatus(status.name(), filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by new status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by new status", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByPreviousStatus(PaymentStatus status, int offset, int limit) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByPreviousStatus(status.name(), filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve events by previous status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by previous status", e);
        }
    }

    @Override
    public List<PaymentEvent> getAuditTrail(Instant startTime, Instant endTime, int offset, int limit) {
        if (startTime == null && endTime == null) {
            throw new IllegalArgumentException("At least one of startTime or endTime must be provided");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.getAuditTrail(startTime, endTime, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve audit trail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve audit trail", e);
        }
    }

    @Override
    public List<PaymentEvent> getUserActivityLog(String userId, Instant startTime, Instant endTime, 
                                               int offset, int limit) {
        validateRequiredParameters(userId);
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.getUserActivityLog(userId, startTime, endTime, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to retrieve user activity log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user activity log", e);
        }
    }

    @Override
    public Map<String, Object> buildEventTimeline(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            List<PaymentEvent> events = eventDAO.getTransactionTimeline(transactionId);
            
            if (events.isEmpty()) {
                return Collections.emptyMap();
            }
            
            // Group events by type
            Map<String, List<PaymentEvent>> eventsByType = events.stream()
                .collect(Collectors.groupingBy(PaymentEvent::getEventType));
            
            // Build timeline data
            Map<String, Object> timeline = new HashMap<>();
            timeline.put("transactionId", transactionId.toString());
            timeline.put("eventCount", events.size());
            timeline.put("firstEventTime", events.get(0).getCreatedAt());
            timeline.put("lastEventTime", events.get(events.size() - 1).getCreatedAt());
            timeline.put("events", buildTimelineFromEvents(events));
            
            // Add status transitions if available
            List<PaymentEvent> statusChanges = eventsByType.getOrDefault("STATUS_CHANGE", Collections.emptyList());
            if (!statusChanges.isEmpty()) {
                List<Map<String, Object>> transitions = statusChanges.stream()
                    .map(event -> {
                        Map<String, Object> transition = new HashMap<>();
                        transition.put("timestamp", event.getCreatedAt());
                        transition.put("from", event.getPreviousStatus());
                        transition.put("to", event.getNewStatus());
                        transition.put("by", event.getCreatedBy());
                        return transition;
                    })
                    .collect(Collectors.toList());
                
                timeline.put("statusTransitions", transitions);
            }
            
            // Add error events if available
            List<PaymentEvent> errors = eventsByType.getOrDefault("ERROR", Collections.emptyList());
            if (!errors.isEmpty()) {
                timeline.put("hasErrors", true);
                timeline.put("errorCount", errors.size());
            } else {
                timeline.put("hasErrors", false);
                timeline.put("errorCount", 0);
            }
            
            return timeline;
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to build event timeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build event timeline", e);
        }
    }

    @Override
    public List<PaymentEvent> searchEventData(String jsonPath, String value, int offset, int limit) {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON path cannot be null or empty");
        }
        
        if (value == null) {
            throw new IllegalArgumentException("Search value cannot be null");
        }
        
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams()
                .withLimitOffset(limit, offset);
            
            return eventDAO.findByEventDataContains(jsonPath, value, filterParams);
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to search event data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search event data", e);
        }
    }

    @Override
    public long countEventsByTransaction(UUID transactionId) {
        validateRequiredParameters(transactionId);
        
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            List<PaymentEvent> events = eventDAO.findByTransactionId(transactionId, filterParams);
            return events.size();
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to count events by transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to count events by transaction", e);
        }
    }

    @Override
    public long countEvents(String eventType, Instant startTime, Instant endTime, String createdBy) {
        try {
            PaymentFilterParams filterParams = new PaymentFilterParams();
            
            // Apply filters based on provided parameters
            List<PaymentEvent> events;
            
            if (eventType != null && !eventType.isEmpty()) {
                events = eventDAO.findByEventType(eventType, filterParams);
            } else if (startTime != null || endTime != null) {
                events = eventDAO.findByCreatedAtBetween(startTime, endTime, filterParams);
            } else if (createdBy != null && !createdBy.isEmpty()) {
                events = eventDAO.findByCreatedBy(createdBy, filterParams);
            } else {
                // No filters provided, return 0 to avoid retrieving all events
                return 0;
            }
            
            // Apply additional filtering in memory
            return events.stream()
                .filter(event -> {
                    boolean matchesType = eventType == null || eventType.isEmpty() || 
                                         event.getEventType().equals(eventType);
                    boolean afterStart = startTime == null || 
                                        !event.getCreatedAt().isBefore(startTime);
                    boolean beforeEnd = endTime == null || 
                                       !event.getCreatedAt().isAfter(endTime);
                    boolean matchesCreator = createdBy == null || createdBy.isEmpty() || 
                                            event.getCreatedBy().equals(createdBy);
                    
                    return matchesType && afterStart && beforeEnd && matchesCreator;
                })
                .count();
        } catch (ConnectionException | QueryExecutionException e) {
            logger.error("Failed to count events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to count events", e);
        }
    }

    /**
     * Builds a timeline representation from a list of events.
     *
     * @param events The list of events to include in the timeline
     * @return A list of timeline entries with event details
     */
    private List<Map<String, Object>> buildTimelineFromEvents(List<PaymentEvent> events) {
        return events.stream()
            .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
            .map(event -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("eventId", event.getEventId().toString());
                entry.put("timestamp", event.getCreatedAt());
                entry.put("type", event.getEventType());
                entry.put("actor", event.getCreatedBy());
                
                if (event.getEventType().equals("STATUS_CHANGE")) {
                    entry.put("previousStatus", event.getPreviousStatus());
                    entry.put("newStatus", event.getNewStatus());
                }
                
                if (event.getEventData() != null && !event.getEventData().isEmpty()) {
                    entry.put("data", event.getEventData());
                }
                
                if (event.getCorrelationId() != null) {
                    entry.put("correlationId", event.getCorrelationId().toString());
                }
                
                return entry;
            })
            .collect(Collectors.toList());
    }

    /**
     * Validates that required parameters are not null.
     *
     * @param params The parameters to validate
     * @throws IllegalArgumentException if any parameter is null
     */
    private void validateRequiredParameters(Object... params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                throw new IllegalArgumentException("Required parameter at position " + i + " cannot be null");
            }
            
            if (params[i] instanceof String && ((String) params[i]).trim().isEmpty()) {
                throw new IllegalArgumentException("Required string parameter at position " + i + " cannot be empty");
            }
        }
    }
}