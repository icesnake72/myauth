package com.example.myauth.dto.dm;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 생성 요청 DTO
 * 대상 사용자 ID를 받아 1:1 채팅방을 생성하거나 기존 채팅방을 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmRoomCreateRequest {

    /**
     * DM을 보낼 상대방 사용자 ID
     */
    @NotNull(message = "대상 사용자 ID는 필수입니다")
    private Long targetUserId;
}
