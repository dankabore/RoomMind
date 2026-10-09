package com.roommind.repository;

import com.roommind.entity.GroupSummary;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Looked up by the group's conversation id, which is the row's id, so the
 * built-in findById and save are all this needs.
 */
public interface GroupSummaryRepository extends JpaRepository<GroupSummary, Long> {
}
