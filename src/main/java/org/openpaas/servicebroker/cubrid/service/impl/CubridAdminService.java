package org.openpaas.servicebroker.cubrid.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.json.JSONObject;
import org.openpaas.servicebroker.cubrid.model.CubridServiceInstance;
import org.openpaas.servicebroker.cubrid.model.CubridServiceInstanceBinding;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.google.common.hash.Hashing;

@Service
public class CubridAdminService {
	private Logger logger = LoggerFactory.getLogger(CubridAdminService.class);
	
	private static final String SERVICE_DRIVER_CLASS_NAME = "cubrid.jdbc.driver.CUBRIDDriver";
	private static final String SERVICE_INSTANCE_IP = "45.248.73.54";
	// private static final String SERVICE_INSTANCE_IP = "192.168.2.205";
	private static final String SERVICE_INSTANCE_PORT = "33000";
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	public String createDbname(String serviceInstanceId) {
		return "db_" + Hashing.sha256().hashString(serviceInstanceId, StandardCharsets.UTF_8).toString().substring(0, 12);
	}

	public String createUsername(String serviceInstanceBindId) {
		return "user_" + Hashing.sha256().hashString(serviceInstanceBindId, StandardCharsets.UTF_8).toString().substring(0, 10);
	}
	
	public String createDbaPassword(String serviceInstanceId) {
		return Hashing.sha256().hashString("dba" + serviceInstanceId, StandardCharsets.UTF_8).toString().substring(0, 15);
	}
	
	public String getDbaPassword(String dbname) {
		String sql = "/* getDbaPassword */ SELECT [password] FROM [credentials_info] WHERE [name] = :dbname AND [username] = 'dba'";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("dbname", dbname);
		
		try {
			return namedParameterJdbcTemplate.queryForObject(sql, param, String.class);
		} catch (DataAccessException e) {
			// Not error.
			logger.info("DBA has no password. : " + dbname);
		}
		
		return null;
	}
	
	public String createRandomPassword() {
		final int length = 16;

		String AlphabetNumberSpecialCharacter = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "abcdefghijklmnopqrstuvwxyz"
				+ "0123456789";

		StringBuilder password = new StringBuilder();

		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(secureRandom.generateSeed(length));
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getLocalizedMessage(), e);
		}

		for (int i = 0; i < length; i++) {
			int index = secureRandom.nextInt(AlphabetNumberSpecialCharacter.length());
			password.append(AlphabetNumberSpecialCharacter.charAt(index));
		}

		return password.toString();
	}
	
	public Map<String, Object> getCredentials(String dbname, String username, String password) {
		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("driverClassName", CubridAdminService.SERVICE_DRIVER_CLASS_NAME);
		credentials.put("name", dbname);
		credentials.put("hostname", CubridAdminService.SERVICE_INSTANCE_IP);
		credentials.put("port", CubridAdminService.SERVICE_INSTANCE_PORT);
		credentials.put("username", username);
		credentials.put("password", password);
		
		StringBuilder uri = new StringBuilder();
		uri.append("cubrid").append(":");
		uri.append(CubridAdminService.SERVICE_INSTANCE_IP).append(":");
		uri.append(CubridAdminService.SERVICE_INSTANCE_PORT).append(":");
		uri.append(dbname).append(":");
		uri.append(username).append(":");
		uri.append(password).append(":");
		
		StringBuilder jdbcUrl = new StringBuilder();
		jdbcUrl.append("jdbc").append(":");
		jdbcUrl.append(uri.toString());
		
		credentials.put("uri", uri.toString());
		credentials.put("jdbcUrl", jdbcUrl.toString());
		
		return credentials;
	}
	
	public Map<String, Object> getCredentialsInfo(String serviceInstanceBindId) {
		String sql = "/* getCredentialsInfo */ SELECT [service_instance_id], [service_instance_bind_id], [name], [hostname], [port], [username], [password], [uri], [jdbcurl] FROM [credentials_info] WHERE [service_instance_bind_id] = :serviceInstanceBindId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceBindId", serviceInstanceBindId);
		
		Map<String, Object> credentials = null;
		try {
			credentials = namedParameterJdbcTemplate.queryForObject(sql, param, new RowMapper<Map<String, Object>>() {
				@Override
				public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
					Map<String, Object> credentials = new HashMap<String, Object>();

					credentials.put("driverClassName", CubridAdminService.SERVICE_DRIVER_CLASS_NAME);
					credentials.put("name", rs.getString("name"));
					credentials.put("hostname", rs.getString("hostname"));
					credentials.put("port", rs.getString("port"));
					credentials.put("username", rs.getString("username"));
					credentials.put("password", rs.getString("password"));
					credentials.put("uri", rs.getString("uri"));
					credentials.put("jdbcUrl", rs.getString("jdbcurl"));

					return credentials;
				}
			});
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}

		return credentials;
	}
	
	public CubridServiceInstance findServiceInstanceInfo(String serviceInstanceId) {
		String sql = "/* findServiceInstanceInfo */ SELECT [service_instance_id], [Service_definition_id], [plan_id], [organization_guid], [space_guid], [database_name] FROM [service_instance_info] WHERE [service_instance_id] = :serviceInstanceId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId);
				
		try {
			return namedParameterJdbcTemplate.queryForObject(sql, param, new RowMapper<CubridServiceInstance>() {
				@Override
				public CubridServiceInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
					CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest();
					createServiceInstanceRequest.withServiceInstanceId(rs.getString("service_instance_id"));
					createServiceInstanceRequest.setServiceDefinitionId(rs.getString("Service_definition_id"));
					createServiceInstanceRequest.setPlanId(rs.getString("plan_id"));
					createServiceInstanceRequest.setOrganizationGuid(rs.getString("organization_guid"));
					createServiceInstanceRequest.setSpaceGuid(rs.getString("space_guid"));

					CubridServiceInstance cubridServiceInstance = new CubridServiceInstance(createServiceInstanceRequest);
					cubridServiceInstance.setDatabaseName(rs.getString("database_name"));
					
					return cubridServiceInstance;
				}
			});
		} catch (DataAccessException e) {
			// Not error.
			logger.info("Service instance does not exist. : " + serviceInstanceId);

			return null;
		}
	}
	
	public CubridServiceInstanceBinding findServiceBindInfo(String serviceInstanceBindId) {
		String sql = "/* findServiceBindInfo */ SELECT [service_instance_bind_id], [service_instance_id], [application_id], [username] FROM [service_instance_bind_info] WHERE [service_instance_bind_id] = :serviceInstanceBindId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceBindId", serviceInstanceBindId);
		
		try {
			return namedParameterJdbcTemplate.queryForObject(sql, param, new RowMapper<CubridServiceInstanceBinding>() {
				@Override
				public CubridServiceInstanceBinding mapRow(ResultSet rs, int rowNum) throws SQLException {
					CubridServiceInstanceBinding cubridServiceInstanceBinding = new CubridServiceInstanceBinding(
							rs.getString("service_instance_bind_id"),
							rs.getString("service_instance_id"),
							getCredentialsInfo(serviceInstanceBindId),
							null,
							rs.getString("application_id"));
					cubridServiceInstanceBinding.setDatabaseUserName(rs.getString("username"));
					
					return cubridServiceInstanceBinding;
				}
			});
		} catch (DataAccessException e) {
			logger.error("Service instance bind does not exist. : " + serviceInstanceBindId);

			return null;
		}
	}
	
	public boolean isExistsServiceBind(String serviceInstanceId) {
		String sql = "/* isExistsServiceBind */ SELECT count([service_instance_bind_id]) FROM [service_instance_bind_info] WHERE [service_instance_id] = :serviceInstanceId";

		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId);
		
		Integer count = 0;
		try {
			count = namedParameterJdbcTemplate.queryForObject(sql, param, Integer.class);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}

		return count > 0 ? true : false;
	}
	
	public void save(CubridServiceInstance serviceInstance, String dbapass) {
		saveServiceInstanceInfo(serviceInstance);
		Map<String, Object> credentials = getCredentials(serviceInstance.getDatabaseName(), "dba", dbapass);
		saveCredentialsInfo(serviceInstance.getServiceInstanceId(), null, credentials);
	}
	
	public void saveBind(CubridServiceInstanceBinding serviceInstanceBinding) {
		saveServiceInstanceBindInfo(serviceInstanceBinding);
		saveCredentialsInfo(serviceInstanceBinding.getServiceInstanceId(), serviceInstanceBinding.getId(), serviceInstanceBinding.getCredentials());
	}
	
	public void delete(String serviceInstanceId) {
		deleteServiceInstanceInfo(serviceInstanceId);
		deleteCredentialsInfoByInstanceId(serviceInstanceId);
	}
	
	public void deleteBind(String serviceInstanceBindId) {
		deleteServiceInstanceBindInfo(serviceInstanceBindId);
		deleteCredentialsInfoByBindId(serviceInstanceBindId);
	}
	
	public void deleteBindByInstanceId(String serviceInstanceId) {
		String sql = "/* deleteBindByInstanceId */ DELETE FROM [service_instance_bind_info] WHERE [service_instance_id] = :serviceInstanceId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId);
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	public void dropOwnerObject(String serviceInstanceBindId) {
		Map<String, Object> credentials = getCredentialsInfo(serviceInstanceBindId);
		
		DriverManagerDataSource instanceDataSource = new DriverManagerDataSource();
		instanceDataSource.setDriverClassName(String.valueOf(credentials.get("driverClassName")));
		instanceDataSource.setUrl(String.valueOf(credentials.get("jdbcUrl")));
		instanceDataSource.setUsername(String.valueOf(credentials.get("username")));
		instanceDataSource.setPassword(String.valueOf(credentials.get("password")));
		
		JdbcTemplate instanceJdbcTemplate = new JdbcTemplate(instanceDataSource);
		
		dropForeignKey(instanceJdbcTemplate);
		dropObject(instanceJdbcTemplate);
	}
	
	public String getErrorMessage(String request, String serviceInstanceId, String dbname, JSONObject response) {
		StringJoiner message = new StringJoiner(" ");

		message.add("[").add(request).add("-").add(serviceInstanceId + " (" + dbname + ")").add("]");

		if (response != null && response.get("task") != null) {
			message.add(response.get("task").toString()).add(":");
		}

		if (response != null && response.get("status") != null) {
			message.add(response.get("status").toString());
		}

		if (response != null && response.get("note") != null) {
			message.add("-").add(response.get("note").toString());
		}

		return message.toString();
	}
	
	private void saveServiceInstanceInfo(CubridServiceInstance serviceInstance) {
		String sql = "/* saveServiceInstanceInfo */ INSERT INTO [service_instance_info] ([service_instance_id], [service_definition_id], [plan_id], [organization_guid], [space_guid], [database_name]) VALUES (:serviceInstanceId, :serviceDefinitionId, :planId, :organizationGuid, :spaceGuid, :databaseName)";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstance.getServiceInstanceId())
				.addValue("serviceDefinitionId", serviceInstance.getServiceDefinitionId())
				.addValue("planId", serviceInstance.getPlanId())
				.addValue("organizationGuid", serviceInstance.getOrganizationGuid())
				.addValue("spaceGuid", serviceInstance.getSpaceGuid())
				.addValue("databaseName", serviceInstance.getDatabaseName());
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	private void saveServiceInstanceBindInfo(CubridServiceInstanceBinding serviceInstanceBinding) {
		String sql = "/* saveServiceInstanceBindInfo */ INSERT INTO [service_instance_bind_info] ([service_instance_bind_id], [service_instance_id], [application_id], [username]) VALUES (:serviceInstanceBindId, :serviceInstanceId, :applicationId, :username)";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceBindId", serviceInstanceBinding.getId())
				.addValue("serviceInstanceId", serviceInstanceBinding.getServiceInstanceId())
				.addValue("applicationId", serviceInstanceBinding.getAppGuid())
				.addValue("username", serviceInstanceBinding.getDatabaseUserName());
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	private void saveCredentialsInfo(String serviceInstanceId, String serviceInstanceBindId, Map<String, Object> credentials) {
		String sql = "/* saveCredentialsInfo */ INSERT INTO [credentials_info] ([service_instance_id], [service_instance_bind_id], [name], [hostname], [port], [username], [password], [uri], [jdbcurl]) VALUES (:serviceInstanceId, :serviceInstanceBindId, :name, :hostname, :port, :username, :password, :uri, :jdbcUrl)";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId)
				.addValue("serviceInstanceBindId", serviceInstanceBindId)
				.addValue("name", credentials.get("name"))
				.addValue("hostname", credentials.get("hostname"))
				.addValue("port", credentials.get("port"))
				.addValue("username", credentials.get("username"))
				.addValue("password", credentials.get("password"))
				.addValue("uri", credentials.get("uri"))
				.addValue("jdbcUrl", credentials.get("jdbcUrl"));
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	private void deleteServiceInstanceInfo(String serviceInstanceId) {
		String sql = "/* deleteServiceInstanceInfo */ DELETE FROM [service_instance_info] WHERE [service_instance_id] = :serviceInstanceId";

		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId);
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	private void deleteServiceInstanceBindInfo(String serviceInstanceBindId) {
		String sql = "/* deleteServiceInstanceBindInfo */ DELETE FROM [service_instance_bind_info] WHERE [service_instance_bind_id] = :serviceInstanceBindId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceBindId", serviceInstanceBindId);
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}
	
	private void deleteCredentialsInfoByInstanceId(String serviceInstanceId) {
		String sql = "/* DELETET_CREDENTIALS_INFO_BY_INSTANCE_ID */ DELETE FROM [credentials_info] WHERE [service_instance_id] = :serviceInstanceId";
		
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceId", serviceInstanceId);
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}	
	}
	
	private void deleteCredentialsInfoByBindId(String serviceInstanceBindId) {
		String sql = "/* deleteCredentialsInfoByBindId */ DELETE FROM [credentials_info] WHERE [service_instance_bind_id] = :serviceInstanceBindId";

		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("serviceInstanceBindId", serviceInstanceBindId);
		
		try {
			this.namedParameterJdbcTemplate.update(sql, param);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage(), e);
		}	
	}
	
	private void dropForeignKey(JdbcTemplate instanceJdbcTemplate) {
		String sql = "/* dropForeignKey */ SELECT 'ALTER TABLE ' || [class_name] || ' DROP CONSTRAINT ' || [index_name] || ';' AS [DROP_FOREIGN_KEY] FROM [db_index] WHERE [is_foreign_key] = 'YES'";
		
		List<Map<String, Object>> ownerForeignKey = instanceJdbcTemplate.queryForList(sql);
		
		Iterator<Map<String, Object>> ownerForeignKeyIter = ownerForeignKey.iterator();
		while (ownerForeignKeyIter.hasNext()) {
			Map<String, Object> ownerForeignKeyRow = ownerForeignKeyIter.next();
			
			String dropForeignKey = ownerForeignKeyRow.get("DROP_FOREIGN_KEY").toString();
			
			instanceJdbcTemplate.execute(dropForeignKey);
		}
	}
	
	private void dropObject(JdbcTemplate instanceJdbcTemplate) {
		String sql = "/* dropOwnerObject */ SELECT DECODE ([class_type], 'CLASS', 'TABLE', 'VCLASS', 'VIEW') AS [object_type], [class_name] AS [name] FROM [db_class] WHERE [owner_name] = USER AND [class_type] IN ('CLASS', 'VCLASS') AND [is_system_class] = 'NO'"
				+ " UNION ALL SELECT 'SERIAL' AS [object_type], [name] AS [name] FROM [db_serial] WHERE [owner].[name] = USER AND [class_name] IS NULL"
				+ " UNION ALL SELECT [sp_type] AS [object_type], [sp_name] AS [name] FROM [db_stored_procedure] WHERE [owner] = USER AND [sp_type] IN ('PROCEDURE', 'FUNCTION')";
		
		List<Map<String, Object>> ownerObject = instanceJdbcTemplate.queryForList(sql);
		
		Iterator<Map<String, Object>> ownerObjectIter = ownerObject.iterator();
		while (ownerObjectIter.hasNext()) {
			Map<String, Object> ownerObjectRow = ownerObjectIter.next();
			
			String objectType = ownerObjectRow.get("object_type").toString();
			String name = ownerObjectRow.get("name").toString();
			
			instanceJdbcTemplate.execute("DROP " + objectType + " " + name + ";");
		}
	}
}
