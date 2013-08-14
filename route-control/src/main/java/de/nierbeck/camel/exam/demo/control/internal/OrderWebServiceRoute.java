package de.nierbeck.camel.exam.demo.control.internal;

import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import de.nierbeck.camel.exam.demo.control.JmsDestinations;
import de.nierbeck.camel.exam.demo.control.RouteID;
import de.nierbeck.camel.exam.demo.control.internal.converter.MessageLogConverter;

public class OrderWebServiceRoute extends RouteBuilder{
	
	private String address;
	private String port;
	private String service;
	
	@Override
	public void configure() throws Exception {
		
		from("cxf:bean:messageService").routeId(RouteID.WEB_SERVICE_ORDER)
		.log(LoggingLevel.DEBUG, "Incoming Request: ${body}")
		.setBody(simple("${body[0]}"))
		.setHeader("MessageId").mvel("exchangeId")
		.wireTap("direct:logMessage")
		.to("activemq:queue:"+ JmsDestinations.QUEUE_MESSAGE_STORE+"?disableReplyTo=true")
		.setExchangePattern(ExchangePattern.InOut)
		.process(new OutMessageProcessor());

		from("direct:logMessage").routeId(RouteID.LOG_ORDER)
			.process(new MessageLogConverter())
			.to("jpa:de.nierbeck.camel.exam.demo.entities.CamelMessage");
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return the service
	 */
	public String getService() {
		return service;
	}

	/**
	 * @param service the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}

}
