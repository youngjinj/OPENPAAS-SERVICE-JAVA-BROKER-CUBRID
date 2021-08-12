package org.openpaas.servicebroker.cubrid.model;

import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;

public class CubridServiceInstance extends ServiceInstance {

	private String databaseName;

	public CubridServiceInstance(CreateServiceInstanceRequest request) {
		super(request);
	}

	public CubridServiceInstance(DeleteServiceInstanceRequest request) {
		super(request);
	}

	public CubridServiceInstance(UpdateServiceInstanceRequest request) {
		super(request);
	}

	public CubridServiceInstance() {
		super(new CreateServiceInstanceRequest());
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

}
