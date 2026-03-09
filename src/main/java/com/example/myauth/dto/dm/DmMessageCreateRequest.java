package com.example.myauth.dto.dm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 전송 요청 DTO
 * 채팅방에 새 메시지를 전송할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmMessageCreateRequest {

    /**
     * 메시지 내용 (1~2000자)
     */
    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(min = 1, max = 2000, message = "메시지는 1~2000자까지 입력 가능합니다")
    private String content;
}
