package io.briklabs.sample.rest.base;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Response filter that ensures proper UTF-8 charset encoding for all responses.
 * Enhanced to support payment-specific JSON response formats and ensure proper
 * encoding for currency formatting (e.g., $123.43, €123.43, 123.43 AUD).
 */
public class CharsetResponseFilter implements ContainerResponseFilter {

    // Set of media types that should have charset=utf-8 appended
    private static final Set<String> CHARSET_REQUIRED_TYPES = new HashSet<>(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.APPLICATION_XML,
            "application/vnd.payment+json",  // Payment-specific media type
            "application/vnd.payment.transaction+json",  // Transaction-specific media type
            "application/vnd.payment.event+json"  // Event-specific media type
    ));

    // Default charset for all responses
    private static final String UTF8_CHARSET = "utf-8";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MediaType type = response.getMediaType();

        if (type != null) {
            String contentType = type.toString();
            
            // Check if this is a media type that requires charset specification
            // and if charset is not already specified
            if (requiresCharset(contentType) && !contentType.contains("charset")) {
                // For large payment transaction result sets, avoid string concatenation
                // and use a more efficient approach
                MediaType mediaTypeWithCharset = type.withCharset(UTF8_CHARSET);
                response.getHeaders().putSingle("Content-Type", mediaTypeWithCharset.toString());
            }
        }
    }

    /**
     * Determines if the given content type requires charset specification.
     * Optimized for performance with payment transaction result sets.
     * 
     * @param contentType The content type to check
     * @return true if charset should be specified, false otherwise
     */
    private boolean requiresCharset(String contentType) {
        // Extract the base media type without parameters
        String baseType = contentType;
        int paramIndex = contentType.indexOf(';');
        if (paramIndex > 0) {
            baseType = contentType.substring(0, paramIndex);
        }
        
        // Check if this is a type that requires charset
        return CHARSET_REQUIRED_TYPES.contains(baseType);
    }
}