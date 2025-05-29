package com.groom.marky.domain.request;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Table(name= "subway_congestion")
public class SubwayCongestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //지하철 메타 정보
    private String stationName; //출발역
    private Integer stationCode;//역번호
    private Integer lineNumber; //호선
    private String direction;   //상하구분 (상선, 하선)
    private String dayType;     //요일구분 (평일, 주말)

    // 시간별 정보
    private LocalTime timeSlot; //시간대
    @Setter
    private Double congestionLevel; //혼잡도 값

    private LocalDateTime createdAt = LocalDateTime.now();
}
