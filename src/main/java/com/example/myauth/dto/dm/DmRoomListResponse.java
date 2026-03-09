package com.example.myauth.dto.dm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 응답 DTO
 * 내 채팅방 목록 조회 시 사용 (마지막 메시지, 안 읽은 수 포함)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmRoomListResponse {

    /** 채팅방 ID */
    private Long roomId;

    /** 상대방 사용자 정보 */
    private DmUserResponse otherUser;

    /** 마지막 메시지 미리보기 (메시지가 없으면 null) */
    private DmLastMessageResponse lastMessage;

    /** 안 읽은 메시지 수 */
    private long unreadCount;

    /** 마지막 활동 일시 (정렬 기준) */
    private LocalDateTime updatedAt;
}
