package kr.momenta.gateway.kis.api.res;

import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VolumeSpikeQuery extends BaseRankingQuery {

    @Builder.Default
    private String mixn = "0"; // MIXN 필수

}
