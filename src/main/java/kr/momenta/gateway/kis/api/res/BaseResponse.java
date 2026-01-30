package kr.momenta.gateway.kis.api.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BaseResponse {

    @JsonProperty("rt_cd")
    private String resultCode;           // 성공/실패 코드

    @JsonProperty("msg_cd")
    private String messageCode;          // 응답 코드

    @JsonProperty("msg1")
    private String message;              // 응답 메시지
}
