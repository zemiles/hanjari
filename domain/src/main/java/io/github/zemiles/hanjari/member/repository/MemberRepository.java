package io.github.zemiles.hanjari.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.zemiles.hanjari.member.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

}
