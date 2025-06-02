package com.groom.marky.domain.request;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "station_lane")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StationLane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stationNm;     // 역명
    private String stationCd;     // 전철역 코드
    private String lineNum;       // 호선
    private String frCode;        // 외부 코드

    private String stationNmEng; // 영문명
    private String stationNmJpn; // 일문명
    private String stationNmChn; // 중문명
}
