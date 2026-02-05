package kr.momenta.gateway.kis.infra;

import kr.momenta.gateway.kis.api.res.AccessTokenResponse;
import lombok.Data;

import java.io.Serializable;

@Data
public class GlobalToken implements Serializable {
    private String id;
    private String accessToken;
    private long expiresIn;
    private String tokenType;
    private String accessTokenExpired;

    public static GlobalToken from(AccessTokenResponse resp) {
        GlobalToken token = new GlobalToken();
        token.setId("globalToken");
        token.setAccessToken(resp.getAccessToken());
        token.setExpiresIn(resp.getExpiresIn());
        token.setTokenType(resp.getTokenType());
        token.setAccessTokenExpired(resp.getAccessTokenExpired());
        return token;
    }

    public static GlobalToken ws(String approvalKey) {
        GlobalToken token = new GlobalToken();
        token.setId("globalWsToken");
        token.setAccessToken(approvalKey);
        return token;
    }
}
