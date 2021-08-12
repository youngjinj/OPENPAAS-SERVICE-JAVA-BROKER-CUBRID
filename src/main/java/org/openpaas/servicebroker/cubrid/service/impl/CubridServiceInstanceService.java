package org.openpaas.servicebroker.cubrid.service.impl;

import org.json.JSONObject;
import org.openpaas.servicebroker.cubrid.exception.CubridServiceException;
import org.openpaas.servicebroker.cubrid.model.CubridServiceInstance;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceExistsException;
import org.openpaas.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;
import org.openpaas.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridServiceInstanceService implements ServiceInstanceService {
	private static final Logger logger = LoggerFactory.getLogger(CubridServiceInstanceService.class);
	
	@Autowired
	private CubridAdminService cubridAdminService;
	
	@Autowired
	private CubridManagerServerAPIService cubridManagerServerAPIService;

//	@Autowired
//	public CubridServiceInstanceService(CubridAdminService cubridAdminService, CubridManagerServerAPIService cubridHttpsURLService) {
//		this.cubridAdminService = cubridAdminService;
//		this.cubridManagerServerAPIService = cubridHttpsURLService;
//	}
	
	@Override
	public CubridServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) throws ServiceInstanceExistsException, ServiceBrokerException {

		CubridServiceInstance findServiceInstance = getServiceInstance(createServiceInstanceRequest.getServiceInstanceId());
		
		if (findServiceInstance != null) {
			if (createServiceInstanceRequest.getServiceInstanceId().equals(findServiceInstance.getServiceInstanceId())
					&& createServiceInstanceRequest.getPlanId().equals(findServiceInstance.getPlanId())
					&& createServiceInstanceRequest.getServiceDefinitionId().equals(findServiceInstance.getServiceDefinitionId())) {
				logger.error("Service instance already exists. : " + findServiceInstance.getServiceInstanceId());
				
				findServiceInstance.setHttpStatusOK();
				
				return findServiceInstance;
			} else {
				logger.error("Service instance creation information does not match. : " + findServiceInstance.getServiceInstanceId());
				
				throw new ServiceInstanceExistsException(findServiceInstance);
			}
		} else { // findServiceInstance == null
			logger.info("Start creating service instance. : " + createServiceInstanceRequest.getServiceInstanceId());
			
			CubridServiceInstance createServiceInstance = new CubridServiceInstance(createServiceInstanceRequest);
			
			String serviceInstanceId = createServiceInstanceRequest.getServiceInstanceId();
			
			String dbname = cubridAdminService.createDbname(serviceInstanceId);
			String dbapass = cubridAdminService.createDbaPassword(serviceInstanceId);
			
			JSONObject response = null;
			
			if (cubridAdminService.isExistsServiceBind(serviceInstanceId)) {
				logger.info("Delete invalid service instance bind. : " + serviceInstanceId);
				
				cubridAdminService.deleteBindByInstanceId(serviceInstanceId);
			}
			
			String status = cubridManagerServerAPIService.doStartinfo(dbname);
			
			if ("start".equals(status)) {
				logger.info("stop invalid database. : " + serviceInstanceId + " (" + dbname + ")");
				
				response = cubridManagerServerAPIService.doStopdb(dbname);
				if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
					logger.error("Failed to stop invalid database. : " + serviceInstanceId + " (" + dbname + ")");
					
					throw new CubridServiceException(cubridAdminService.getErrorMessage("service-create: stop old-instance", serviceInstanceId, dbname, response));
				}
			}
			
			if ("create".equals(status) || "start".equals(status)) {
				logger.info("Delete invalid database. : " + serviceInstanceId + " (" + dbname + ")");
				
				response = cubridManagerServerAPIService.doDeletedb(dbname);
				if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
					logger.error("Failed to delete invalid database. : " + serviceInstanceId + " (" + dbname + ")");
					
					throw new CubridServiceException(cubridAdminService.getErrorMessage("service-create: delete old-instance", serviceInstanceId, dbname, response));
				}
			}
			
			logger.info("Create database. : " + serviceInstanceId + " (" + dbname + ")");
			response = cubridManagerServerAPIService.doCreatedb(dbname);
			if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
				logger.error("Failed to create database. : " + serviceInstanceId + " (" + dbname + ")");
				
				throw new CubridServiceException(cubridAdminService.getErrorMessage("service-create: create new-instance", serviceInstanceId, dbname, response));
			}
			
			response = cubridManagerServerAPIService.doStartdb(dbname);
			logger.info("Start database. : " + serviceInstanceId + " (" + dbname + ")");
			if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
				logger.error("Failed to start database. : " + serviceInstanceId + " (" + dbname + ")");
				
				throw new CubridServiceException(cubridAdminService.getErrorMessage("service-create: start new-instance", serviceInstanceId, dbname, response));
			}
			
			response = cubridManagerServerAPIService.doUpdateuserDBAPassword(dbname, dbapass);
			logger.info("Change dba password. : " + serviceInstanceId + " (" + dbname + ")");
			if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
				logger.error("Failed to change dba password. : " + serviceInstanceId + " (" + dbname + ")");
				
				throw new CubridServiceException(cubridAdminService.getErrorMessage("service-create: change dba-password", serviceInstanceId, dbname, response));
			}
			
			if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == true)) {
				logger.info("Save database creation information. : " + serviceInstanceId + " (" + dbname + ")");
				
				createServiceInstance.setDatabaseName(dbname);
				cubridAdminService.save(createServiceInstance, dbapass);
			}
			
			logger.info("Service instance creation complete. : " + serviceInstanceId);
			
			return createServiceInstance;
		}
	}

	@Override
	public CubridServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) throws ServiceBrokerException {
		CubridServiceInstance findServiceInstance = getServiceInstance(deleteServiceInstanceRequest.getServiceInstanceId());

		if (findServiceInstance != null) {
			logger.info("Start deleting service instance. : " + findServiceInstance.getServiceInstanceId());
			
			String serviceInstanceId = findServiceInstance.getServiceInstanceId();
			String dbname = findServiceInstance.getDatabaseName();
			
			JSONObject response = null;
			
			if (cubridAdminService.isExistsServiceBind(serviceInstanceId)) {
				logger.info("Delete invalid service instance bind. : " + serviceInstanceId);
				
				cubridAdminService.deleteBind(serviceInstanceId);
			}
			
			String status = cubridManagerServerAPIService.doStartinfo(dbname);
			
			if ("start".equals(status)) {
				logger.info("stop database. : " + serviceInstanceId + " (" + dbname + ")");
				
				response = cubridManagerServerAPIService.doStopdb(dbname);
				if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
					logger.error("Failed to stop database. : " + serviceInstanceId + " (" + dbname + ")");
					
					throw new CubridServiceException(cubridAdminService.getErrorMessage("service-delete: stop instance", serviceInstanceId, dbname, response));
				}
			}
			
			if ("create".equals(status) || "start".equals(status)) {
				logger.info("Delete database. : " + serviceInstanceId + " (" + dbname + ")");
				
				response = cubridManagerServerAPIService.doDeletedb(dbname);
				if (response != null && response.get("status") != null && ("success".equals(response.get("status").toString()) == false)) {
					logger.error("Failed to delete database. : " + serviceInstanceId + " (" + dbname + ")");
					
					throw new CubridServiceException(cubridAdminService.getErrorMessage("service-delete: delete instance", serviceInstanceId, dbname, response));
				}
			}
			
			logger.info("Delete database creation information. : " + serviceInstanceId + " (" + dbname + ")");
			cubridAdminService.delete(findServiceInstance.getServiceInstanceId());
			
			logger.info("Service instance deletion complete. : " + serviceInstanceId);

			return findServiceInstance;
		} else { // findServiceInstance == null
			logger.error("Service instance does not exist. : " + deleteServiceInstanceRequest.getServiceInstanceId());
			
			return null;
		}
	}

	@Override
	public CubridServiceInstance getServiceInstance(String serviceInstanceId) {
		return cubridAdminService.findServiceInstanceInfo(serviceInstanceId);
	}

	@Override
	public CubridServiceInstance updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest) throws ServiceInstanceUpdateNotSupportedException {
		logger.info("Service instance (" + updateServiceInstanceRequest.getServiceInstanceId() + ") update. (Not supported.)");
		
		throw new ServiceInstanceUpdateNotSupportedException("Update of service instance is not supported.");
	}
}
