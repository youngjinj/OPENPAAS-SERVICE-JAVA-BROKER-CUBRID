package org.openpaas.servicebroker.cubrid.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.openpaas.servicebroker.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridCatalogService implements CatalogService {

	private Catalog catalog;
	private Map<String, ServiceDefinition> serviceDefinitions = new HashMap<String, ServiceDefinition>();

	@Autowired
	public CubridCatalogService(Catalog catalog) {
		this.catalog = catalog;
		initializeMap();
	}

	public Catalog getCatalog() {
		return this.catalog;
	}

	private void initializeMap() {
		for (ServiceDefinition serviceDefinition : this.catalog.getServiceDefinitions()) {
			this.serviceDefinitions.put(serviceDefinition.getId(), serviceDefinition);
		}
	}

	public ServiceDefinition getServiceDefinition(String serviceId) {
		return (ServiceDefinition) this.serviceDefinitions.get(serviceId);
	}
}