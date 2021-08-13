package org.openpaas.servicebroker.cubrid.service.impl;

import java.util.Map;

import org.json.JSONObject;
import org.openpaas.servicebroker.cubrid.exception.CubridServiceException;
import org.openpaas.servicebroker.cubrid.model.CubridServiceInstance;
import org.openpaas.servicebroker.cubrid.model.CubridServiceInstanceBinding;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.openpaas.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.openpaas.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CubridServiceInstanceBindingService implements ServiceInstanceBindingService {
	private static final Logger logger = LoggerFactory.getLogger(CubridServiceInstanceBindingService.class);
	
	@Autowired
	private CubridAdminService cubridAdminService;

	@Override
	public CubridServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest) throws ServiceInstanceBindingExistsException, ServiceBrokerException {
		
		CubridServiceInstanceBinding findServiceInstanceBinding = cubridAdminService.findServiceBindInfo(createServiceInstanceBindingRequest.getBindingId());
		
		if (findServiceInstanceBinding != null) {
			if (createServiceInstanceBindingRequest.getBindingId().equals(findServiceInstanceBinding.getId())
					&& createServiceInstanceBindingRequest.getServiceInstanceId().equals(findServiceInstanceBinding.getServiceInstanceId())
					&& createServiceInstanceBindingRequest.getAppGuid().equals(findServiceInstanceBinding.getAppGuid())) {
				logger.error("Service instance bind already exists. : " + findServiceInstanceBinding.getId());
				
				findServiceInstanceBinding.setHttpStatusOK();
				
				return findServiceInstanceBinding;
			} else {
				logger.error("Service instance bind information does not match. : " + findServiceInstanceBinding.getId());
				
				throw new ServiceInstanceBindingExistsException(findServiceInstanceBinding);
			}
		} else { // findServiceInstanceBinding == null
			logger.info("Start binding service instance. : " + createServiceInstanceBindingRequest.getBindingId());
			
			CubridServiceInstance findServiceInstance = cubridAdminService.findServiceInstanceInfo(createServiceInstanceBindingRequest.getServiceInstanceId());
			
			String serviceInstanceId = createServiceInstanceBindingRequest.getServiceInstanceId();
			String serviceInstanceBindId = createServiceInstanceBindingRequest.getBindingId();
			
			if (findServiceInstance != null) { // (findServiceInstanceBinding == null) && (findServiceInstance != null)
				String dbname = findServiceInstance.getDatabaseName();
				String dbapass = cubridAdminService.getDbaPassword(dbname);
				String username = cubridAdminService.createUsername(serviceInstanceBindId);
				String password = cubridAdminService.createRandomPassword();
				
				Map<String, Object> credentials = cubridAdminService.getCredentials(dbname, username, password);
				
				CubridServiceInstanceBinding createServiceInstanceBinding = new CubridServiceInstanceBinding(
						serviceInstanceBindId, serviceInstanceId, credentials, null,
						createServiceInstanceBindingRequest.getAppGuid());

				logger.info("Create user. : " + serviceInstanceBindId + " (" + dbname + " - " + username + ")");
				cubridAdminService.doCreateUser(dbname, dbapass, username, password);
				
				logger.info("Save service instance bind information. : " + serviceInstanceBindId + " (" + dbname + " - " + username + ")");
				createServiceInstanceBinding.setDatabaseUserName(username);
				cubridAdminService.saveBind(createServiceInstanceBinding);
				
				logger.info("Service instance binding complete. : " + serviceInstanceBindId);

				return createServiceInstanceBinding;
			} else { // (findServiceInstanceBinding == null) && (findServiceInstance == null)
				logger.error("Service instance does not exist. : " + serviceInstanceId);
				
				throw new ServiceBrokerException("Service instance does not exist. : " + serviceInstanceId);
			}
		}
	}
	
	@Override
	public CubridServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) throws ServiceBrokerException {
		CubridServiceInstanceBinding findServiceInstanceBinding = cubridAdminService.findServiceBindInfo(deleteServiceInstanceBindingRequest.getBindingId());
		
		if (findServiceInstanceBinding != null) {
			logger.info("Start deleting service instance binds. : " + deleteServiceInstanceBindingRequest.getBindingId());
			
			CubridServiceInstance findServiceInstance = cubridAdminService.findServiceInstanceInfo(findServiceInstanceBinding.getServiceInstanceId());
			
			String serviceInstanceBindId = findServiceInstanceBinding.getId();
			
			if (findServiceInstance != null) { // (findServiceInstanceBinding != null) && (findServiceInstance != null)
				String dbname = findServiceInstance.getDatabaseName();
				String dbapass = cubridAdminService.getDbaPassword(dbname);
				String username = findServiceInstanceBinding.getDatabaseUserName();
				
				logger.info("Delete user object. : " + serviceInstanceBindId + " (" + dbname + " - " + username + ")");
				cubridAdminService.dropOwnerObject(serviceInstanceBindId);
				
				logger.info("Delete user. : " + serviceInstanceBindId + " (" + dbname + " - " + username + ")");
				cubridAdminService.doDeleteUser(dbname, dbapass, username);
				
				logger.info("Delete service instance bind information. : " + serviceInstanceBindId);
				cubridAdminService.deleteBind(serviceInstanceBindId);
				
				logger.info("Service instance bind deletion complete. : " + serviceInstanceBindId);
				
				return findServiceInstanceBinding;
			} else { // (findServiceInstanceBinding != null) && (findServiceInstance == null)
				logger.error("Service instance bind information does not match. : " + serviceInstanceBindId);
				
				logger.info("Delete invalid service instance bind. : " + serviceInstanceBindId);
				cubridAdminService.deleteBind(serviceInstanceBindId);
				
				logger.info("Invalid service instance bind deletion complete. : " + serviceInstanceBindId);
				
				return null;
			}
		} else { // findServiceInstanceBinding == null
			logger.error("Service instance bind does not exist. : " + deleteServiceInstanceBindingRequest.getBindingId());
			
			return null;
		}
	}
}
