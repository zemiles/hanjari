package io.github.zemiles.hanjari.meetup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.meetup.entity.Meetup;

@Repository
public interface MeetupRepository extends JpaRepository<Meetup, Long> {
}
