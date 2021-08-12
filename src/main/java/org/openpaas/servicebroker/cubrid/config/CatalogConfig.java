package org.openpaas.servicebroker.cubrid.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.Plan;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {

	@Bean
	public Catalog catalog() {
		return new Catalog(Arrays.asList(new ServiceDefinition("cubrid", // id
				"CUBRID", // name
				"CUBRID is engineered as a completely free, open-source relational database management engine, with built-in enterprise grade features.", // description
				true, // bindable
				false, // plan_updatable
				getPlans(), // plans
				Arrays.asList("cubrid", "opensource"), // tags
				getServiceDefinitionMetadata(), // metadata
				getRequires(), // requires
				// getDashboardClient() // dashboard_client
				null)));
	}

	/* Used by OpenPaaS console */

	private List<Plan> getPlans() {
		List<Plan> plans = Arrays.asList(new Plan("128M", // id
				"128M", // name
				"CUBRID Database, Store up to 128M of data.", // description
				getPlanMetadata("128M"), // metadata
				true));

		return plans;
	}

	private Map<String, Object> getServiceDefinitionMetadata() {
		Map<String, Object> serviceDefinitionMetadata = new HashMap<String, Object>();
		serviceDefinitionMetadata.put("displayName", "CUBRID"); // displayName
		serviceDefinitionMetadata.put("imageUrl",
				"https://www.cubrid.org/layouts/layout_master/img/cubrid-logo-vertical.PNG"); // imageUrl
		serviceDefinitionMetadata.put("longDescription",
				"CUBRID is a compound word of \"CUBE\" (meaning data storage space) and \"BRIDGE\" (meaning data connection); profoundly implicating our corporation goal is to exert the biggest positive influence as a DBMS provider in this data-centric society."); // longDescription
		serviceDefinitionMetadata.put("providerDisplayName", "CUBRID Corporation."); // providerDisplayName
		serviceDefinitionMetadata.put("documentationUrl", "https://www.cubrid.org/documentation/manuals"); // documentationUrl
		serviceDefinitionMetadata.put("supportUrl", "http://www.cubrid.com"); // supportUrl
		return serviceDefinitionMetadata;
	}

	private Map<String, Object> getPlanMetadata(String planId) {
		Map<String, Object> planMetadata = new HashMap<String, Object>();
		planMetadata.put("bullets", getBullets(planId)); // bullets
		planMetadata.put("costs", getCosts(planId)); // costs

		if ("128M".equals(planId)) {
			planMetadata.put("displayName", "CUBRID Database, Store up to 128M of data."); // displayName
		}

		return planMetadata;
	}

	private List<String> getBullets(String planId) {
		if ("128M".equals(planId)) {
			return Arrays.asList("CUBRID Database, Store up to 128M of data.");
		} else {
			return Arrays.asList("Disabled");
		}
	}

	private List<Map<String, Object>> getCosts(String planId) {
		Map<String, Object> costs = new HashMap<String, Object>();
		Map<String, Object> amount = new HashMap<String, Object>();

		if ("128M".equals(planId)) {
			amount.put("won", new Integer(0));
			costs.put("amount", amount);
			costs.put("unit", "MONTHLY");
		}

		return Arrays.asList(costs);
	}

	private List<String> getRequires() {
//		return Arrays.asList("Windows 32/64 Bit XP, 2003, Vista, Windows 7",
//			"Linux family 32/64 Bit(Linux kernel 2.4, glibc 2.3.4 or higher",
//			"Requires a 500 MB of free disk space on the initial installation; requires approximately 1.5 GB of free disk space with a database creating with default options.",
//			"JRE/JDK 1.6 or higher (Required when Java Stored Procedure is required");

		return Arrays.asList("syslog_drain");
	}

//	private DashboardClient getDashboardClient() {
//		return new DashboardClient(
//			"CUBRID Admin", // id
//			null, // secret
//			"https://www.cubrid.com/downloads"); // redirect_uri
//	}
}