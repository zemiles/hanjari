package io.github.zemiles.hanjari.attendance.entity;

/**
 * 출석을 체크한 방식. 노쇼는 체크가 없어서 비어 있다.
 */
public enum CheckMethod {

	/** 본인이 QR이나 코드로 체크했다 */
	SELF,

	/** 출석 관리 권한이 있는 사람이 명단에서 대신 처리했다 */
	STAFF
}
