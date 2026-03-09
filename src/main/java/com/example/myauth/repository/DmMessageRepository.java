package com.example.myauth.repository;

import com.example.myauth.entity.DmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DM 메시지 리포지토리
 * 메시지 CRUD, 페이징, 읽음 처리, 폴링 기능 제공
 */
@Repository
public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {

    /**
     * 채팅방별 메시지 페이징 조회 (최신 순)
     * sender를 JOIN FETCH하여 N+1 방지
     *
     * @param roomId   채팅방 ID
     * @param pageable 페이징 정보
     * @return 메시지 페이지
     */
    @Query(value = "SELECT m FROM DmMessage m JOIN FETCH m.sender WHERE m.room.id = :roomId ORDER BY m.createdAt DESC",
           countQuery = "SELECT COUNT(m) FROM DmMessage m WHERE m.room.id = :roomId")
    Page<DmMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 채팅방의 마지막 메시지 조회
     * 채팅방 목록에서 마지막 메시지 미리보기에 사용
     *
     * @param roomId 채팅방 ID
     * @return 가장 최근 메시지
     */
    Optional<DmMessage> findTopByRoomIdOrderByCreatedAtDesc(Long roomId);

    /**
     * 채팅방별 안 읽은 메시지 수 카운트
     * 상대방이 보낸 메시지 중 아직 읽지 않은 것만 카운트
     *
     * @param roomId 채팅방 ID
     * @param userId 현재 사용자 ID (본인이 보낸 메시지 제외)
     * @return 안 읽은 메시지 수
     */
    @Query("SELECT COUNT(m) FROM DmMessage m " +
           "WHERE m.room.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 일괄 읽음 처리
     * 해당 채팅방에서 상대방이 보낸 안 읽은 메시지를 모두 읽음으로 변경
     *
     * @param roomId 채팅방 ID
     * @param userId 현재 사용자 ID (본인이 보낸 메시지 제외)
     * @return 읽음 처리된 메시지 수
     */
    @Modifying
    @Query("UPDATE DmMessage m SET m.isRead = true " +
           "WHERE m.room.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
    int markAllAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /**
     * 새 메시지 폴링 (lastMessageId 이후 메시지 조회)
     * 프론트엔드에서 주기적으로 호출하여 실시간성 구현
     *
     * @param roomId        채팅방 ID
     * @param lastMessageId 마지막으로 받은 메시지 ID
     * @return lastMessageId 이후에 생성된 메시지 목록 (오래된 순)
     */
    @Query("SELECT m FROM DmMessage m JOIN FETCH m.sender " +
           "WHERE m.room.id = :roomId AND m.id > :lastMessageId ORDER BY m.createdAt ASC")
    List<DmMessage> findNewMessages(@Param("roomId") Long roomId, @Param("lastMessageId") Long lastMessageId);

    /**
     * 전체 안 읽은 메시지 수 (모든 채팅방 합산)
     * 네비게이션 바의 DM 아이콘 뱃지에 사용
     *
     * @param roomIds 사용자가 참여한 채팅방 ID 목록
     * @param userId  현재 사용자 ID
     * @return 전체 안 읽은 메시지 수
     */
    @Query("SELECT COUNT(m) FROM DmMessage m " +
           "WHERE m.room.id IN :roomIds AND m.sender.id != :userId AND m.isRead = false")
    long countTotalUnreadMessages(@Param("roomIds") List<Long> roomIds, @Param("userId") Long userId);

    /**
     * 채팅방의 모든 메시지 삭제
     * 채팅방 나가기(삭제) 시 사용
     *
     * @param roomId 채팅방 ID
     */
    void deleteAllByRoomId(Long roomId);
}
