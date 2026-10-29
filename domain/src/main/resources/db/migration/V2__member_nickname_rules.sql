CREATE UNIQUE INDEX uk_member_nickname_active ON member (nickname) WHERE status = 'ACTIVE';

ALTER TABLE member ADD CONSTRAINT ck_member_nickname_no_space CHECK (nickname !~ '[[:space:]]');
