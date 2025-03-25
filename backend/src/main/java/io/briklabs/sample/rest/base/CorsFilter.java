package io.briklabs.sample.rest.base;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * CORS filter implementation for handling Cross-Origin Resource Sharing.
 * Implements stricter security policies for payment endpoints while maintaining
 * compatibility with the frontend application.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    // Explicitly whitelist allowed origins instead of using wildcard
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",     // Local development
            "http://localhost:5173",     // Vite dev server
            "https://app.example.com",   // Production frontend
            "https://staging.example.com" // Staging frontend
    );

    // Maximum age for preflight requests in seconds (24 hours)
    private static final String MAX_AGE = "86400";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        
        // Only allow whitelisted origins
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.add("Access-Control-Allow-Origin", origin);
        }
        
        // Set credentials allowed (needed for authentication)
        headers.add("Access-Control-Allow-Credentials", "true");
        
        // Add payment-specific headers to allowed headers list
        headers.add("Access-Control-Allow-Headers", 
                "Origin, Content-Type, Accept, Authorization, X-Requested-With, " +
                "X-Payment-ID, X-Transaction-ID, X-Merchant-ID, X-Account-ID");
        
        // Include all methods needed for payment operations
        headers.add("Access-Control-Allow-Methods", 
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        
        // Set max age for preflight requests to reduce OPTIONS traffic
        headers.add("Access-Control-Max-Age", MAX_AGE);
        
        // Apply stricter CORS policies for payment endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path != null && path.contains("/transactions")) {
            // For payment endpoints, we might want to be even more restrictive
            // For example, we could limit the allowed methods or headers further
            // depending on the specific security requirements
            
            // Example of restricting methods for payment endpoints if needed:
            // headers.putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            
            // We could also add additional security headers for payment endpoints
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");
            headers.add("Content-Security-Policy", "default-src 'self'");
        }
    }
}