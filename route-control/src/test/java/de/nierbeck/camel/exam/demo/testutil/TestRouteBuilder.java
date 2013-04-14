package de.nierbeck.camel.exam.demo.testutil;

import org.apache.camel.builder.RouteBuilder;

import de.nierbeck.camel.exam.demo.control.JmsDestinations;

public class TestRouteBuilder extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		from("direct:start").id("testroute").to("cxf:bean:sendTest").to("log:response");
		
		from("activemq:queue:" + JmsDestinations.QUEUE_MESSAGE_STORE).to("mock:OrderRoute");
	}

}
