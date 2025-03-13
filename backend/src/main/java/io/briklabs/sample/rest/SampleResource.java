package io.briklabs.sample.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.briklabs.sample.config.SampleDatabaseConfig;

@Path("sample")
public class SampleResource {

	@SuppressWarnings("unused")
	private SampleDatabaseConfig databaseConfig;

	public SampleResource(SampleDatabaseConfig databaseConfig) {
		this.databaseConfig = databaseConfig;
	}

	@GET
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	@Path("hello")
	public Response all(String payload) {

		// Use the database
		return Response.ok("Hello").build();
	}
}
