package org.openpaas.servicebroker.cubrid.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@PropertySource("classpath:datasource.properties")
public class CubridConfig {
	@Autowired
	private Environment environment;

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(this.environment.getRequiredProperty("cubrid.jdbc.driver"));
		dataSource.setUrl(this.environment.getRequiredProperty("cubrid.jdbc.url"));
		dataSource.setUsername(this.environment.getRequiredProperty("cubrid.jdbc.username"));
		dataSource.setPassword(this.environment.getRequiredProperty("cubrid.jdbc.password"));
		
		return dataSource;
	}
	
	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource datasource) {
	    return new NamedParameterJdbcTemplate(datasource);
	}
}
