package com.roommind.health;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		String database;
		try {
			jdbcTemplate.queryForObject("select 1", Integer.class);
			database = "up";
		}
		catch (DataAccessException ex) {
			database = "down";
		}

		return Map.of("service", "roommind", "status", "ok", "database", database);
	}
}
