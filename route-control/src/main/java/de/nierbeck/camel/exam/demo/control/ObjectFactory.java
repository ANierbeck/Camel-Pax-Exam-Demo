package de.nierbeck.camel.exam.demo.control;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	private static final QName _ORDER_QNAME = new QName("http://control.datacombination.tecdoc.de/", "order");
	
	@XmlElementDecl(namespace = "http://control.datacombination.tecdoc.de/", name="order")
	public JAXBElement<OrderDataMergingBean> createOrder(OrderDataMergingBean order) {
		return new JAXBElement<OrderDataMergingBean>(_ORDER_QNAME, OrderDataMergingBean.class, order);
	}
	
}