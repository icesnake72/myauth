package com.example.myauth.service;

import com.example.myauth.dto.admin.*;
import com.example.myauth.entity.Comment;
import com.example.myauth.entity.Post;
import com.example.myauth.entity.User;
import com.example.myauth.entity.UserProfile;
import com.example.myauth.exception.CommentNotFoundException;
import com.example.myauth.exception.PostNotFoundException;
import com.example.myauth.exception.UserNotFoundException;
import com.example.myauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자 서비스
 * 대시보드 통계, 사용자/게시글/댓글 관리 등 관리자 전용 비즈니스 로직 처리
 *
 * 【주요 기능】
 * - 대시보드 통계 조회
 * - 사용자 관리 (목록, 상세, 상태/역할 변경, 강제 로그아웃)
 * - 게시글 관리 (목록, 상세, 삭제/복구, 공개 범위 변경)
 * - 댓글 관리 (목록, 삭제)
 *
 * 【보안 설계】
 * - 슈퍼유저(isSuperUser=true)는 상태/역할 변경 불가
 * - 본인 계정 상태 변경 불가, 본인 역할 강등 불가
 * - 강제 로그아웃은 토큰 삭제가 아닌 revoke 처리 (감사 추적 가능)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final LikeRepository likeRepository;
  private final FollowRepository followRepository;
  private final BookmarkRepository bookmarkRepository;
  private final DmRoomRepository dmRoomRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 대시보드 통계 조회
   * 전체 사용자, 게시글, 댓글 수 및 오늘/이번 주 신규 가입자/게시글 수 반환
   *
   * @return 대시보드 통계 정보
   */
  public AdminDashboardStatsResponse getDashboardStats() {
    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
    LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();

    return AdminDashboardStatsResponse.builder()
        .totalUsers(userRepository.count())
        .activeUsers(userRepository.countByStatus(User.Status.ACTIVE))
        .suspendedUsers(userRepository.countByStatus(User.Status.SUSPENDED))
        .totalPosts(postRepository.countByIsDeletedFalse())
        .totalComments(commentRepository.countByIsDeletedFalse())
        .totalDmRooms(dmRoomRepository.count())
        .todayNewUsers(userRepository.countByCreatedAtAfter(todayStart))
        .todayNewPosts(postRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart))
        .weeklyNewUsers(userRepository.countByCreatedAtAfter(weekStart))
        .weeklyNewPosts(postRepository.countByCreatedAtAfterAndIsDeletedFalse(weekStart))
        .build();
  }

  /**
   * 최근 가입 사용자 조회 (대시보드용)
   * 배치 카운트 쿼리를 사용하여 N+1 문제 방지
   *
   * @param limit 조회 건수 (최대 50)
   * @return 최근 가입 사용자 목록
   */
  public List<AdminUserListResponse> getRecentUsers(int limit) {
    Pageable pageable = PageRequest.of(0, Math.min(limit, 50));

    List<User> users = userRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    return toAdminUserListResponses(users);
  }

  /**
   * 최근 게시글 조회 (대시보드용)
   * 삭제되지 않은 게시글만 조회 (대시보드에서는 활성 게시글만 표시)
   *
   * @param limit 조회 건수 (최대 50)
   * @return 최근 게시글 목록
   */
  public List<AdminPostListResponse> getRecentPosts(int limit) {
    Pageable pageable = PageRequest.of(0, Math.min(limit, 50));

    // isDeleted=false: 대시보드에서는 삭제되지 않은 활성 게시글만 표시
    return postRepository.findByAdminFilter(null, false, pageable)
        .stream()
        .map(AdminPostListResponse::from)
        .toList();
  }

  /**
   * 관리자 사용자 목록 조회 (필터링/페이징)
   * 배치 카운트 쿼리를 사용하여 N+1 문제 방지
   *
   * @param keyword  검색 키워드 (이메일/이름)
   * @param status   상태 필터
   * @param role     역할 필터
   * @param pageable 페이징 정보
   * @return 사용자 목록 페이지
   */
  public Page<AdminUserListResponse> getUsers(
      String keyword,
      User.Status status,
      User.Role role,
      Pageable pageable
  ) {
    Page<User> userPage = userRepository.findByAdminFilter(normalizeKeyword(keyword), status, role, pageable);

    // 페이지 내 사용자 ID 목록 추출
    List<Long> userIds = userPage.getContent().stream()
        .map(User::getId)
        .toList();

    if (userIds.isEmpty()) {
      return userPage.map(this::toAdminUserListResponseFallback);
    }

    // 배치 쿼리로 게시글 수, 팔로워 수를 한번에 조회 (N+1 방지)
    Map<Long, Long> postCounts = toBatchCountMap(postRepository.countPostsByUserIds(userIds));
    Map<Long, Long> followerCounts = toBatchCountMap(followRepository.countFollowersByUserIds(userIds));

    return userPage.map(user -> AdminUserListResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .role(user.getRole().name())
        .status(user.getStatus().name())
        .isActive(user.getIsActive())
        .provider(user.getProvider())
        .postCount(postCounts.getOrDefault(user.getId(), 0L))
        .followerCount(followerCounts.getOrDefault(user.getId(), 0L))
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .build());
  }

  /**
   * 관리자 사용자 상세 조회
   * 프로필 정보와 활동 통계(게시글/댓글/좋아요/팔로워/팔로잉/북마크 수)를 포함
   *
   * @param userId 사용자 ID
   * @return 사용자 상세 정보
   */
  public AdminUserDetailResponse getUserDetail(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    UserProfile profile = userProfileRepository.findByUser(userId).orElse(null);

    AdminUserDetailResponse.ProfileInfo profileInfo = AdminUserDetailResponse.ProfileInfo.builder()
        .lastName(profile != null ? profile.getLastName() : null)
        .firstName(profile != null ? profile.getFirstName() : null)
        .phoneNumber(profile != null ? profile.getPhoneNumber() : null)
        .birth(profile != null ? profile.getBirth() : null)
        .bgImage(profile != null ? profile.getBgImage() : null)
        .build();

    // 사용자 활동 통계 조회 (단건 조회이므로 6개 개별 쿼리 허용)
    AdminUserDetailResponse.UserActivityStats stats = AdminUserDetailResponse.UserActivityStats.builder()
        .postCount(postRepository.countByUserIdAndIsDeletedFalse(userId))
        .commentCount(commentRepository.countByUserIdAndIsDeletedFalse(userId))
        .likeCount(likeRepository.countByUserId(userId))
        .followerCount(followRepository.countByFollowingId(userId))
        .followingCount(followRepository.countByFollowerId(userId))
        .bookmarkCount(bookmarkRepository.countByUserId(userId))
        .build();

    return AdminUserDetailResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .role(user.getRole().name())
        .status(user.getStatus().name())
        .isActive(user.getIsActive())
        .isSuperUser(user.getIsSuperUser())
        .provider(user.getProvider())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .failedLoginAttempts(user.getFailedLoginAttempts())
        .profile(profileInfo)
        .stats(stats)
        .build();
  }

  /**
   * 사용자 상태 변경
   * 슈퍼유저 보호 및 본인 계정 변경 방지 로직 포함
   *
   * @param adminUserId 관리자 사용자 ID (변경 수행자)
   * @param userId      대상 사용자 ID
   * @param status      변경할 상태
   * @param reason      변경 사유
   * @throws IllegalArgumentException 본인 계정 또는 슈퍼유저 변경 시도 시
   */
  @Transactional
  public void changeUserStatus(Long adminUserId, Long userId, User.Status status, String reason) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 본인 계정 상태 변경 방지
    if (adminUserId.equals(userId)) {
      throw new IllegalArgumentException("본인 계정 상태는 변경할 수 없습니다.");
    }

    // 슈퍼유저 보호: 슈퍼유저의 상태는 변경 불가
    if (Boolean.TRUE.equals(user.getIsSuperUser())) {
      throw new IllegalArgumentException("슈퍼유저의 상태는 변경할 수 없습니다.");
    }

    // 다른 관리자 상태 변경 방지 (슈퍼유저만 관리자 상태 변경 가능)
    if (user.getRole() == User.Role.ROLE_ADMIN) {
      User adminUser = userRepository.findById(adminUserId)
          .orElseThrow(() -> new UserNotFoundException(adminUserId));
      if (!Boolean.TRUE.equals(adminUser.getIsSuperUser())) {
        throw new IllegalArgumentException("다른 관리자의 상태를 변경하려면 슈퍼유저 권한이 필요합니다.");
      }
    }

    user.setStatus(status);
    user.setIsActive(status == User.Status.ACTIVE);

    userRepository.save(user);
    log.info("관리자 상태 변경 - adminId: {}, userId: {}, status: {}, reason: {}",
        adminUserId, userId, status, reason);
  }

  /**
   * 사용자 역할 변경
   * 슈퍼유저 보호, 본인 강등 방지, 관리자 간 역할 변경 제한 로직 포함
   *
   * @param adminUserId 관리자 사용자 ID (변경 수행자)
   * @param userId      대상 사용자 ID
   * @param role        변경할 역할
   * @throws IllegalArgumentException 보호 규칙 위반 시
   */
  @Transactional
  public void changeUserRole(Long adminUserId, Long userId, User.Role role) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 본인 역할을 일반 사용자로 강등 방지
    if (adminUserId.equals(userId) && role == User.Role.ROLE_USER) {
      throw new IllegalArgumentException("본인 계정을 일반 사용자로 변경할 수 없습니다.");
    }

    // 슈퍼유저 보호: 슈퍼유저의 역할은 변경 불가
    if (Boolean.TRUE.equals(user.getIsSuperUser())) {
      throw new IllegalArgumentException("슈퍼유저의 역할은 변경할 수 없습니다.");
    }

    // 다른 관리자의 역할 변경 방지 (슈퍼유저만 가능)
    if (user.getRole() == User.Role.ROLE_ADMIN && !adminUserId.equals(userId)) {
      User adminUser = userRepository.findById(adminUserId)
          .orElseThrow(() -> new UserNotFoundException(adminUserId));
      if (!Boolean.TRUE.equals(adminUser.getIsSuperUser())) {
        throw new IllegalArgumentException("다른 관리자의 역할을 변경하려면 슈퍼유저 권한이 필요합니다.");
      }
    }

    user.setRole(role);
    userRepository.save(user);
    log.info("관리자 역할 변경 - adminId: {}, userId: {}, role: {}", adminUserId, userId, role);
  }

  /**
   * 강제 로그아웃
   * 대상 사용자의 모든 Refresh Token을 revoke 처리 (삭제가 아닌 무효화)
   * 토큰 감사 추적(audit trail)을 위해 revoke 방식 사용
   *
   * @param userId 대상 사용자 ID
   * @return 무효화된 토큰 수
   */
  @Transactional
  public int forceLogout(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 토큰 삭제 대신 revoke 처리 (감사 추적 가능)
    int revoked = refreshTokenRepository.revokeAllByUserId(userId);
    log.info("강제 로그아웃 (토큰 revoke) - userId: {}, revokedTokens: {}", userId, revoked);
    return revoked;
  }

  /**
   * 관리자 게시글 목록 조회 (필터링/페이징)
   *
   * @param keyword   검색 키워드 (내용/작성자명)
   * @param isDeleted 삭제 여부 필터 (null이면 전체)
   * @param pageable  페이징 정보
   * @return 게시글 목록 페이지
   */
  public Page<AdminPostListResponse> getPosts(String keyword, Boolean isDeleted, Pageable pageable) {
    return postRepository.findByAdminFilter(normalizeKeyword(keyword), isDeleted, pageable)
        .map(AdminPostListResponse::from);
  }

  /**
   * 관리자 게시글 상세 조회
   * 이미지 정보를 포함하여 반환 (JOIN FETCH로 N+1 방지)
   *
   * @param postId 게시글 ID
   * @return 게시글 상세 정보
   */
  public AdminPostListResponse getPostDetail(Long postId) {
    Post post = postRepository.findByIdForAdmin(postId)
        .orElseThrow(() -> new PostNotFoundException(postId));

    return AdminPostListResponse.from(post);
  }

  /**
   * 게시글 삭제 (Soft Delete)
   *
   * @param postId 게시글 ID
   * @throws IllegalArgumentException 이미 삭제된 게시글인 경우
   */
  @Transactional
  public void deletePost(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new PostNotFoundException(postId));

    if (Boolean.TRUE.equals(post.getIsDeleted())) {
      throw new IllegalArgumentException("이미 삭제된 게시글입니다.");
    }

    post.softDelete();
    postRepository.save(post);
  }

  /**
   * 게시글 복구 (Soft Delete 해제)
   *
   * @param postId 게시글 ID
   * @throws IllegalArgumentException 삭제되지 않은 게시글인 경우
   */
  @Transactional
  public void restorePost(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new PostNotFoundException(postId));

    if (Boolean.FALSE.equals(post.getIsDeleted())) {
      throw new IllegalArgumentException("삭제되지 않은 게시글입니다.");
    }

    post.restore();
    postRepository.save(post);
  }

  /**
   * 게시글 공개 범위 변경
   *
   * @param postId     게시글 ID
   * @param visibility 변경할 공개 범위
   */
  @Transactional
  public void changePostVisibility(Long postId, com.example.myauth.entity.Visibility visibility) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new PostNotFoundException(postId));

    post.setVisibility(visibility);
    postRepository.save(post);
  }

  /**
   * 관리자 댓글 목록 조회 (필터링/페이징)
   *
   * @param keyword  검색 키워드 (내용/작성자명)
   * @param postId   게시글 ID 필터 (null이면 전체)
   * @param pageable 페이징 정보
   * @return 댓글 목록 페이지
   */
  public Page<AdminCommentListResponse> getComments(String keyword, Long postId, Pageable pageable) {
    return commentRepository.findByAdminFilter(normalizeKeyword(keyword), postId, pageable)
        .map(AdminCommentListResponse::from);
  }

  /**
   * 댓글 삭제 (Soft Delete)
   *
   * @param commentId 댓글 ID
   * @throws IllegalArgumentException 이미 삭제된 댓글인 경우
   */
  @Transactional
  public void deleteComment(Long commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (Boolean.TRUE.equals(comment.getIsDeleted())) {
      throw new IllegalArgumentException("이미 삭제된 댓글입니다.");
    }

    comment.softDelete();
    commentRepository.save(comment);
  }

  // ===== 내부 헬퍼 메서드 =====

  /**
   * 사용자 목록을 AdminUserListResponse 목록으로 변환 (배치 카운트 쿼리 사용)
   * 기존 방식: 사용자 1명당 2개 추가 쿼리 (N+1 문제)
   * 개선 방식: 배치 쿼리로 모든 사용자의 카운트를 2개 쿼리로 한번에 조회
   *
   * @param users 사용자 목록
   * @return AdminUserListResponse 목록
   */
  private List<AdminUserListResponse> toAdminUserListResponses(List<User> users) {
    if (users.isEmpty()) {
      return List.of();
    }

    List<Long> userIds = users.stream().map(User::getId).toList();

    // 배치 쿼리로 게시글 수, 팔로워 수를 한번에 조회 (N+1 방지)
    // 기존: 사용자 N명 × 2쿼리 = 2N개 쿼리 → 개선: 2개 쿼리로 해결
    Map<Long, Long> postCounts = toBatchCountMap(postRepository.countPostsByUserIds(userIds));
    Map<Long, Long> followerCounts = toBatchCountMap(followRepository.countFollowersByUserIds(userIds));

    return users.stream()
        .map(user -> AdminUserListResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .profileImage(user.getProfileImage())
            .role(user.getRole().name())
            .status(user.getStatus().name())
            .isActive(user.getIsActive())
            .provider(user.getProvider())
            .postCount(postCounts.getOrDefault(user.getId(), 0L))
            .followerCount(followerCounts.getOrDefault(user.getId(), 0L))
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build())
        .toList();
  }

  /**
   * [userId, count] 배열 목록을 Map<userId, count>로 변환
   *
   * @param results 배치 쿼리 결과 ([userId, count] 배열 목록)
   * @return userId → count 맵
   */
  private Map<Long, Long> toBatchCountMap(List<Object[]> results) {
    return results.stream()
        .collect(Collectors.toMap(
            row -> (Long) row[0],
            row -> (Long) row[1]
        ));
  }

  /**
   * 단일 사용자 변환 (빈 페이지 대비 폴백)
   * 배치 쿼리 대상이 없을 때 사용
   */
  private AdminUserListResponse toAdminUserListResponseFallback(User user) {
    return AdminUserListResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .role(user.getRole().name())
        .status(user.getStatus().name())
        .isActive(user.getIsActive())
        .provider(user.getProvider())
        .postCount(0)
        .followerCount(0)
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .build();
  }

  /**
   * 검색 키워드 정규화
   * 빈 문자열이나 공백만 있는 경우 null로 변환하여 JPQL 조건 처리 용이하게 함
   *
   * @param keyword 원본 키워드
   * @return 정규화된 키워드 (또는 null)
   */
  private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return keyword.trim();
  }
}
