package com.example.myauth.exception;

/**
 * DM 채팅방을 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됨
 */
public class DmRoomNotFoundException extends RuntimeException {

    public DmRoomNotFoundException(String message) {
        super(message);
    }

    public DmRoomNotFoundException() {
        super("채팅방을 찾을 수 없습니다.");
    }
}
