package com.roommind.repository;

import java.util.List;
import java.util.Optional;

import com.roommind.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	/**
	 * The people page, in one method. Spring Data writes the query from the name:
	 * username starts with the prefix, case ignored, id is not the caller's, rows
	 * come back in alphabetical order.
	 *
	 * An empty prefix is not a special case — every username starts with "" — so
	 * the unfiltered list and a search are the same call.
	 */
	List<User> findByUsernameStartingWithIgnoreCaseAndIdNotOrderByUsernameAsc(String prefix, Long excludedId);
}
