package io.github.zemiles.hanjari.apply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.apply.entity.ApplyHistory;

@Repository
public interface ApplyHistoryRepository extends JpaRepository<ApplyHistory, Long> {
}
