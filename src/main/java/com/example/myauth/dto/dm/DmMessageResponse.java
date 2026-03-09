package com.example.myauth.dto.dm;

import com.example.myauth.entity.DmMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메시지 응답 DTO
 * 개별 메시지 정보를 반환할 때 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmMessageResponse {

    /** 메시지 ID */
    private Long id;

    /** 발신자 ID */
    private Long senderId;

    /** 발신자 이름 */
    private String senderName;

    /** 메시지 내용 */
    private String content;

    /** 읽음 여부 */
    private boolean isRead;

    /** 내가 보낸 메시지인지 여부 */
    private boolean isMine;

    /** 전송 일시 */
    private LocalDateTime createdAt;

    /**
     * DmMessage 엔티티를 DmMessageResponse DTO로 변환
     * 현재 사용자 기준으로 isMine 판단
     *
     * @param message       DmMessage 엔티티
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return DmMessageResponse DTO
     */
    public static DmMessageResponse from(DmMessage message, Long currentUserId) {
        return DmMessageResponse.builder()
            .id(message.getId())
            .senderId(message.getSender().getId())
            .senderName(message.getSender().getName())
            .content(message.getContent())
            .isRead(message.isRead())
            .isMine(message.isMine(currentUserId))
            .createdAt(message.getCreatedAt())
            .build();
    }
}
