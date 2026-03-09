package com.example.myauth.service;

import com.example.myauth.dto.dm.*;
import com.example.myauth.entity.DmMessage;
import com.example.myauth.entity.DmRoom;
import com.example.myauth.entity.User;
import com.example.myauth.exception.DmRoomNotFoundException;
import com.example.myauth.exception.SelfDmException;
import com.example.myauth.exception.UserNotFoundException;
import com.example.myauth.repository.DmMessageRepository;
import com.example.myauth.repository.DmRoomRepository;
import com.example.myauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DM(Direct Message) 서비스
 * 채팅방 생성, 메시지 전송, 읽음 처리 등 DM 관련 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DmService {

    private final DmRoomRepository dmRoomRepository;
    private final DmMessageRepository dmMessageRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     * 두 사용자 간 채팅방이 이미 존재하면 기존 채팅방을 반환하고,
     * 없으면 새로 생성한다.
     *
     * @param currentUser  현재 로그인한 사용자
     * @param targetUserId 대상 사용자 ID
     * @return 채팅방 정보
     */
    @Transactional
    public DmRoomResponse createOrGetRoom(User currentUser, Long targetUserId) {
        log.info("채팅방 생성/조회 요청 - 현재 사용자: {}, 대상 사용자: {}", currentUser.getId(), targetUserId);

        // 1. 자기 자신에게 DM을 보내는지 확인
        if (currentUser.getId().equals(targetUserId)) {
            throw new SelfDmException();
        }

        // 2. 대상 사용자 존재 여부 확인
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new UserNotFoundException("대상 사용자를 찾을 수 없습니다."));

        // 3. 두 사용자 ID를 정규화 (작은 ID = user1, 큰 ID = user2)
        Long smallerId = Math.min(currentUser.getId(), targetUserId);
        Long largerId = Math.max(currentUser.getId(), targetUserId);

        // 4. 기존 채팅방 조회
        DmRoom room = dmRoomRepository.findByUser1IdAndUser2Id(smallerId, largerId)
            .orElseGet(() -> {
                // 5. 없으면 새로 생성
                log.info("새 채팅방 생성 - user1: {}, user2: {}", smallerId, largerId);
                DmRoom newRoom = DmRoom.create(currentUser, targetUser);
                return dmRoomRepository.save(newRoom);
            });

        log.info("채팅방 반환 - roomId: {}", room.getId());
        return DmRoomResponse.from(room, currentUser.getId());
    }

    /**
     * 내 채팅방 목록 조회
     * 최근 활동순으로 정렬하며, 각 채팅방의 마지막 메시지와 안 읽은 메시지 수를 포함
     *
     * @param currentUser 현재 로그인한 사용자
     * @return 채팅방 목록
     */
    public List<DmRoomListResponse> getMyRooms(User currentUser) {
        log.info("채팅방 목록 조회 - 사용자: {}", currentUser.getId());

        // 1. 내가 참여한 모든 채팅방 조회 (최근 활동순)
        List<DmRoom> rooms = dmRoomRepository.findRoomsByUserId(currentUser.getId());

        // 2. 각 채팅방에 대해 마지막 메시지, 안 읽은 수 조회
        List<DmRoomListResponse> result = new ArrayList<>();
        for (DmRoom room : rooms) {
            // 마지막 메시지 조회
            DmLastMessageResponse lastMessage = dmMessageRepository
                .findTopByRoomIdOrderByCreatedAtDesc(room.getId())
                .map(DmLastMessageResponse::from)
                .orElse(null);

            // 안 읽은 메시지 수 카운트
            long unreadCount = dmMessageRepository.countUnreadMessages(room.getId(), currentUser.getId());

            DmRoomListResponse response = DmRoomListResponse.builder()
                .roomId(room.getId())
                .otherUser(DmUserResponse.from(room.getOtherUser(currentUser.getId())))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .updatedAt(room.getUpdatedAt())
                .build();

            result.add(response);
        }

        log.info("채팅방 목록 조회 완료 - {}개 채팅방", result.size());
        return result;
    }

    /**
     * 채팅방의 메시지 목록 조회 (페이징)
     * 최신 메시지가 먼저 오는 순서로 반환
     *
     * @param currentUser 현재 로그인한 사용자
     * @param roomId      채팅방 ID
     * @param pageable    페이징 정보
     * @return 메시지 페이지
     */
    public Page<DmMessageResponse> getMessages(User currentUser, Long roomId, Pageable pageable) {
        log.info("메시지 목록 조회 - 사용자: {}, 채팅방: {}", currentUser.getId(), roomId);

        // 1. 채팅방 조회 및 참여자 확인
        DmRoom room = findRoomAndValidateParticipant(roomId, currentUser.getId());

        // 2. 메시지 페이징 조회
        Page<DmMessage> messages = dmMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        log.info("메시지 목록 조회 완료 - {}개 메시지", messages.getTotalElements());
        return messages.map(msg -> DmMessageResponse.from(msg, currentUser.getId()));
    }

    /**
     * 메시지 전송
     * 메시지를 저장하고 채팅방의 마지막 활동 시간을 갱신
     *
     * @param currentUser 현재 로그인한 사용자 (발신자)
     * @param roomId      채팅방 ID
     * @param content     메시지 내용
     * @return 전송된 메시지 정보
     */
    @Transactional
    public DmMessageResponse sendMessage(User currentUser, Long roomId, String content) {
        log.info("메시지 전송 - 사용자: {}, 채팅방: {}", currentUser.getId(), roomId);

        // 1. 채팅방 조회 및 참여자 확인
        DmRoom room = findRoomAndValidateParticipant(roomId, currentUser.getId());

        // 2. 메시지 생성 및 저장
        DmMessage message = DmMessage.create(room, currentUser, content);
        DmMessage savedMessage = dmMessageRepository.save(message);

        // 3. 채팅방 마지막 활동 시간 갱신 (updatedAt은 @UpdateTimestamp로 자동 갱신)
        // save를 호출하여 updatedAt 자동 갱신 트리거
        dmRoomRepository.save(room);

        log.info("메시지 전송 완료 - messageId: {}", savedMessage.getId());
        return DmMessageResponse.from(savedMessage, currentUser.getId());
    }

    /**
     * 읽음 처리 (일괄)
     * 해당 채팅방에서 상대방이 보낸 안 읽은 메시지를 모두 읽음으로 변경
     *
     * @param currentUser 현재 로그인한 사용자
     * @param roomId      채팅방 ID
     */
    @Transactional
    public void markAsRead(User currentUser, Long roomId) {
        log.info("읽음 처리 - 사용자: {}, 채팅방: {}", currentUser.getId(), roomId);

        // 1. 채팅방 조회 및 참여자 확인
        findRoomAndValidateParticipant(roomId, currentUser.getId());

        // 2. 일괄 읽음 처리 (상대방이 보낸 안 읽은 메시지만)
        int updatedCount = dmMessageRepository.markAllAsRead(roomId, currentUser.getId());

        log.info("읽음 처리 완료 - {}개 메시지 업데이트", updatedCount);
    }

    /**
     * 채팅방 나가기 (삭제)
     * 채팅방과 관련 메시지를 모두 삭제
     *
     * @param currentUser 현재 로그인한 사용자
     * @param roomId      채팅방 ID
     */
    @Transactional
    public void leaveRoom(User currentUser, Long roomId) {
        log.info("채팅방 나가기 - 사용자: {}, 채팅방: {}", currentUser.getId(), roomId);

        // 1. 채팅방 조회 및 참여자 확인
        DmRoom room = findRoomAndValidateParticipant(roomId, currentUser.getId());

        // 2. 메시지 먼저 삭제 (FK 제약 조건)
        dmMessageRepository.deleteAllByRoomId(roomId);

        // 3. 채팅방 삭제
        dmRoomRepository.delete(room);

        log.info("채팅방 삭제 완료 - roomId: {}", roomId);
    }

    /**
     * 새 메시지 폴링
     * lastMessageId 이후에 생성된 메시지만 반환
     * 프론트엔드에서 주기적으로 호출하여 실시간성 구현
     *
     * @param currentUser   현재 로그인한 사용자
     * @param roomId        채팅방 ID
     * @param lastMessageId 마지막으로 받은 메시지 ID
     * @return 새로운 메시지 목록 (오래된 순)
     */
    public List<DmMessageResponse> getNewMessages(User currentUser, Long roomId, Long lastMessageId) {
        // 1. 채팅방 조회 및 참여자 확인
        findRoomAndValidateParticipant(roomId, currentUser.getId());

        // 2. lastMessageId 이후의 새 메시지 조회
        List<DmMessage> newMessages = dmMessageRepository.findNewMessages(roomId, lastMessageId);

        return newMessages.stream()
            .map(msg -> DmMessageResponse.from(msg, currentUser.getId()))
            .collect(Collectors.toList());
    }

    /**
     * 전체 안 읽은 메시지 수 조회
     * 모든 채팅방의 안 읽은 메시지 수 합산
     * 네비게이션 바의 DM 아이콘 뱃지에 사용
     *
     * @param currentUser 현재 로그인한 사용자
     * @return 전체 안 읽은 메시지 수
     */
    public DmUnreadCountResponse getTotalUnreadCount(User currentUser) {
        log.info("전체 안 읽은 메시지 수 조회 - 사용자: {}", currentUser.getId());

        // 1. 내가 참여한 모든 채팅방 ID 조회
        List<DmRoom> rooms = dmRoomRepository.findRoomsByUserId(currentUser.getId());
        List<Long> roomIds = rooms.stream()
            .map(DmRoom::getId)
            .collect(Collectors.toList());

        // 2. 채팅방이 없으면 0 반환
        if (roomIds.isEmpty()) {
            return DmUnreadCountResponse.builder()
                .totalUnreadCount(0)
                .build();
        }

        // 3. 전체 안 읽은 메시지 수 합산
        long totalUnread = dmMessageRepository.countTotalUnreadMessages(roomIds, currentUser.getId());

        log.info("전체 안 읽은 메시지 수: {}", totalUnread);
        return DmUnreadCountResponse.builder()
            .totalUnreadCount(totalUnread)
            .build();
    }

    // ===== 내부 헬퍼 메서드 =====

    /**
     * 채팅방 조회 및 참여자 검증
     * 채팅방이 존재하지 않거나 현재 사용자가 참여자가 아니면 예외 발생
     *
     * @param roomId 채팅방 ID
     * @param userId 현재 사용자 ID
     * @return 검증된 DmRoom 엔티티
     */
    private DmRoom findRoomAndValidateParticipant(Long roomId, Long userId) {
        DmRoom room = dmRoomRepository.findByIdWithUsers(roomId)
            .orElseThrow(() -> new DmRoomNotFoundException());

        if (!room.isParticipant(userId)) {
            throw new DmRoomNotFoundException("해당 채팅방에 접근 권한이 없습니다.");
        }

        return room;
    }
}
