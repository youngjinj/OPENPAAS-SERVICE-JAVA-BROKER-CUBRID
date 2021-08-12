package org.openpaas.servicebroker.cubrid.exception;

import org.openpaas.servicebroker.exception.ServiceBrokerException;

public class CubridServiceException extends ServiceBrokerException {
	
	private static final long serialVersionUID = 3174340055788029498L;

	public CubridServiceException(String message) {
		super(message);
	}
}
