package io.briklabs.sample.rest.base;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;

/**
 * Response filter that ensures proper character encoding for all responses.
 * This filter adds UTF-8 charset to Content-Type headers when not already specified,
 * ensuring consistent character encoding across all API responses including payment-specific
 * endpoints that require proper currency symbol and international character rendering.
 */
public class CharsetResponseFilter implements ContainerResponseFilter {

    // Set of media types that should have charset specified
    private static final Set<String> CHARSET_REQUIRED_TYPES = new HashSet<>(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.APPLICATION_XML,
            // Payment-specific media types
            "application/vnd.payment+json",
            "application/vnd.payment.transaction+json",
            "application/vnd.payment.event+json"
    ));

    // Default charset for all responses
    private static final String DEFAULT_CHARSET = "utf-8";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        MediaType type = response.getMediaType();

        if (type != null) {
            String contentType = type.toString();
            
            // Check if this is a media type that requires charset and doesn't already have it
            if (requiresCharset(type) && !contentType.contains("charset")) {
                // Add UTF-8 charset to ensure proper currency symbol rendering
                contentType = contentType + ";charset=" + DEFAULT_CHARSET;
                response.getHeaders().putSingle("Content-Type", contentType);
            }
        }
    }
    
    /**
     * Determines if the given media type requires charset specification.
     * This method checks if the media type is one that should have charset
     * explicitly defined to ensure proper character rendering, particularly
     * for payment-related responses with currency symbols and international characters.
     *
     * @param mediaType The media type to check
     * @return true if charset should be specified, false otherwise
     */
    private boolean requiresCharset(MediaType mediaType) {
        // Check if this is a text-based or JSON-based media type that needs charset
        String baseType = mediaType.getType() + "/" + mediaType.getSubtype();
        
        // Check against our set of types requiring charset
        return CHARSET_REQUIRED_TYPES.contains(baseType) ||
               // Handle vendor-specific payment media types with parameters
               (baseType.startsWith("application/vnd.payment") && 
                baseType.endsWith("+json"));
    }
}