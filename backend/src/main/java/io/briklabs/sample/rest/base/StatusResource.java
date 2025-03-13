package io.briklabs.sample.rest.base;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("up")
public class StatusResource {

	private final long start;

	public StatusResource() {
		start = System.currentTimeMillis();
	}

	@GET
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Response get() {
		long uptime = System.currentTimeMillis() - start;
		return Response.ok(uptime).build();
	}

}
