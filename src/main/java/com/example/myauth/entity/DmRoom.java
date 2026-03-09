package com.example.myauth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DM 채팅방 엔티티
 * 1:1 다이렉트 메시지를 위한 채팅방 관리
 *
 * 【테이블 정보】
 * - 테이블명: dm_rooms
 * - 주요 기능: 두 사용자 간 1:1 채팅방 관리
 *
 * 【연관 관계】
 * - User(user1): N:1 (참여자 1, 항상 작은 ID)
 * - User(user2): N:1 (참여자 2, 항상 큰 ID)
 *
 * 【유니크 제약조건】
 * - (user1_id, user2_id) 조합이 유일해야 함
 * - user1_id < user2_id를 항상 보장하여 같은 두 사용자 간 중복 채팅방 방지
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "dm_rooms",
    // 같은 두 사용자 간 중복 채팅방 방지
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_dm_room_users",
            columnNames = {"user1_id", "user2_id"}
        )
    },
    indexes = {
        // 특정 사용자의 채팅방 목록 조회용 인덱스
        @Index(name = "idx_dm_user1_id", columnList = "user1_id"),
        @Index(name = "idx_dm_user2_id", columnList = "user2_id"),
        // 최근 활동순 정렬용 인덱스
        @Index(name = "idx_dm_updated_at", columnList = "updated_at")
    }
)
public class DmRoom {

    /**
     * 채팅방 고유 식별자 (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 참여자 1 - 항상 더 작은 userId를 가진 사용자
     * 두 사용자 간 채팅방 중복 생성 방지를 위한 정규화 규칙
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /**
     * 참여자 2 - 항상 더 큰 userId를 가진 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    /**
     * 채팅방 생성 일시 (자동 설정)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 활동 일시 (메시지 전송 시 갱신)
     * 채팅방 목록을 최근 활동순으로 정렬하는 데 사용
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 팩토리 메서드 =====

    /**
     * 채팅방 생성
     * user1_id < user2_id를 보장하여 같은 두 사용자 간 중복 채팅방 방지
     *
     * @param userA 사용자 A
     * @param userB 사용자 B
     * @return 정규화된 DmRoom 인스턴스 (user1 = 작은 ID, user2 = 큰 ID)
     */
    public static DmRoom create(User userA, User userB) {
        // 항상 작은 ID가 user1, 큰 ID가 user2가 되도록 정규화
        User first = userA.getId() < userB.getId() ? userA : userB;
        User second = userA.getId() < userB.getId() ? userB : userA;

        return DmRoom.builder()
            .user1(first)
            .user2(second)
            .build();
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 주어진 사용자가 이 채팅방의 참여자인지 확인
     *
     * @param userId 확인할 사용자 ID
     * @return 참여자이면 true, 아니면 false
     */
    public boolean isParticipant(Long userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }

    /**
     * 주어진 사용자의 상대방을 반환
     * 채팅방 목록에서 상대방 정보를 표시할 때 사용
     *
     * @param userId 현재 사용자 ID
     * @return 상대방 User 엔티티
     */
    public User getOtherUser(Long userId) {
        return user1.getId().equals(userId) ? user2 : user1;
    }
}