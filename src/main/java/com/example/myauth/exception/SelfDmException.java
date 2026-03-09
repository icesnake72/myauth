package com.example.myauth.exception;

/**
 * 자기 자신에게 DM을 보내려고 할 때 발생하는 예외
 * HTTP 400 Bad Request로 매핑됨
 */
public class SelfDmException extends RuntimeException {

    public SelfDmException(String message) {
        super(message);
    }

    public SelfDmException() {
        super("자기 자신에게는 메시지를 보낼 수 없습니다.");
    }
}
