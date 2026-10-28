-- domain/src/main/resources/db/migration/V1__init.sql

CREATE TABLE member (
						id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
						kakao_id bigint NOT NULL,
						nickname varchar(30) NOT NULL,
						email varchar(255),
						status varchar(20) NOT NULL DEFAULT 'ACTIVE',
						last_seen_at timestamptz,
						withdrawn_at timestamptz,
						created_at timestamptz NOT NULL DEFAULT now(),
						updated_at timestamptz NOT NULL DEFAULT now(),
						CONSTRAINT uk_member_kakao_id UNIQUE (kakao_id),
						CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'WITHDRAWN')),
						CONSTRAINT ck_member_withdrawn_at CHECK ((status = 'WITHDRAWN') = (withdrawn_at IS NOT NULL))
);

CREATE TABLE club (
					  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
					  name varchar(50) NOT NULL,
					  description varchar(500),
					  visibility varchar(20) NOT NULL,
					  penalty_scope varchar(20) NOT NULL DEFAULT 'MEETUP',
					  succession_mode varchar(20) NOT NULL,
					  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
					  closes_at timestamptz,
					  created_at timestamptz NOT NULL DEFAULT now(),
					  updated_at timestamptz NOT NULL DEFAULT now(),
					  CONSTRAINT ck_club_visibility CHECK (visibility IN ('PUBLIC_LINK', 'MEMBERS_ONLY')),
					  CONSTRAINT ck_club_penalty_scope CHECK (penalty_scope IN ('MEETUP', 'CLUB')),
					  CONSTRAINT ck_club_succession_mode CHECK (succession_mode IN ('AUTO', 'VOTE')),
					  CONSTRAINT ck_club_status CHECK (status IN ('ACTIVE', 'CLOSING', 'CLOSED')),
					  CONSTRAINT ck_club_closes_at CHECK (status <> 'CLOSING' OR closes_at IS NOT NULL)
);

CREATE TABLE club_membership (
								 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
								 club_id bigint NOT NULL,
								 member_id bigint NOT NULL,
								 role varchar(20) NOT NULL,
								 status varchar(20) NOT NULL,
								 joined_at timestamptz,
								 created_at timestamptz NOT NULL DEFAULT now(),
								 updated_at timestamptz NOT NULL DEFAULT now(),
								 CONSTRAINT fk_club_membership_club FOREIGN KEY (club_id) REFERENCES club (id),
								 CONSTRAINT fk_club_membership_member FOREIGN KEY (member_id) REFERENCES member (id),
								 CONSTRAINT uk_club_membership_member_club UNIQUE (member_id, club_id),
								 CONSTRAINT ck_club_membership_role CHECK (role IN ('OWNER', 'VICE_OWNER', 'MANAGER', 'MEMBER')),
								 CONSTRAINT ck_club_membership_status CHECK (status IN ('PENDING', 'ACTIVE', 'LEFT', 'KICKED')),
								 CONSTRAINT ck_club_membership_role_needs_active CHECK (role = 'MEMBER' OR status = 'ACTIVE'),
								 CONSTRAINT ck_club_membership_joined_at CHECK (status <> 'ACTIVE' OR joined_at IS NOT NULL)
);

CREATE UNIQUE INDEX uk_club_membership_owner ON club_membership (club_id) WHERE role = 'OWNER';
CREATE UNIQUE INDEX uk_club_membership_vice_owner ON club_membership (club_id) WHERE role = 'VICE_OWNER';
CREATE INDEX idx_club_membership_club_status ON club_membership (club_id, status);

CREATE TABLE club_membership_permission (
											id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
											club_membership_id bigint NOT NULL,
											permission varchar(30) NOT NULL,
											created_at timestamptz NOT NULL DEFAULT now(),
											updated_at timestamptz NOT NULL DEFAULT now(),
											CONSTRAINT fk_club_membership_permission_membership
												FOREIGN KEY (club_membership_id) REFERENCES club_membership (id) ON DELETE CASCADE,
											CONSTRAINT uk_club_membership_permission UNIQUE (club_membership_id, permission),
											CONSTRAINT ck_club_membership_permission CHECK (permission IN
																							('MEETUP_MANAGE', 'APPLY_MANAGE', 'ATTENDANCE_MANAGE', 'MEMBER_MANAGE'))
);

CREATE TABLE meetup (
						id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
						club_id bigint NOT NULL,
						title varchar(100) NOT NULL,
						place varchar(200),
						starts_at timestamptz NOT NULL,
						status varchar(20) NOT NULL DEFAULT 'SCHEDULED',
						cancelled_at timestamptz,
						capacity int NOT NULL,
						apply_opens_at timestamptz NOT NULL,
						apply_closes_at timestamptz NOT NULL,
						waitlist_enabled boolean NOT NULL DEFAULT true,
						waitlist_capacity int,
						promotion_mode varchar(20) NOT NULL,
						manual_fallback_minutes int,
						attendance_window_minutes int NOT NULL DEFAULT 60,
						qr_radius_meters int NOT NULL DEFAULT 100,
						attendance_opened_at timestamptz,
						confirmed_count int NOT NULL DEFAULT 0,
						waiting_count int NOT NULL DEFAULT 0,
						next_waiting_seq int NOT NULL DEFAULT 1,
						created_at timestamptz NOT NULL DEFAULT now(),
						updated_at timestamptz NOT NULL DEFAULT now(),
						CONSTRAINT fk_meetup_club FOREIGN KEY (club_id) REFERENCES club (id),
						CONSTRAINT ck_meetup_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
						CONSTRAINT ck_meetup_cancelled_at CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
						CONSTRAINT ck_meetup_capacity CHECK (capacity > 0),
						CONSTRAINT ck_meetup_apply_period CHECK (apply_opens_at < apply_closes_at AND apply_closes_at <= starts_at),
						CONSTRAINT ck_meetup_promotion_mode CHECK (promotion_mode IN ('AUTO', 'MANUAL')),
						CONSTRAINT ck_meetup_manual_fallback CHECK (manual_fallback_minutes IS NULL OR manual_fallback_minutes > 0),
						CONSTRAINT ck_meetup_attendance_window CHECK (attendance_window_minutes > 0),
						CONSTRAINT ck_meetup_qr_radius CHECK (qr_radius_meters > 0),
						CONSTRAINT ck_meetup_confirmed_count CHECK (confirmed_count BETWEEN 0 AND capacity),
						CONSTRAINT ck_meetup_waitlist_capacity CHECK (waitlist_capacity IS NULL OR waitlist_capacity >= 0),
						CONSTRAINT ck_meetup_waiting_count CHECK (waiting_count >= 0
							AND (waitlist_enabled OR waiting_count = 0)
							AND (waitlist_capacity IS NULL OR waiting_count <= waitlist_capacity)),
						CONSTRAINT ck_meetup_next_waiting_seq CHECK (next_waiting_seq >= 1)
);

CREATE INDEX idx_meetup_club_starts_at ON meetup (club_id, starts_at);

CREATE TABLE apply (
							 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
							 meetup_id bigint NOT NULL,
							 member_id bigint NOT NULL,
							 status varchar(20) NOT NULL,
							 waiting_seq int,
							 idempotency_key varchar(64) NOT NULL,
							 applied_at timestamptz NOT NULL,
							 confirmed_at timestamptz,
							 ended_at timestamptz,
							 created_at timestamptz NOT NULL DEFAULT now(),
							 updated_at timestamptz NOT NULL DEFAULT now(),
							 CONSTRAINT fk_apply_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
							 CONSTRAINT fk_apply_member FOREIGN KEY (member_id) REFERENCES member (id),
							 CONSTRAINT uk_apply_idempotency_key UNIQUE (idempotency_key),
							 CONSTRAINT uk_apply_meetup_waiting_seq UNIQUE (meetup_id, waiting_seq),
							 CONSTRAINT ck_apply_status CHECK (status IN
																	 ('WAITING', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'MEETUP_CANCELLED')),
							 CONSTRAINT ck_apply_waiting_seq CHECK (status <> 'WAITING' OR waiting_seq IS NOT NULL),
							 CONSTRAINT ck_apply_confirmed_at CHECK (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL),
							 CONSTRAINT ck_apply_ended_at CHECK ((status IN ('WAITING', 'CONFIRMED')) = (ended_at IS NULL))
);

-- 진행 중 신청은 회차당 한 사람에 하나 (불변 조건 2번)
CREATE UNIQUE INDEX uk_apply_active ON apply (meetup_id, member_id)
	WHERE status IN ('WAITING', 'CONFIRMED');
-- 대기 1번 찾기
CREATE INDEX idx_apply_waiting ON apply (meetup_id, waiting_seq) WHERE status = 'WAITING';
CREATE INDEX idx_apply_member ON apply (member_id);

CREATE TABLE attendance (
							id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
							apply_id bigint NOT NULL,
							status varchar(20) NOT NULL,
							checked_at timestamptz,
							check_method varchar(20),
							processed_by bigint,
							distance_meters int,
							created_at timestamptz NOT NULL DEFAULT now(),
							updated_at timestamptz NOT NULL DEFAULT now(),
							CONSTRAINT fk_attendance_apply FOREIGN KEY (apply_id) REFERENCES apply (id),
							CONSTRAINT fk_attendance_processed_by FOREIGN KEY (processed_by) REFERENCES member (id),
							CONSTRAINT uk_attendance_apply UNIQUE (apply_id),
							CONSTRAINT ck_attendance_status CHECK (status IN ('ATTENDED', 'LATE', 'NO_SHOW')),
							CONSTRAINT ck_attendance_check_method CHECK (check_method IS NULL OR check_method IN ('SELF', 'STAFF')),
							CONSTRAINT ck_attendance_checked CHECK (status = 'NO_SHOW' OR (checked_at IS NOT NULL AND check_method IS NOT NULL)),
							CONSTRAINT ck_attendance_no_show CHECK (status <> 'NO_SHOW' OR (checked_at IS NULL AND check_method IS NULL)),
							CONSTRAINT ck_attendance_distance CHECK (distance_meters IS NULL OR distance_meters >= 0)
);

CREATE TABLE apply_history (
									 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
									 apply_id bigint NOT NULL,
									 from_status varchar(20),
									 to_status varchar(20) NOT NULL,
									 actor_type varchar(20) NOT NULL,
									 actor_member_id bigint,
									 created_at timestamptz NOT NULL DEFAULT now(),
									 updated_at timestamptz NOT NULL DEFAULT now(),
									 CONSTRAINT fk_apply_history_apply FOREIGN KEY (apply_id) REFERENCES apply (id),
									 CONSTRAINT fk_apply_history_actor FOREIGN KEY (actor_member_id) REFERENCES member (id),
									 CONSTRAINT ck_apply_history_actor_type CHECK (actor_type IN ('SELF', 'STAFF', 'SYSTEM')),
									 CONSTRAINT ck_apply_history_actor CHECK ((actor_type = 'SYSTEM') = (actor_member_id IS NULL))
);

CREATE INDEX idx_apply_history_apply ON apply_history (apply_id, created_at);
