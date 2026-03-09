package com.example.myauth.dto.dm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전체 안 읽은 메시지 수 응답 DTO
 * 네비게이션 바의 DM 아이콘 뱃지에 표시할 수치
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmUnreadCountResponse {

    /** 모든 채팅방의 안 읽은 메시지 총합 */
    private long totalUnreadCount;
}
