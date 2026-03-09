package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.dm.*;
import com.example.myauth.entity.User;
import com.example.myauth.service.DmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DM(Direct Message) 컨트롤러
 * 1:1 다이렉트 메시지 관련 REST API 엔드포인트 제공
 *
 * 【API 목록】
 * - POST   /api/dm/rooms                        → 채팅방 생성/조회
 * - GET    /api/dm/rooms                        → 내 채팅방 목록
 * - GET    /api/dm/rooms/{roomId}/messages       → 메시지 목록 (페이징)
 * - POST   /api/dm/rooms/{roomId}/messages       → 메시지 전송
 * - PUT    /api/dm/rooms/{roomId}/read           → 읽음 처리
 * - DELETE /api/dm/rooms/{roomId}                → 채팅방 나가기
 * - GET    /api/dm/rooms/{roomId}/messages/new   → 새 메시지 폴링
 * - GET    /api/dm/unread-count                  → 전체 안 읽은 메시지 수
 */
@Slf4j
@RestController
@RequestMapping("/api/dm")
@RequiredArgsConstructor
public class DmController {

    private final DmService dmService;

    /**
     * 채팅방 생성 또는 기존 채팅방 조회
     * 대상 사용자와의 채팅방이 이미 존재하면 기존 채팅방을 반환하고,
     * 없으면 새로 생성하여 반환
     *
     * POST /api/dm/rooms
     *
     * @param user    현재 로그인한 사용자
     * @param request 채팅방 생성 요청 (targetUserId)
     * @return 채팅방 정보
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<DmRoomResponse>> createRoom(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody DmRoomCreateRequest request
    ) {
        log.info("채팅방 생성/조회 요청 - 사용자: {}, 대상: {}", user.getId(), request.getTargetUserId());

        DmRoomResponse response = dmService.createOrGetRoom(user, request.getTargetUserId());

        return ResponseEntity.ok(ApiResponse.success("채팅방 조회 성공", response));
    }

    /**
     * 내 채팅방 목록 조회
     * 최근 활동순으로 정렬되며, 각 채팅방의 마지막 메시지와 안 읽은 메시지 수 포함
     *
     * GET /api/dm/rooms
     *
     * @param user 현재 로그인한 사용자
     * @return 채팅방 목록
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<DmRoomListResponse>>> getMyRooms(
        @AuthenticationPrincipal User user
    ) {
        log.info("채팅방 목록 조회 요청 - 사용자: {}", user.getId());

        List<DmRoomListResponse> response = dmService.getMyRooms(user);

        return ResponseEntity.ok(ApiResponse.success("채팅방 목록 조회 성공", response));
    }

    /**
     * 채팅방의 메시지 목록 조회 (페이징)
     * 최신 메시지가 먼저 오는 순서로 반환
     *
     * GET /api/dm/rooms/{roomId}/messages?page=0&size=50
     *
     * @param user   현재 로그인한 사용자
     * @param roomId 채팅방 ID
     * @param page   페이지 번호 (0부터 시작, 기본값 0)
     * @param size   페이지 크기 (기본값 50, 최대 50)
     * @return 메시지 페이지
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<DmMessageResponse>>> getMessages(
        @AuthenticationPrincipal User user,
        @PathVariable Long roomId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        log.info("메시지 목록 조회 요청 - 사용자: {}, 채팅방: {}", user.getId(), roomId);

        // 페이지 크기 최대값 제한
        if (size > 50) size = 50;

        Pageable pageable = PageRequest.of(page, size);
        Page<DmMessageResponse> response = dmService.getMessages(user, roomId, pageable);

        return ResponseEntity.ok(ApiResponse.success("메시지 목록 조회 성공", response));
    }

    /**
     * 메시지 전송
     * 채팅방에 새 텍스트 메시지를 전송
     *
     * POST /api/dm/rooms/{roomId}/messages
     *
     * @param user    현재 로그인한 사용자
     * @param roomId  채팅방 ID
     * @param request 메시지 전송 요청 (content)
     * @return 전송된 메시지 정보
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<DmMessageResponse>> sendMessage(
        @AuthenticationPrincipal User user,
        @PathVariable Long roomId,
        @Valid @RequestBody DmMessageCreateRequest request
    ) {
        log.info("메시지 전송 요청 - 사용자: {}, 채팅방: {}", user.getId(), roomId);

        DmMessageResponse response = dmService.sendMessage(user, roomId, request.getContent());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("메시지 전송 성공", response));
    }

    /**
     * 읽음 처리 (일괄)
     * 해당 채팅방에서 상대방이 보낸 안 읽은 메시지를 모두 읽음으로 변경
     *
     * PUT /api/dm/rooms/{roomId}/read
     *
     * @param user   현재 로그인한 사용자
     * @param roomId 채팅방 ID
     * @return 성공 메시지
     */
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
        @AuthenticationPrincipal User user,
        @PathVariable Long roomId
    ) {
        log.info("읽음 처리 요청 - 사용자: {}, 채팅방: {}", user.getId(), roomId);

        dmService.markAsRead(user, roomId);

        return ResponseEntity.ok(ApiResponse.success("읽음 처리 완료"));
    }

    /**
     * 채팅방 나가기 (삭제)
     * 채팅방과 관련 메시지를 모두 삭제
     *
     * DELETE /api/dm/rooms/{roomId}
     *
     * @param user   현재 로그인한 사용자
     * @param roomId 채팅방 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
        @AuthenticationPrincipal User user,
        @PathVariable Long roomId
    ) {
        log.info("채팅방 나가기 요청 - 사용자: {}, 채팅방: {}", user.getId(), roomId);

        dmService.leaveRoom(user, roomId);

        return ResponseEntity.ok(ApiResponse.success("채팅방을 나갔습니다."));
    }

    /**
     * 새 메시지 폴링
     * lastMessageId 이후에 생성된 메시지만 반환
     * 프론트엔드에서 주기적으로 호출하여 실시간성 구현 (예: 3초 간격)
     *
     * GET /api/dm/rooms/{roomId}/messages/new?lastMessageId=100
     *
     * @param user          현재 로그인한 사용자
     * @param roomId        채팅방 ID
     * @param lastMessageId 마지막으로 받은 메시지 ID (기본값 0)
     * @return 새로운 메시지 목록 (오래된 순)
     */
    @GetMapping("/rooms/{roomId}/messages/new")
    public ResponseEntity<ApiResponse<List<DmMessageResponse>>> getNewMessages(
        @AuthenticationPrincipal User user,
        @PathVariable Long roomId,
        @RequestParam(defaultValue = "0") Long lastMessageId
    ) {
        List<DmMessageResponse> response = dmService.getNewMessages(user, roomId, lastMessageId);

        return ResponseEntity.ok(ApiResponse.success("새 메시지 조회 성공", response));
    }

    /**
     * 전체 안 읽은 메시지 수 조회
     * 모든 채팅방의 안 읽은 메시지 수 합산
     * 네비게이션 바의 DM 아이콘 뱃지에 표시
     *
     * GET /api/dm/unread-count
     *
     * @param user 현재 로그인한 사용자
     * @return 전체 안 읽은 메시지 수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<DmUnreadCountResponse>> getUnreadCount(
        @AuthenticationPrincipal User user
    ) {
        log.info("전체 안 읽은 메시지 수 조회 - 사용자: {}", user.getId());

        DmUnreadCountResponse response = dmService.getTotalUnreadCount(user);

        return ResponseEntity.ok(ApiResponse.success("안 읽은 메시지 수 조회 성공", response));
    }
}
