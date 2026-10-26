package io.github.zemiles.hanjari.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.club.entity.Club;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
}
