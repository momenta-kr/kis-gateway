package kr.momenta.gateway.kis.api.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisCandleResponse {
    public String rt_cd;     // 성공실패 여부
    public String msg_cd;    // 응답코드
    public String msg1;      // 응답메세지
    public List<KisOutput1> output1;  // object array
    public List<KisCandleRow> output2; // array



    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KisOutput1 {
        public String rsym; // 실시간종목코드
        public String zdiv;
        public String stim;
        public String etim;
        public String sktm;
        public String ektm;
        public String next;
        public String more;
        public String nrec;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KisCandleRow {
        public String tymd; // 현지영업일자
        public String xymd; // 현지기준일자
        public String xhms; // 현지기준시간
        public String kymd; // 한국기준일자
        public String khms; // 한국기준시간
        public String open; // 시가
        public String high; // 고가
        public String low;  // 저가
        public String last; // 종가
        public String evol; // 체결량
        public String eamt; // 체결대금
    }
}