package com.groom.marky.domain.response;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RouteDescriptionBuilder {

    public static String build(TmapRouteResponse response) {
        StringBuilder sb = new StringBuilder();
        List<TmapRouteResponse.LegDto> legs = response.getLegs();

        String startStation = legs.get(0).getStart().getName();
        String endStation = legs.get(legs.size() - 1).getEnd().getName();

        sb.append(startStation).append("에서 ").append(endStation).append("까지 가는 경로는 다음과 같습니다:\n\n");

        // 지하철과 버스 경로를 분리하여 저장
        List<TmapRouteResponse.LegDto> subwayLegs = new ArrayList<>();
        List<TmapRouteResponse.LegDto> busLegs = new ArrayList<>();
        List<TmapRouteResponse.LegDto> walkLegs = new ArrayList<>();

        for (TmapRouteResponse.LegDto leg : legs) {
            String mode = leg.getMode();
            if ("SUBWAY".equalsIgnoreCase(mode)) {
                subwayLegs.add(leg);
            } else if ("BUS".equalsIgnoreCase(mode)) {
                busLegs.add(leg);
            } else if ("WALK".equalsIgnoreCase(mode)) {
                walkLegs.add(leg);
            }
        }

        int stepCount = 1;

        // 지하철 경로가 있는 경우
        if (!subwayLegs.isEmpty()) {
            sb.append("1. 지하철 경로\n");
            for (TmapRouteResponse.LegDto leg : subwayLegs) {
                handleSubwayLeg(sb, leg, stepCount++);
            }
            sb.append("\n");
        }

        // 버스 경로가 있는 경우
        if (!busLegs.isEmpty()) {
            sb.append("2. 버스 경로\n");
            for (TmapRouteResponse.LegDto leg : busLegs) {
                handleBusLeg(sb, leg, stepCount++);
            }
            sb.append("\n");
        }

        // 도보 경로 처리
        for (TmapRouteResponse.LegDto leg : walkLegs) {
            handleWalkLeg(sb, leg, stepCount++);
        }

        sb.append("환승 횟수: ").append(response.getTransferCount()).append("회\n");
        sb.append("총 소요 시간: 약 ").append(formatTime(response.getTotalTime() / 60)).append("\n");
        sb.append("카드 요금: ").append(response.getFare().getRegular().getTotalFare()).append("원\n\n");
        sb.append("안전하고 편안한 여행 되세요!");

        return sb.toString();
    }

    private static void handleSubwayLeg(StringBuilder sb, TmapRouteResponse.LegDto leg, int index) {
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
        
        sb.append("   ").append(index).append(". ").append(leg.getStart().getName()).append("에서 ")
                .append(lineName).append("을 타세요. (소요시간: 약 ")
                .append(formatTime(leg.getSectionTime() / 60)).append(", 정차역 ")
                .append(stationCount).append("개)\n");
    }

    private static void handleBusLeg(StringBuilder sb, TmapRouteResponse.LegDto leg, int index) {
        String busRoute = "버스";
        if (leg.getLane() != null && !leg.getLane().isEmpty()) {
            TmapRouteResponse.LaneDto lane = leg.getLane().get(0);
            if (lane.getRoute() != null) {
                busRoute = lane.getRoute();
            }
        }

        List<TmapRouteResponse.StationDto> stations = leg.getPassStopList().getStationList();
        int stationCount = stations.size() - 1; // 출발역을 제외한 정차역 개수

        sb.append("   ").append(index).append(". ").append(leg.getStart().getName()).append("에서 ")
                .append(busRoute).append("을 타세요. (소요시간: 약 ")
                .append(formatTime(leg.getSectionTime() / 60)).append(", 정차역 ")
                .append(stationCount).append("개)\n");
    }

    private static void handleWalkLeg(StringBuilder sb, TmapRouteResponse.LegDto leg, int index) {
        if (leg.getSteps() != null && !leg.getSteps().isEmpty()) {
            sb.append(index).append(". ").append(leg.getSteps().get(0).getDescription())
                    .append(" (소요시간: 약 ").append(formatTime(leg.getSectionTime() / 60)).append(")\n");
        }
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