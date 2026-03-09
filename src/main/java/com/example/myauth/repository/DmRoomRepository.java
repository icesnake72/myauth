package com.example.myauth.repository;

import com.example.myauth.entity.DmRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DM 채팅방 리포지토리
 * 채팅방 CRUD 및 사용자별 채팅방 조회 기능 제공
 */
@Repository
public interface DmRoomRepository extends JpaRepository<DmRoom, Long> {

    /**
     * 두 사용자의 기존 채팅방 조회
     * 호출 전에 반드시 user1Id < user2Id를 보장해야 함
     *
     * @param user1Id 작은 사용자 ID
     * @param user2Id 큰 사용자 ID
     * @return 채팅방 (존재하면)
     */
    Optional<DmRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    /**
     * 채팅방 상세 조회 (N+1 방지 — user1, user2 JOIN FETCH)
     *
     * @param roomId 채팅방 ID
     * @return user1, user2가 함께 로딩된 채팅방
     */
    @Query("SELECT r FROM DmRoom r JOIN FETCH r.user1 JOIN FETCH r.user2 WHERE r.id = :roomId")
    Optional<DmRoom> findByIdWithUsers(@Param("roomId") Long roomId);

    /**
     * 특정 사용자가 참여한 모든 채팅방 조회 (최근 활동순)
     * user1 또는 user2로 참여한 채팅방 모두 포함
     * N+1 방지를 위해 user1, user2를 JOIN FETCH
     *
     * @param userId 사용자 ID
     * @return 최근 활동순으로 정렬된 채팅방 목록
     */
    @Query("SELECT r FROM DmRoom r JOIN FETCH r.user1 JOIN FETCH r.user2 " +
           "WHERE r.user1.id = :userId OR r.user2.id = :userId " +
           "ORDER BY r.updatedAt DESC")
    List<DmRoom> findRoomsByUserId(@Param("userId") Long userId);
}
