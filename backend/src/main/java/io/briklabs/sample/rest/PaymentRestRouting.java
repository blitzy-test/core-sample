package io.briklabs.sample.rest;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import com.zaxxer.hikari.HikariDataSource;

import io.briklabs.sample.payments.rest.TransactionResource;
import io.briklabs.sample.payments.rest.TransactionProcessingResource;
import io.briklabs.sample.payments.rest.TransactionEventResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Routing configuration for payment-related REST endpoints.
 * This class serves as a bridge between the core REST application and the specialized payment endpoints,
 * mapping URI patterns to their respective resources.
 * <p>
 * The routing follows the pattern: /organizations/{org_id}/accounts/{account_id}/transactions/
 * with support for the special '_all' placeholder to retrieve data for all accounts or transactions.
 */
@Path("/organizations/{org_id}")
@Tag(name = "Payments", description = "Payment transaction operations")
public class PaymentRestRouting {

    @Context
    private ResourceContext resourceContext;
    
    private final HikariDataSource dataSource;
    
    /**
     * Creates a new PaymentRestRouting instance with the specified data source.
     * 
     * @param dataSource The HikariCP connection pool for payment data access
     */
    public PaymentRestRouting(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Routes requests to account-level payment endpoints.
     * 
     * @param organizationId The organization identifier
     * @return The account-level payment routing resource
     */
    @Path("/accounts/{account_id}")
    @Operation(
        summary = "Access account-level payment operations",
        description = "Routes to payment operations for a specific account within an organization."
    )
    public AccountPaymentRouting getAccountPaymentRouting(
            @Parameter(description = "Organization identifier", required = true)
            @PathParam("org_id") String organizationId) {
        
        return resourceContext.getResource(AccountPaymentRouting.class);
    }
    
    /**
     * Routes requests to organization-wide payment endpoints using the '_all' placeholder.
     * This allows querying transactions across all accounts within an organization.
     * 
     * @param organizationId The organization identifier
     * @return The transaction resource for organization-wide operations
     */
    @Path("/accounts/_all/transactions")
    @Operation(
        summary = "Access organization-wide payment transactions",
        description = "Routes to payment operations across all accounts within an organization using the '_all' placeholder."
    )
    public TransactionResource getOrganizationTransactions(
            @Parameter(description = "Organization identifier", required = true)
            @PathParam("org_id") String organizationId) {
        
        TransactionResource resource = resourceContext.getResource(TransactionResource.class);
        resource.setDataSource(dataSource);
        resource.setOrganizationId(organizationId);
        resource.setAllAccounts(true);
        return resource;
    }
    
    /**
     * Nested resource class for account-level payment routing.
     */
    @Path("/")
    public static class AccountPaymentRouting {
        
        @Context
        private ResourceContext resourceContext;
        
        private HikariDataSource dataSource;
        
        /**
         * Sets the data source for this routing resource.
         * 
         * @param dataSource The HikariCP connection pool for payment data access
         */
        public void setDataSource(HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        /**
         * Routes requests to transaction-level endpoints for a specific account.
         * 
         * @param organizationId The organization identifier
         * @param accountId The account identifier
         * @return The transaction resource for account-specific operations
         */
        @Path("/transactions")
        @Operation(
            summary = "Access account-specific payment transactions",
            description = "Routes to payment transaction operations for a specific account."
        )
        public TransactionResource getAccountTransactions(
                @Parameter(description = "Organization identifier", required = true)
                @PathParam("org_id") String organizationId,
                @Parameter(description = "Account identifier", required = true)
                @PathParam("account_id") String accountId) {
            
            TransactionResource resource = resourceContext.getResource(TransactionResource.class);
            resource.setDataSource(dataSource);
            resource.setOrganizationId(organizationId);
            resource.setAccountId(accountId);
            return resource;
        }
        
        /**
         * Routes requests to transaction processing endpoints for a specific transaction.
         * 
         * @param organizationId The organization identifier
         * @param accountId The account identifier
         * @param transactionId The transaction identifier
         * @return The transaction processing resource for lifecycle operations
         */
        @Path("/transactions/{transaction_id}")
        @Operation(
            summary = "Access specific transaction operations",
            description = "Routes to operations for a specific transaction including retrieval, processing, capturing, and refunding."
        )
        public TransactionProcessingResource getTransactionProcessing(
                @Parameter(description = "Organization identifier", required = true)
                @PathParam("org_id") String organizationId,
                @Parameter(description = "Account identifier", required = true)
                @PathParam("account_id") String accountId,
                @Parameter(description = "Transaction identifier", required = true)
                @PathParam("transaction_id") String transactionId) {
            
            TransactionProcessingResource resource = resourceContext.getResource(TransactionProcessingResource.class);
            resource.setDataSource(dataSource);
            resource.setOrganizationId(organizationId);
            resource.setAccountId(accountId);
            resource.setTransactionId(transactionId);
            return resource;
        }
        
        /**
         * Routes requests to transaction event history endpoints for a specific transaction.
         * 
         * @param organizationId The organization identifier
         * @param accountId The account identifier
         * @param transactionId The transaction identifier
         * @return The transaction event resource for event history operations
         */
        @Path("/transactions/{transaction_id}/events")
        @Operation(
            summary = "Access transaction event history",
            description = "Routes to operations for retrieving the event history of a specific transaction."
        )
        public TransactionEventResource getTransactionEvents(
                @Parameter(description = "Organization identifier", required = true)
                @PathParam("org_id") String organizationId,
                @Parameter(description = "Account identifier", required = true)
                @PathParam("account_id") String accountId,
                @Parameter(description = "Transaction identifier", required = true)
                @PathParam("transaction_id") String transactionId) {
            
            TransactionEventResource resource = resourceContext.getResource(TransactionEventResource.class);
            resource.setDataSource(dataSource);
            resource.setOrganizationId(organizationId);
            resource.setAccountId(accountId);
            resource.setTransactionId(transactionId);
            return resource;
        }
    }
}