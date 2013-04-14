package de.nierbeck.camel.exam.demo.control.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

final public class OutMessageProcessor implements Processor {
	public void process(Exchange exchange)
			throws Exception {
		exchange.getOut().setBody(exchange.getIn().getMessageId());
	}
}