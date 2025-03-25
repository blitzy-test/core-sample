package io.briklabs.sample.rest.base;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * CORS filter implementation for cross-origin resource sharing.
 * Provides enhanced security for payment endpoints with explicit origin whitelisting
 * and specialized headers for payment operations.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

	// Whitelist of allowed origins instead of permissive wildcard
	private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
		"https://app.briklabs.io",
		"https://admin.briklabs.io",
		"https://merchant.briklabs.io",
		"http://localhost:3000" // Development environment only
	));

	// Payment-specific paths that require stricter CORS policies
	private static final Set<String> PAYMENT_PATHS = new HashSet<>(Arrays.asList(
		"/organizations",
		"/accounts",
		"/transactions"
	));

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		String origin = requestContext.getHeaderString("Origin");
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		
		// Apply origin restrictions instead of wildcard
		if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
			headers.add("Access-Control-Allow-Origin", origin);
		} else if (origin != null && origin.startsWith("http://localhost")) {
			// Special case for development environments
			headers.add("Access-Control-Allow-Origin", origin);
		}
		
		// Set credentials flag
		headers.add("Access-Control-Allow-Credentials", "true");
		
		// Add payment-specific headers to the allowed headers list
		headers.add("Access-Control-Allow-Headers", 
			"Origin, Content-Type, Accept, Authorization, X-Requested-With, X-Transaction-ID, X-Idempotency-Key, X-Merchant-ID");
		
		// Update allowed methods to include all methods needed for payment operations
		headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
		
		// Apply stricter CORS policies for payment endpoint paths
		String path = requestContext.getUriInfo().getPath();
		boolean isPaymentEndpoint = PAYMENT_PATHS.stream().anyMatch(path::contains);
		
		if (isPaymentEndpoint) {
			// Set cache duration for preflight requests to 24 hours (86400 seconds)
			headers.add("Access-Control-Max-Age", "86400");
			
			// Ensure Vary header is set to prevent caching issues
			headers.add("Vary", "Origin");
			
			// Additional security headers for payment endpoints
			headers.add("X-Content-Type-Options", "nosniff");
			headers.add("X-Frame-Options", "DENY");
			headers.add("Content-Security-Policy", "default-src 'self'");
		} else {
			// Standard endpoints get a shorter preflight cache time
			headers.add("Access-Control-Max-Age", "3600");
		}
	}
}