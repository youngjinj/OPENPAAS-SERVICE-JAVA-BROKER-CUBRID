/* cubrid createdb --db-volume-size=128M --log-volume-size=128M servicebroker ko_KR.utf8
 * cubrid server start servicebroker
 * csql -u dba servicebroker -c "create user cubrid password 'cubrid';"
 * csql -u cubrid -p cubrid servicebroker
 * 
 * admin:7ec40be3750ed6af68233b78dcdcee22ab018cbd359adca67275ec6f6b61406a
 */

/* CREATE_SERVICE_INSTANCE_INFO */
CREATE TABLE [service_instance_info] (
	[service_instance_id] VARCHAR,
	[service_definition_id] VARCHAR,
	[plan_id] VARCHAR,
	[organization_guid] VARCHAR,
	[space_guid] VARCHAR,
	[database_name] VARCHAR,
	[updateDateTime] DATETIME DEFAULT SYSDATETIME,
	CONSTRAINT [pk_service_instance_info_service_instance_id] PRIMARY KEY([service_instance_id])
) REUSE_OID;

/* CREATE_SERVICE_INSTANCE_BIND_INFO */
CREATE TABLE [service_instance_bind_info] (
	[service_instance_bind_id] VARCHAR,
	[service_instance_id] VARCHAR,
	[application_id] VARCHAR,
	[username] VARCHAR,
	[updateDateTime] DATETIME DEFAULT SYSDATETIME,
	CONSTRAINT [pk_service_bind_info_service_instance_bind_id] PRIMARY KEY([service_instance_bind_id])
) REUSE_OID;

/* CREATE_CREDENTIALS_INFO */
CREATE TABLE [credentials_info] (
	[service_instance_id] VARCHAR,
	[service_instance_bind_id] VARCHAR,
	[name] VARCHAR,
	[hostname] VARCHAR,
	[port] VARCHAR,
	[username] VARCHAR,
	[password] VARCHAR,
	[uri] VARCHAR,
	[jdbcurl] VARCHAR,
	[updateDateTime] DATETIME DEFAULT SYSDATETIME,
	CONSTRAINT [u_credentials_info_name_username] UNIQUE (
		[service_instance_id],
		[service_instance_bind_id]
	)
) REUSE_OID;