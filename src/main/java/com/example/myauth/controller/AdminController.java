package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.admin.*;
import com.example.myauth.entity.User;
import com.example.myauth.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  @GetMapping("/dashboard/stats")
  public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
    return ResponseEntity.ok(ApiResponse.success("대시보드 통계 조회 성공", adminService.getDashboardStats()));
  }

  // 일별 통계 조회 (차트용 시계열 데이터, days: 기본값 30, 최대 90)
  @GetMapping("/dashboard/daily-stats")
  public ResponseEntity<ApiResponse<List<AdminDailyStatsResponse>>> getDailyStats(
      @RequestParam(defaultValue = "30") int days
  ) {
    if (days > 90) days = 90;
    if (days < 1) days = 1;
    return ResponseEntity.ok(ApiResponse.success("일별 통계 조회 성공", adminService.getDailyStats(days)));
  }

  @GetMapping("/dashboard/recent-users")
  public ResponseEntity<ApiResponse<List<AdminUserListResponse>>> getRecentUsers(
      @RequestParam(defaultValue = "10") int limit
  ) {
    if (limit > 50) limit = 50;
    return ResponseEntity.ok(ApiResponse.success("최근 가입 사용자 조회 성공", adminService.getRecentUsers(limit)));
  }

  @GetMapping("/dashboard/recent-posts")
  public ResponseEntity<ApiResponse<List<AdminPostListResponse>>> getRecentPosts(
      @RequestParam(defaultValue = "10") int limit
  ) {
    if (limit > 50) limit = 50;
    return ResponseEntity.ok(ApiResponse.success("최근 게시글 조회 성공", adminService.getRecentPosts(limit)));
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<Page<AdminUserListResponse>>> getUsers(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) User.Status status,
      @RequestParam(required = false) User.Role role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    if (size > 50) size = 50;
    Pageable pageable = PageRequest.of(page, size);

    Page<AdminUserListResponse> response = adminService.getUsers(keyword, status, role, pageable);
    return ResponseEntity.ok(ApiResponse.success("관리자 사용자 목록 조회 성공", response));
  }

  @GetMapping("/users/{userId}")
  public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(
      @PathVariable Long userId
  ) {
    return ResponseEntity.ok(ApiResponse.success("관리자 사용자 상세 조회 성공", adminService.getUserDetail(userId)));
  }

  @PutMapping("/users/{userId}/status")
  public ResponseEntity<ApiResponse<Void>> changeUserStatus(
      @AuthenticationPrincipal User admin,
      @PathVariable Long userId,
      @Valid @RequestBody AdminStatusChangeRequest request
  ) {
    adminService.changeUserStatus(admin.getId(), userId, request.getStatus(), request.getReason());
    return ResponseEntity.ok(ApiResponse.success("사용자 상태가 변경되었습니다."));
  }

  @PutMapping("/users/{userId}/role")
  public ResponseEntity<ApiResponse<Void>> changeUserRole(
      @AuthenticationPrincipal User admin,
      @PathVariable Long userId,
      @Valid @RequestBody AdminRoleChangeRequest request
  ) {
    adminService.changeUserRole(admin.getId(), userId, request.getRole());
    return ResponseEntity.ok(ApiResponse.success("사용자 역할이 변경되었습니다."));
  }

  @PostMapping("/users/{userId}/force-logout")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> forceLogout(
      @PathVariable Long userId
  ) {
    int revokedCount = adminService.forceLogout(userId);
    return ResponseEntity.ok(ApiResponse.success("강제 로그아웃 처리 완료", Map.of("revokedRefreshTokens", revokedCount)));
  }

  @GetMapping("/posts")
  public ResponseEntity<ApiResponse<Page<AdminPostListResponse>>> getPosts(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean isDeleted,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    if (size > 50) size = 50;
    Pageable pageable = PageRequest.of(page, size);

    Page<AdminPostListResponse> response = adminService.getPosts(keyword, isDeleted, pageable);
    return ResponseEntity.ok(ApiResponse.success("관리자 게시글 목록 조회 성공", response));
  }

  @GetMapping("/posts/{postId}")
  public ResponseEntity<ApiResponse<AdminPostListResponse>> getPostDetail(
      @PathVariable Long postId
  ) {
    return ResponseEntity.ok(ApiResponse.success("관리자 게시글 상세 조회 성공", adminService.getPostDetail(postId)));
  }

  @DeleteMapping("/posts/{postId}")
  public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
    adminService.deletePost(postId);
    return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
  }

  @PutMapping("/posts/{postId}/restore")
  public ResponseEntity<ApiResponse<Void>> restorePost(@PathVariable Long postId) {
    adminService.restorePost(postId);
    return ResponseEntity.ok(ApiResponse.success("게시글이 복구되었습니다."));
  }

  @PutMapping("/posts/{postId}/visibility")
  public ResponseEntity<ApiResponse<Void>> changePostVisibility(
      @PathVariable Long postId,
      @Valid @RequestBody AdminVisibilityChangeRequest request
  ) {
    adminService.changePostVisibility(postId, request.getVisibility());
    return ResponseEntity.ok(ApiResponse.success("게시글 공개 범위가 변경되었습니다."));
  }

  @GetMapping("/comments")
  public ResponseEntity<ApiResponse<Page<AdminCommentListResponse>>> getComments(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long postId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    if (size > 50) size = 50;
    Pageable pageable = PageRequest.of(page, size);

    Page<AdminCommentListResponse> response = adminService.getComments(keyword, postId, pageable);
    return ResponseEntity.ok(ApiResponse.success("관리자 댓글 목록 조회 성공", response));
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
    adminService.deleteComment(commentId);
    return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
  }
}
