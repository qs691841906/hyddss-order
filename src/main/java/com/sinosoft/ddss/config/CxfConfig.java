package com.sinosoft.ddss.config;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sinosoft.ddss.service.OrderWebService;


@Configuration
public class CxfConfig {
	@Autowired
	private Bus bus;
	
	@Autowired
	OrderWebService orderWebService;

	/** JAX-WS **/
	@Bean
	public Endpoint endpoint() {
		EndpointImpl endpoint = new EndpointImpl(bus, orderWebService);
		endpoint.publish("/OrderWebService");
		return endpoint;
	}
	
}
