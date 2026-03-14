package com.example.myauth.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 관리자 일별 통계 응답 DTO
 * 프론트엔드 대시보드 차트 렌더링용 시계열 데이터
 *
 * 【사용처】
 * - GET /api/admin/dashboard/daily-stats?days=30
 * - 일별 신규 가입자, 게시글, 댓글 수 및 조회수 합계 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDailyStatsResponse {

    /** 날짜 (yyyy-MM-dd 형식으로 직렬화됨) */
    private LocalDate date;

    /** 해당 날짜의 신규 가입자 수 */
    private long newUsers;

    /** 해당 날짜의 신규 게시글 수 (삭제되지 않은 것만) */
    private long newPosts;

    /** 해당 날짜의 신규 댓글 수 (삭제되지 않은 것만) */
    private long newComments;

    /** 해당 날짜에 작성된 게시글들의 누적 조회수 합계 */
    private long totalViews;
}
