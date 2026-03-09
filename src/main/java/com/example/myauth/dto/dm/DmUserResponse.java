package com.example.myauth.dto.dm;

import com.example.myauth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DM 사용자 정보 응답 DTO
 * 채팅방 목록에서 상대방 정보를 표시할 때 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmUserResponse {

    /** 사용자 ID */
    private Long id;

    /** 사용자 이름 */
    private String name;

    /** 프로필 이미지 URL (없으면 null) */
    private String profileImage;

    /**
     * User 엔티티를 DmUserResponse DTO로 변환
     *
     * @param user User 엔티티
     * @return DmUserResponse DTO
     */
    public static DmUserResponse from(User user) {
        return DmUserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .profileImage(user.getProfileImage())
            .build();
    }
}
