package io.github.zemiles.hanjari.club.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.zemiles.hanjari.club.entity.Club;
import io.github.zemiles.hanjari.club.entity.PenaltyScope;
import io.github.zemiles.hanjari.club.entity.SuccessionMode;
import io.github.zemiles.hanjari.club.entity.Visibility;
import io.github.zemiles.hanjari.common.config.ClockConfig;
import io.github.zemiles.hanjari.common.config.JpaConfig;
import io.github.zemiles.hanjari.common.exception.ErrorType;
import io.github.zemiles.hanjari.common.exception.HanjariException;
import io.github.zemiles.hanjari.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@DisplayName("ClubRepository 관련 테스트")
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
public class ClubRepositoryTest {

	private final ClubRepository clubRepository;

	private final EntityManager entityManager;

	@Autowired
	public ClubRepositoryTest(ClubRepository clubRepository, EntityManager entityManager) {
		this.clubRepository = clubRepository;
		this.entityManager = entityManager;
	}

	@Test
	@DisplayName("클럽 생성")
	void createClub() {
		Club club = new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK, PenaltyScope.CLUB, SuccessionMode.AUTO);
		Club save = clubRepository.save(club);

		entityManager.flush();
		entityManager.clear();

		assertThat(save.getId()).isNotNull();
	}

	@Test
	@DisplayName("클럽 조회")
	void findClub() {
		Club club = new Club("클럽테스트", "테스트 클럽입니다.", Visibility.PUBLIC_LINK, PenaltyScope.CLUB, SuccessionMode.AUTO);
		clubRepository.save(club);

		entityManager.flush();
		entityManager.clear();

		Club resultClub = clubRepository.findById(club.getId()).orElseThrow(
			() -> new HanjariException(ErrorType.CLUB_NOT_FOUND, Map.of("clubId", club.getId()))
		);

		assertThat(resultClub.getId()).isNotNull();

	}

}
