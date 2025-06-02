package com.groom.marky.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class SubwayRouteResponse {

    @JsonProperty("result")
    private SubwayRouteDto result;

    @Data
    public static class SubwayRouteDto {
        private String globalStartName; //출발역 명
        private String globalEndName;   //도착역 명
        private int globalTravelTime;   //전체 운행소요시간(분)
        private double globalDistance;  //전체 운행거리(km)
        private int globalStationCount; //전체 정차역 수
        private int fare;               //카드 요금 (성인기준)
        private int cashFare;           //현금 요금 (성인기준)
        private String directionType;
        private String startCongestion;  // 혼잡도 라벨
        private String dayType;          // "평일", "주말", "토요일" 같은 요일 정보
        private LocalTime roundedTime;   // "08:00", "18:30" 같은 시간 정보 (LocalTime을 문자열로)

        @JsonProperty("driveInfoSet")
        private DriveInfoWrapper driveInfoSet;
        @JsonProperty("exChangeInfoSet")
        private ExChangeInfoWrapper exChangeInfoSet;
        @JsonProperty("stationSet")
        private StationsWrapper stationSet;

        @Data
        public static class DriveInfoWrapper {
            @JsonProperty("driveInfo")
            private List<DriveInfo> driveInfo;
        }

        @Data
        public static class DriveInfo {
            private String laneID;
            private String laneName;
            private String startName;
            private int stationCount;
            private int wayCode;
            private String wayName;
        }

        @Data
        public static class ExChangeInfoWrapper {
            @JsonProperty("exChangeInfo")
            private List<ExChangeInfo> exChangeInfo;
        }

        @Data
        public static class ExChangeInfo {
            private String laneName;
            private String startName;
            private String exName;
            private int exSID;
            private int fastTrain;
            private int fastDoor;
            private int exWalkTime;

            //추가: 환승 구간 혼잡도 정보
            private String startCongestion;  // 혼잡도 라벨
            private String direction;        // "상선","하선","내선","외선"
            private String dayType;          // "평일", "주말", "토요일" 같은 요일 정보
            private LocalTime roundedTime;   // "08:00", "18:30" 같은 시간 정보 (LocalTime을 문자열로)
        }

        @Data
        public static class StationsWrapper {
            @JsonProperty("stations")
            private List<Station> stations;
        }

        @Data
        public static class Station {
            private int startID;
            private String startName;
            private int endSID;
            private String endName;
            private int travelTime;
        }
    }
}
