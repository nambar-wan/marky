package com.groom.marky.domain.response;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SubwayRouteDescriptionBuilder {

    public static String build(TmapRouteResponse response) {
        StringBuilder sb = new StringBuilder();

        List<TmapRouteResponse.LegDto> legs = response.getLegs();

        String startStation = legs.get(0).getStart().getName();
        String endStation = legs.get(legs.size() - 1).getEnd().getName();

        sb.append(startStation).append("에서 ").append(endStation).append("까지 가는 지하철 경로는 다음과 같습니다:\n\n");

        for (int i = 0; i < legs.size(); i++) {
            TmapRouteResponse.LegDto leg = legs.get(i);
            if ("SUBWAY".equalsIgnoreCase(leg.getMode())) {
                String lineName = "지하철";
                if (leg.getLane() != null && !leg.getLane().isEmpty()) {
                    TmapRouteResponse.LaneDto lane = leg.getLane().get(0);
                    if (lane.getRoute() != null) {
                        lineName = lane.getRoute();
                    }
                } else if (leg.getRoute() != null) {
                    lineName = leg.getRoute();
                }

                List<TmapRouteResponse.StationDto> stations = leg.getPassStopList().getStationList();
                int stationCount = stations.size() - 1; // 출발역을 제외한 정차역 개수
                
                if (i == 0) {
                    sb.append(i + 1).append(". ").append(leg.getStart().getName()).append("에서 ")
                            .append(lineName).append("을 타세요. (소요시간: 약 ")
                            .append(formatTime(leg.getSectionTime() / 60)).append(", 정차역 ")
                            .append(stationCount).append("개)\n");
                } else {
                    sb.append(i + 1).append(". ").append(leg.getStart().getName()).append("에서 \"")
                            .append(lineName).append("\" 환승하여 지하철을 타세요. (소요시간: 약 ")
                            .append(formatTime(leg.getSectionTime() / 60)).append(", 정차역 ")
                            .append(stationCount).append("개)\n");
                }
                sb.append("\n");
            }
        }

        sb.append("환승 횟수: ").append(response.getTransferCount()).append("회\n");
        sb.append("총 소요 시간: 약 ").append(formatTime(response.getTotalTime() / 60)).append("\n");
        sb.append("카드 요금: ").append(response.getFare().getRegular().getTotalFare()).append("원\n\n");
        sb.append("안전하고 편안한 여행 되세요!");

        return sb.toString();
    }

    private static String formatTime(int minutes) {
        if (minutes < 60) {
            return minutes + "분";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + remainingMinutes + "분";
    }
}