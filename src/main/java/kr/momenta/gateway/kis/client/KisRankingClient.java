package kr.momenta.gateway.kis.client;

import kr.momenta.gateway.kis.api.res.*;
import kr.momenta.gateway.kis.property.KisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KisRankingClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;
    private final KisTokenService tokenService;

    public Mono<KisApiResponse<OverseasStockRankingItem>> tradeAmount(TradeAmountQuery q) {
        return get(KisOverseasRankingApi.TRADE_AMOUNT, q).timeout(TIMEOUT);
    }

    public Mono<KisApiResponse<OverseasStockRankingItem>> volumeSpike(VolumeSpikeQuery q) {
        return get(KisOverseasRankingApi.VOLUME_SPIKE, q).timeout(TIMEOUT);
    }

    public Mono<KisApiResponse<OverseasStockRankingItem>> buyStrength(BuyStrengthQuery q) {
        return get(KisOverseasRankingApi.BUY_STRENGTH, q).timeout(TIMEOUT);
    }

    public Mono<KisApiResponse<OverseasStockRankingItem>> priceMoveUp(PriceMoveQuery base) {
        PriceMoveQuery q = PriceMoveQuery.builder()
                .keyb(base.getKeyb()).auth(base.getAuth())
                .excd(base.getExcd()).volRang(base.getVolRang())
                .mixn(base.getMixn())
                .gubn("1")
                .build();
        return get(KisOverseasRankingApi.PRICE_CHANGE, q).timeout(TIMEOUT);
    }

    public Mono<KisApiResponse<OverseasStockRankingItem>> priceMoveDown(PriceMoveQuery base) {
        PriceMoveQuery q = PriceMoveQuery.builder()
                .keyb(base.getKeyb()).auth(base.getAuth())
                .excd(base.getExcd()).volRang(base.getVolRang())
                .mixn(base.getMixn())
                .gubn("0")
                .build();
        return get(KisOverseasRankingApi.PRICE_CHANGE, q).timeout(TIMEOUT);
    }

    private Mono<KisApiResponse<OverseasStockRankingItem>> get(KisOverseasRankingApi api, BaseRankingQuery q) {
        q.validate();

        return tokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder -> {
                                    var b = uriBuilder.path(api.getPath())
                                            // 공통
                                            .queryParam("KEYB", nullToEmpty(q.getKeyb()))
                                            .queryParam("AUTH", nullToEmpty(q.getAuth()))
                                            .queryParam("EXCD", q.getExcd())
                                            .queryParam("VOL_RANG", nullToEmpty(q.getVolRang()));

                                    if (q instanceof TradeAmountQuery ta) {
                                        b.queryParam("NDAY", ta.getNday())
                                                .queryParam("PRC1", ta.getPrc1())
                                                .queryParam("PRC2", ta.getPrc2());
                                    } else if (q instanceof VolumeSpikeQuery vs) {
                                        b.queryParam("MIXN", vs.getMixn());
                                    } else if (q instanceof BuyStrengthQuery bs) {
                                        b.queryParam("NDAY", bs.getNday());
                                    } else if (q instanceof PriceMoveQuery pm) {
                                        b.queryParam("GUBN", pm.getGubn())
                                                .queryParam("MIXN", pm.getMixn());
                                    }

                                    return b.build();
                                })
                                .headers(h -> applyHeaders(h, token, api))
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<KisApiResponse<OverseasStockRankingItem>>() {})
                );
    }

    private void applyHeaders(HttpHeaders h, String token, KisOverseasRankingApi api) {
        h.setBearerAuth(token);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("appkey", kisProperties.getAppKey());
        h.add("appsecret", kisProperties.getAppSecret());
        h.add("tr_id", api.getTrId());
        h.add("custtype", "P");
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
