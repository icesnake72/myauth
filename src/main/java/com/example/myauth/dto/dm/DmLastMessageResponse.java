package com.example.myauth.dto.dm;

import com.example.myauth.entity.DmMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 마지막 메시지 응답 DTO
 * 채팅방 목록에서 마지막 메시지 미리보기에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmLastMessageResponse {

    /** 메시지 내용 */
    private String content;

    /** 발신자 ID */
    private Long senderId;

    /** 전송 일시 */
    private LocalDateTime createdAt;

    /**
     * DmMessage 엔티티를 DmLastMessageResponse DTO로 변환
     *
     * @param message DmMessage 엔티티
     * @return DmLastMessageResponse DTO
     */
    public static DmLastMessageResponse from(DmMessage message) {
        return DmLastMessageResponse.builder()
            .content(message.getContent())
            .senderId(message.getSender().getId())
            .createdAt(message.getCreatedAt())
            .build();
    }
}
