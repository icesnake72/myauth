package com.example.myauth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

/**
 * DM 메시지 엔티티
 * 채팅방 내 개별 메시지를 관리
 *
 * 【테이블 정보】
 * - 테이블명: dm_messages
 * - 주요 기능: 1:1 다이렉트 메시지 저장 및 읽음 상태 관리
 *
 * 【연관 관계】
 * - DmRoom(room): N:1 (소속 채팅방, CASCADE DELETE)
 * - User(sender): N:1 (발신자)
 *
 * 【인덱스】
 * - (room_id, created_at DESC): 채팅방별 메시지 페이징 조회
 * - (room_id, is_read, sender_id): 안 읽은 메시지 카운팅
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "dm_messages",
    indexes = {
        // 채팅방별 메시지 페이징 조회용 (최신 순 정렬)
        @Index(name = "idx_dm_msg_room_created", columnList = "room_id, created_at DESC"),
        // 안 읽은 메시지 카운팅용 (채팅방 + 읽음여부 + 발신자)
        @Index(name = "idx_dm_msg_unread", columnList = "room_id, is_read, sender_id")
    }
)
public class DmMessage {

    /**
     * 메시지 고유 식별자 (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 채팅방
     * 채팅방 삭제 시 메시지도 함께 삭제됨 (CASCADE DELETE는 DB 레벨에서 처리)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DmRoom room;

    /**
     * 메시지 발신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * 메시지 내용 (최대 2000자)
     */
    @Column(nullable = false, length = 2000)
    private String content;

    /**
     * 수신자 읽음 여부
     * - false: 아직 읽지 않음 (기본값)
     * - true: 수신자가 읽음 처리 완료
     */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /**
     * 메시지 전송 일시 (자동 설정)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ===== 팩토리 메서드 =====

    /**
     * 메시지 생성
     *
     * @param room    소속 채팅방
     * @param sender  발신자
     * @param content 메시지 내용
     * @return DmMessage 인스턴스
     */
    public static DmMessage create(DmRoom room, User sender, String content) {
        return DmMessage.builder()
            .room(room)
            .sender(sender)
            .content(content)
            .isRead(false)
            .build();
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 내가 보낸 메시지인지 확인
     *
     * @param userId 현재 사용자 ID
     * @return 내가 보낸 메시지이면 true
     */
    public boolean isMine(Long userId) {
        return sender.getId().equals(userId);
    }

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
