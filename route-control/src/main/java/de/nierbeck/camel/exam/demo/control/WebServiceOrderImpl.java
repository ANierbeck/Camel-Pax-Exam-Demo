package de.nierbeck.camel.exam.demo.control;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_XML)
@WebService(endpointInterface="de.tecdoc.datacombination.control.OrderDataMerging")
public class WebServiceOrderImpl implements WebServiceOrder {

	@Override
	public String storeMessage(@WebParam(name = "order") CamelMessageBean order) {
		return "42";
	}


}