package de.nierbeck.camel.exam.demo.control;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface WebServiceOrder {
	   
    public String storeMessage(@WebParam(name = "order") CamelMessageBean message);
    
}