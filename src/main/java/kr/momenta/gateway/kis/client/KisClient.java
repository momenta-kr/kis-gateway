package kr.momenta.gateway.kis.client;

import kr.momenta.gateway.kis.api.res.*;
import kr.momenta.gateway.kis.property.KisProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class KisClient {

    @Qualifier("kisWebClient")
    private final WebClient kisWebClient;
    @Qualifier("kisStockWebClient")
    private final WebClient kisStockWebClient;

    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    /**
     * https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/inquire-price
     * 주식현재가 시세[v1_국내주식-008]
     */
    public Mono<DomesticStockCurrentPriceResponse> fetchDomesticStockCurrentPrice(String symbol) {
        return fetchDomesticStockCurrentPrice(symbol, "J"); // 기본: KRX
    }

    public Mono<DomesticStockCurrentPriceResponse> fetchDomesticStockCurrentPrice(String symbol, String marketDivCode) {
        final String normalizedSymbol = normalizeDomesticSymbol(symbol);

        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisStockWebClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                                        .queryParam("FID_COND_MRKT_DIV_CODE", marketDivCode) // J:KRX, NX:NXT, UN:통합
                                        .queryParam("FID_INPUT_ISCD", normalizedSymbol)      // 005930 / Q000660(ETN)
                                        .build())
                                .headers(commonHeaders("FHKST01010100", token))
                                .retrieve()
                                .bodyToMono(DomesticStockCurrentPriceResponse.class)
                );
    }

    private static String normalizeDomesticSymbol(String symbol) {
        if (symbol == null) return "";
        String s = symbol.trim();
        if (s.isEmpty()) return s;

        // ETN: "Q" + 6자리 (문서 기준)
        if (s.matches("^Q\\d{1,6}$")) {
            String digits = s.substring(1);
            return "Q" + org.apache.commons.lang3.StringUtils.leftPad(digits, 6, '0');
        }

        // 일반 종목: 6자리 0-padding
        if (s.matches("^\\d{1,6}$")) {
            return org.apache.commons.lang3.StringUtils.leftPad(s, 6, '0');
        }

        return s;
    }





    public Mono<CandlesResponse> fetchCandles(String excd, String symbol, int nmin, int limit) {
        // 문서상 NREC 최대 120
        int nrec = Math.min(Math.max(limit, 1), 120);

        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice")
                                        .queryParam("AUTH", "")                 // 문서: "" 공백
                                        .queryParam("EXCD", excd)               // NYS/NAS/AMS/HKS...
                                        .queryParam("SYMB", symbol)             // TSLA/AAPL...
                                        .queryParam("NMIN", nmin)               // 1=1분,2=2분...
                                        .queryParam("PINC", "0")                // 0=당일, 1=전일포함
                                        .queryParam("NEXT", "")                 // 처음조회: ""
                                        .queryParam("NREC", String.valueOf(nrec))
                                        .queryParam("FILL", "")                 // "" 공백
                                        .queryParam("KEYB", "")                 // 처음조회: ""
                                        .build()
                                )
                                .headers(commonHeaders("HHDFS76950200", token))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, resp ->
                                        resp.bodyToMono(String.class)
                                                .defaultIfEmpty("")
                                                .flatMap(body -> Mono.error(
                                                        new RuntimeException(
                                                                "KIS candle API HTTP error: status=" + resp.statusCode() + " body=" + body
                                                        )
                                                ))
                                )
                                .bodyToMono(KisCandleResponse.class)
                                .timeout(Duration.ofSeconds(4))
                )
                .flatMap(res -> {
                    if (res == null) {
                        return Mono.error(new RuntimeException("KIS candle API empty response"));
                    }
                    if (!"0".equals(res.rt_cd)) {
                        return Mono.error(new RuntimeException(
                                "KIS candle API failed: rt_cd=" + res.rt_cd + " msg_cd=" + res.msg_cd + " msg1=" + res.msg1
                        ));
                    }

                    List<Candle> candles = new ArrayList<>();
                    if (res.output2 != null) {
                        for (KisCandleResponse.KisCandleRow r : res.output2) {
                            candles.add(toCandle(r));
                        }
                    }

                    return Mono.just(new CandlesResponse(excd, symbol, nmin, candles));
                })
                .onErrorMap(WebClientResponseException.class, e ->
                        new RuntimeException(
                                "KIS candle API HTTP error: status=" + e.getStatusCode() + " body=" + e.getResponseBodyAsString(),
                                e
                        )
                )
                .onErrorMap(e -> (e instanceof RuntimeException) ? e : new RuntimeException("KIS candle API error", e));
    }

    private Candle toCandle(KisCandleResponse.KisCandleRow r) {
        // 문서: kymd(YYYYMMDD) + khms(HHMMSS) 가 “한국기준”
        LocalDate date = LocalDate.parse(r.kymd, DateTimeFormatter.BASIC_ISO_DATE);
        LocalTime time = LocalTime.parse(r.khms, DateTimeFormatter.ofPattern("HHmmss"));
        LocalDateTime kstDateTime = LocalDateTime.of(date, time);

        return new Candle(
                kstDateTime,
                bd(r.open),
                bd(r.high),
                bd(r.low),
                bd(r.last),
                lng(r.evol),
                lng(r.eamt)
        );
    }

    private long lng(String s) {
        if (s == null || s.isBlank()) return 0L;
        // 일부 값이 소수점으로 올 가능성 대비
        return new BigDecimal(s.trim()).longValue();
    }

    private BigDecimal bd(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(s.trim());
    }

    public Mono<IndustryIndexPriceResponse> fetchIndustryIndexPrice() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                                                .queryParam("FID_INPUT_ISCD", "0001")
                                                .queryParam("FID_COND_SCR_DIV_CODE", "20214")
                                                .queryParam("FID_MRKT_CLS_CODE", "K")
                                                .queryParam("FID_BLNG_CLS_CODE", "3")
                                                .build()
                                )
                                .headers(commonHeaders("FHPUP02140000", token))
                                .retrieve()
                                .bodyToMono(IndustryIndexPriceResponse.class)
                );
    }

    /**
     * https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice
     * 국내주식기간별시세(일/주/월/년)[v1_국내주식-016]
     */
    public Mono<DomesticStockPriceResponse> fetchDomesticStockPeriodPrices(String symbol, String period, String from, String to) {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                        .queryParam("FID_INPUT_ISCD", symbol)
                                        .queryParam("FID_INPUT_DATE_1", from)
                                        .queryParam("FID_INPUT_DATE_2", to)
                                        .queryParam("FID_PERIOD_DIV_CODE", period)
                                        .queryParam("FID_ORG_ADJ_PRC", "0")
                                        .build())
                                .headers(commonHeaders("FHKST03010100", token))
                                .retrieve()
                                .bodyToMono(DomesticStockPriceResponse.class));
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("00yyyyMMdd");
    /**
     * https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/invest-opinion
     * 국내주식 종목투자의견 [국내주식-188]
     */
    public Mono<InvestmentOpinionApiResponse> fetchInvestmentOpinion(String symbol) {
        LocalDateTime now = LocalDateTime.now();
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/uapi/domestic-stock/v1/quotations/invest-opinion")
                                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                        .queryParam("FID_COND_SCR_DIV_CODE", 16633)
                                        .queryParam("FID_INPUT_ISCD", symbol)
                                        .queryParam("FID_INPUT_DATE_1", formatter.format(now.minusYears(1)))
                                        .queryParam("FID_INPUT_DATE_2", formatter.format(now))
                                        .build())
                                .headers(commonHeaders("FHKST663300C0", token))
                                .retrieve()
                                .bodyToMono(InvestmentOpinionApiResponse.class));
    }

    /**
     * 국내주식 등락률 순위
     */
    public Mono<FluctuationResponse> fetchTopGainers() {
        return fetchFluctuation(true);
    }

    /**
     * 국내주식 등락률 순위
     */
    public Mono<FluctuationResponse> fetchTopLosers() {
        return fetchFluctuation(false);
    }

    private Mono<FluctuationResponse> fetchFluctuation(boolean isUp) {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                                                .queryParam("fid_rsfl_rate2", "")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20170")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_rank_sort_cls_code", isUp ? "0" : "1")
                                                .queryParam("fid_input_cnt_1", "0")
                                                .queryParam("fid_prc_cls_code", isUp ? "0" : "1")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_rsfl_rate1", "")
                                                .build())
                                .headers(commonHeaders("FHPST01700000", token))
                                .retrieve()
                                .bodyToMono(FluctuationResponse.class)
                );
    }

    public Mono<IndexPriceResponse> fetchIndexPrice() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                                                .queryParam("FID_INPUT_ISCD", "0001")
                                                .build())
                                .headers(commonHeaders("FHPUP02100000", token))
                                .retrieve()
                                .bodyToMono(IndexPriceResponse.class)
                );
    }

    public Mono<MarketCapRankResponse> fetchMarketCapRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/market-cap")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20174")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .build())
                                .headers(commonHeaders("FHPST01740000", token))
                                .retrieve()
                                .bodyToMono(MarketCapRankResponse.class));
    }

    public Mono<VolumeRankResponse> fetchVolumeRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                                .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                                                .queryParam("FID_INPUT_ISCD", "0000")
                                                .queryParam("FID_DIV_CLS_CODE", "0")
                                                .queryParam("FID_BLNG_CLS_CODE", "3")
                                                .queryParam("FID_TRGT_CLS_CODE", "111111111")
                                                .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                                                .queryParam("FID_INPUT_PRICE_1", "")
                                                .queryParam("FID_INPUT_PRICE_2", "")
                                                .queryParam("FID_VOL_CNT", "")
                                                .queryParam("FID_INPUT_DATE_1", "")
                                                .build())
                                .headers(commonHeaders("FHPST01710000", token))
                                .retrieve()
                                .bodyToMono(VolumeRankResponse.class));
    }

    public Mono<StocksOfInterestRankResponse> fetchStocksOfInterestRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/top-interest-stock")
                                                .queryParam("fid_input_iscd_2", "000000")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20180")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_cnt_1", "1")
                                                .build())
                                .headers(commonHeaders("FHPST01800000", token))
                                .retrieve()
                                .bodyToMono(StocksOfInterestRankResponse.class));
    }

    public Mono<TradeStrengthResponse> fetchTradeStrengthRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/volume-power")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20168")
                                                .queryParam("fid_input_iscd", "0000")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .build())
                                .headers(commonHeaders("FHPST01680000", token))
                                .retrieve()
                                .bodyToMono(TradeStrengthResponse.class));
    }


    private @NonNull Consumer<HttpHeaders> commonHeaders(String transactionId, String token) {
        return httpHeaders -> {
            httpHeaders.setBearerAuth(token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("appkey", kisProperties.getAppKey());
            httpHeaders.set("appsecret", kisProperties.getAppSecret());
            httpHeaders.set("tr_id", transactionId);
            httpHeaders.set("custtype", "P");
        };
    }

    public Mono<String> fetchWsToken() {
        return kisTokenService.getWebSocketToken();
    }
}
