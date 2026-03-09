package com.example.myauth.dto.dm;

import com.example.myauth.entity.DmRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 생성/조회 응답 DTO
 * 채팅방 생성 또는 기존 채팅방 반환 시 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmRoomResponse {

    /** 채팅방 ID */
    private Long roomId;

    /** 상대방 사용자 정보 */
    private DmUserResponse otherUser;

    /** 채팅방 생성 일시 */
    private LocalDateTime createdAt;

    /**
     * DmRoom 엔티티를 DmRoomResponse DTO로 변환
     * 현재 사용자 기준으로 상대방 정보를 결정
     *
     * @param room          DmRoom 엔티티
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return DmRoomResponse DTO
     */
    public static DmRoomResponse from(DmRoom room, Long currentUserId) {
        return DmRoomResponse.builder()
            .roomId(room.getId())
            .otherUser(DmUserResponse.from(room.getOtherUser(currentUserId)))
            .createdAt(room.getCreatedAt())
            .build();
    }
}
