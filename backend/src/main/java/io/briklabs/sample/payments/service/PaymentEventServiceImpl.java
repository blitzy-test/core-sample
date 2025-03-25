package io.briklabs.sample.payments.service;

import io.briklabs.sample.payments.data.dao.PaymentEventDAO;
import io.briklabs.sample.payments.data.dao.PaymentDAOFactory;
import io.briklabs.sample.payments.model.PaymentEvent;
import io.briklabs.sample.payments.model.PaymentStatus;
import io.briklabs.sample.payments.model.PaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the PaymentEventService interface that manages payment lifecycle event tracking.
 * This class handles the recording of events at each transaction lifecycle stage, constructs event
 * timelines, and provides comprehensive event history for audit and reporting purposes.
 */
public class PaymentEventServiceImpl implements PaymentEventService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventServiceImpl.class);
    
    private final PaymentEventDAO eventDAO;
    
    /**
     * Constructs a new PaymentEventServiceImpl with the specified DAO factory.
     *
     * @param daoFactory The factory for creating data access objects
     */
    public PaymentEventServiceImpl(PaymentDAOFactory daoFactory) {
        this.eventDAO = daoFactory.getPaymentEventDAO();
        logger.info("PaymentEventServiceImpl initialized");
    }
    
    /**
     * Constructs a new PaymentEventServiceImpl with the specified event DAO.
     * This constructor is primarily used for testing with mock DAOs.
     *
     * @param eventDAO The payment event DAO implementation
     */
    public PaymentEventServiceImpl(PaymentEventDAO eventDAO) {
        this.eventDAO = eventDAO;
        logger.info("PaymentEventServiceImpl initialized with provided DAO");
    }

    @Override
    public PaymentEvent recordEvent(PaymentEvent event) {
        logger.debug("Recording payment event: {}", event);
        
        // Validate the event data
        try {
            event.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid event data: {}", e.getMessage());
            throw e;
        }
        
        // Ensure event ID is set
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        
        // Ensure timestamp is set
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(Instant.now());
        }
        
        // Persist the event
        try {
            PaymentEvent savedEvent = eventDAO.create(event);
            logger.info("Recorded payment event: {} for transaction: {}", 
                    savedEvent.getEventType(), savedEvent.getTransactionId());
            return savedEvent;
        } catch (Exception e) {
            logger.error("Failed to record payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record payment event", e);
        }
    }

    @Override
    public PaymentEvent recordStatusChangeEvent(PaymentTransaction transaction, PaymentStatus newStatus, String userId) {
        logger.debug("Recording status change event for transaction {}: {} -> {}", 
                transaction.getTransactionId(), transaction.getStatus(), newStatus);
        
        // Create a status change event
        PaymentEvent event = PaymentEvent.createStatusChangeEvent(transaction, newStatus, userId);
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordTransactionCreatedEvent(PaymentTransaction transaction, String userId) {
        logger.debug("Recording transaction created event for transaction {}", transaction.getTransactionId());
        
        // Create a transaction created event
        PaymentEvent event = PaymentEvent.createTransactionCreatedEvent(transaction, userId);
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordProcessingEvent(PaymentTransaction transaction, String userId, Map<String, String> metadata) {
        logger.debug("Recording processing event for transaction {}", transaction.getTransactionId());
        
        // Convert metadata to JSON string
        String eventData = convertMetadataToJson(metadata);
        
        // Create a processing event
        PaymentEvent event = PaymentEvent.createProcessingEvent(transaction, userId, eventData);
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordCaptureEvent(PaymentTransaction transaction, String userId, String amount, boolean isPartial) {
        logger.debug("Recording {} capture event for transaction {}: amount={}", 
                isPartial ? "partial" : "full", transaction.getTransactionId(), amount);
        
        // Create a capture event
        PaymentEvent event = PaymentEvent.createCaptureEvent(transaction, userId, amount);
        
        // Set event type based on partial flag
        event.setEventType(isPartial ? "PARTIAL_CAPTURE_INITIATED" : "CAPTURE_INITIATED");
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordRefundEvent(PaymentTransaction transaction, String userId, String amount, String reason, boolean isPartial) {
        logger.debug("Recording {} refund event for transaction {}: amount={}, reason='{}'", 
                isPartial ? "partial" : "full", transaction.getTransactionId(), amount, reason);
        
        // Create a refund event
        PaymentEvent event = PaymentEvent.createRefundEvent(transaction, userId, amount, reason);
        
        // Set event type based on partial flag
        event.setEventType(isPartial ? "PARTIAL_REFUND_INITIATED" : "REFUND_INITIATED");
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordVoidEvent(PaymentTransaction transaction, String userId, String reason) {
        logger.debug("Recording void event for transaction {}: reason='{}'", 
                transaction.getTransactionId(), reason);
        
        // Create a void event
        PaymentEvent event = new PaymentEvent(transaction.getTransactionId(), "VOID_INITIATED", userId);
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData("{\"reason\":\"" + escapeJsonString(reason) + "\"}");
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordErrorEvent(PaymentTransaction transaction, String userId, String errorCode, String errorMessage) {
        logger.debug("Recording error event for transaction {}: code={}, message='{}'", 
                transaction.getTransactionId(), errorCode, errorMessage);
        
        // Create an error event
        PaymentEvent event = PaymentEvent.createErrorEvent(transaction, userId, errorCode, errorMessage);
        
        return recordEvent(event);
    }

    @Override
    public PaymentEvent recordCustomEvent(PaymentTransaction transaction, String eventType, String userId, Map<String, String> metadata) {
        logger.debug("Recording custom event '{}' for transaction {}", eventType, transaction.getTransactionId());
        
        // Convert metadata to JSON string
        String eventData = convertMetadataToJson(metadata);
        
        // Create a custom event
        PaymentEvent event = new PaymentEvent(transaction.getTransactionId(), eventType, userId);
        event.setPreviousStatus(transaction.getStatus().name());
        event.setEventData(eventData);
        
        return recordEvent(event);
    }

    @Override
    public List<PaymentEvent> getEventsByTransactionId(UUID transactionId) {
        logger.debug("Retrieving events for transaction {}", transactionId);
        
        try {
            List<PaymentEvent> events = eventDAO.findByTransactionId(transactionId);
            logger.debug("Retrieved {} events for transaction {}", events.size(), transactionId);
            return events;
        } catch (Exception e) {
            logger.error("Failed to retrieve events for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transaction events", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByTransactionIdAndTimeRange(UUID transactionId, Instant startTime, Instant endTime) {
        logger.debug("Retrieving events for transaction {} between {} and {}", 
                transactionId, startTime, endTime);
        
        try {
            List<PaymentEvent> events = eventDAO.findByTransactionIdAndTimeRange(transactionId, startTime, endTime);
            logger.debug("Retrieved {} events for transaction {} in time range", events.size(), transactionId);
            return events;
        } catch (Exception e) {
            logger.error("Failed to retrieve events for transaction {} in time range: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transaction events by time range", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByTransactionIdAndType(UUID transactionId, String eventType) {
        logger.debug("Retrieving events of type '{}' for transaction {}", eventType, transactionId);
        
        try {
            List<PaymentEvent> events = eventDAO.findByTransactionIdAndType(transactionId, eventType);
            logger.debug("Retrieved {} events of type '{}' for transaction {}", 
                    events.size(), eventType, transactionId);
            return events;
        } catch (Exception e) {
            logger.error("Failed to retrieve events of type '{}' for transaction {}: {}", 
                    eventType, transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transaction events by type", e);
        }
    }

    @Override
    public List<PaymentEvent> buildTransactionTimeline(UUID transactionId) {
        logger.debug("Building timeline for transaction {}", transactionId);
        
        try {
            // Retrieve all events for the transaction
            List<PaymentEvent> events = getEventsByTransactionId(transactionId);
            
            // Sort events chronologically (oldest first)
            List<PaymentEvent> timeline = events.stream()
                    .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
                    .collect(Collectors.toList());
            
            logger.debug("Built timeline with {} events for transaction {}", timeline.size(), transactionId);
            return timeline;
        } catch (Exception e) {
            logger.error("Failed to build timeline for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to build transaction timeline", e);
        }
    }

    @Override
    public List<PaymentEvent> buildFilteredTransactionTimeline(UUID transactionId, List<String> eventTypes, 
                                                             Instant startTime, Instant endTime) {
        logger.debug("Building filtered timeline for transaction {}", transactionId);
        
        try {
            // Retrieve all events for the transaction
            List<PaymentEvent> allEvents = getEventsByTransactionId(transactionId);
            
            // Apply filters
            List<PaymentEvent> filteredEvents = allEvents.stream()
                    .filter(event -> eventTypes == null || eventTypes.isEmpty() || 
                            eventTypes.contains(event.getEventType()))
                    .filter(event -> startTime == null || !event.getCreatedAt().isBefore(startTime))
                    .filter(event -> endTime == null || !event.getCreatedAt().isAfter(endTime))
                    .sorted(Comparator.comparing(PaymentEvent::getCreatedAt))
                    .collect(Collectors.toList());
            
            logger.debug("Built filtered timeline with {} events for transaction {}", 
                    filteredEvents.size(), transactionId);
            return filteredEvents;
        } catch (Exception e) {
            logger.error("Failed to build filtered timeline for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to build filtered transaction timeline", e);
        }
    }

    @Override
    public PaymentEvent getMostRecentEvent(UUID transactionId) {
        logger.debug("Retrieving most recent event for transaction {}", transactionId);
        
        try {
            PaymentEvent event = eventDAO.findMostRecentByTransactionId(transactionId);
            if (event != null) {
                logger.debug("Retrieved most recent event of type '{}' for transaction {}", 
                        event.getEventType(), transactionId);
            } else {
                logger.debug("No events found for transaction {}", transactionId);
            }
            return event;
        } catch (Exception e) {
            logger.error("Failed to retrieve most recent event for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve most recent transaction event", e);
        }
    }

    @Override
    public PaymentEvent getMostRecentEventByType(UUID transactionId, String eventType) {
        logger.debug("Retrieving most recent event of type '{}' for transaction {}", 
                eventType, transactionId);
        
        try {
            PaymentEvent event = eventDAO.findMostRecentByTransactionIdAndType(transactionId, eventType);
            if (event != null) {
                logger.debug("Retrieved most recent event of type '{}' for transaction {}", 
                        eventType, transactionId);
            } else {
                logger.debug("No events of type '{}' found for transaction {}", 
                        eventType, transactionId);
            }
            return event;
        } catch (Exception e) {
            logger.error("Failed to retrieve most recent event of type '{}' for transaction {}: {}", 
                    eventType, transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve most recent transaction event by type", e);
        }
    }

    @Override
    public int countEventsByTransactionId(UUID transactionId) {
        logger.debug("Counting events for transaction {}", transactionId);
        
        try {
            int count = eventDAO.countByTransactionId(transactionId);
            logger.debug("Counted {} events for transaction {}", count, transactionId);
            return count;
        } catch (Exception e) {
            logger.error("Failed to count events for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to count transaction events", e);
        }
    }

    @Override
    public int countEventsByTransactionIdAndType(UUID transactionId, String eventType) {
        logger.debug("Counting events of type '{}' for transaction {}", eventType, transactionId);
        
        try {
            int count = eventDAO.countByTransactionIdAndType(transactionId, eventType);
            logger.debug("Counted {} events of type '{}' for transaction {}", 
                    count, eventType, transactionId);
            return count;
        } catch (Exception e) {
            logger.error("Failed to count events of type '{}' for transaction {}: {}", 
                    eventType, transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to count transaction events by type", e);
        }
    }

    @Override
    public boolean hasEventType(UUID transactionId, String eventType) {
        logger.debug("Checking if transaction {} has events of type '{}'", transactionId, eventType);
        
        try {
            boolean hasEvents = eventDAO.existsByTransactionIdAndType(transactionId, eventType);
            logger.debug("Transaction {} {} events of type '{}'", 
                    transactionId, hasEvents ? "has" : "does not have", eventType);
            return hasEvents;
        } catch (Exception e) {
            logger.error("Failed to check if transaction {} has events of type '{}': {}", 
                    transactionId, eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to check transaction events by type", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByCorrelationId(UUID correlationId) {
        logger.debug("Retrieving events with correlation ID {}", correlationId);
        
        try {
            List<PaymentEvent> events = eventDAO.findByCorrelationId(correlationId);
            logger.debug("Retrieved {} events with correlation ID {}", events.size(), correlationId);
            return events;
        } catch (Exception e) {
            logger.error("Failed to retrieve events with correlation ID {}: {}", 
                    correlationId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by correlation ID", e);
        }
    }

    @Override
    public List<PaymentEvent> getEventsByUserId(String userId, int limit, int offset) {
        logger.debug("Retrieving events created by user '{}' (limit={}, offset={})", 
                userId, limit, offset);
        
        try {
            List<PaymentEvent> events = eventDAO.findByCreatedBy(userId, limit, offset);
            logger.debug("Retrieved {} events created by user '{}'", events.size(), userId);
            return events;
        } catch (Exception e) {
            logger.error("Failed to retrieve events created by user '{}': {}", 
                    userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve events by user ID", e);
        }
    }

    @Override
    public int purgeOldEvents(UUID transactionId, int retentionPeriod) {
        logger.warn("Purging events older than {} days for transaction {}", 
                retentionPeriod, transactionId);
        
        if (retentionPeriod <= 0) {
            throw new IllegalArgumentException("Retention period must be positive");
        }
        
        try {
            // Calculate cutoff date
            Instant cutoffDate = Instant.now().minusSeconds(retentionPeriod * 86400L);
            
            // Purge events
            int purgedCount = eventDAO.deleteByTransactionIdAndCreatedBefore(transactionId, cutoffDate);
            
            logger.info("Purged {} events older than {} days for transaction {}", 
                    purgedCount, retentionPeriod, transactionId);
            
            return purgedCount;
        } catch (Exception e) {
            logger.error("Failed to purge old events for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to purge old transaction events", e);
        }
    }
    
    /**
     * Converts a map of metadata to a JSON string.
     *
     * @param metadata The metadata map
     * @return JSON string representation of the metadata
     */
    private String convertMetadataToJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            json.append("\"").append(escapeJsonString(entry.getValue())).append("\"");
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escapes special characters in a string for JSON encoding.
     *
     * @param input The input string
     * @return The escaped string
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '/':
                    escaped.append("\\/");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
            }
        }
        
        return escaped.toString();
    }
}