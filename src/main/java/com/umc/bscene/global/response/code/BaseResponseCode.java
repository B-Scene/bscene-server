package com.umc.bscene.global.response.code;

public interface BaseResponseCode {

    /*
     * HttpStatus의 경우, HTTP 프로토콜에서 사용
     * 통신 단에서 처리되는 타입이므로, 애플리케이션 내에서 사용하기 부적합(DIP 위배)
     * 따라서, int 타입으로 수정
     */
    int getStatus();
    String getCode();
    String getMessage();
}
