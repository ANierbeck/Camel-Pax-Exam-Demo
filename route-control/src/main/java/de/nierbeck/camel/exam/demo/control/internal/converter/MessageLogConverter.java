package de.nierbeck.camel.exam.demo.control.internal.converter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import de.nierbeck.camel.exam.demo.control.CamelMessageBean;
import de.nierbeck.camel.exam.demo.entities.CamelMessage;

public class MessageLogConverter implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		CamelMessageBean messageBean = exchange.getIn().getBody(CamelMessageBean.class);
		
		CamelMessage order = new CamelMessage();
		
		order.setId(exchange.getIn().getHeader("MessageId", String.class));
		order.setTimeStamp(Long.parseLong(messageBean.getTmstamp()));
		order.setMessage(messageBean.getMessage());

		exchange.getIn().setBody(order, CamelMessage.class);
	}

}
