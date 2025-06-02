package com.groom.marky.domain.response;


import java.time.format.DateTimeFormatter;
import java.util.List;

public class SubwayRouteDescriptionBuilder {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String build(SubwayRouteResponse.SubwayRouteDto route) {
        StringBuilder sb = new StringBuilder();

        sb.append("[서울 지하철 경로 안내]\n\n");

        sb.append("출발역: ").append(route.getGlobalStartName()).append("\n");


        // 출발역 혼잡도 정보가 있을 경우 추가
        if (route.getStartCongestion() != null &&
                !route.getStartCongestion().isEmpty() &&
                route.getDayType() != null &&
                route.getRoundedTime() != null) {

            sb.append("출발역 혼잡도: ")
                    .append(route.getStartCongestion())
                    .append(" (")
                    .append(route.getDayType()).append(", ")
                    .append(route.getRoundedTime().format(TIME_FORMATTER)).append(" 기준)\n");
        }

        sb.append("도착역: ").append(route.getGlobalEndName()).append("\n");
        sb.append("총 소요 시간: 약 ").append(route.getGlobalTravelTime()).append("분\n");
        sb.append("총 이동 거리: ").append(route.getGlobalDistance()).append("km\n");
        sb.append("지나는 역 수: ").append(route.getGlobalStationCount()).append("개\n");
        sb.append("카드 요금: ").append(route.getFare()).append("원\n");
        sb.append("현금 요금: ").append(route.getCashFare()).append("원\n\n");


        sb.append("[운행 구간 정보]\n");
        for (SubwayRouteResponse.SubwayRouteDto.DriveInfo drive : route.getDriveInfoSet().getDriveInfo()) {
            sb.append("- ")
                    .append(drive.getStartName()).append("역에서 ")
                    .append(drive.getLaneName()).append(" 노선을 타고, ")
                    .append(drive.getStationCount()).append("개 역을 이동합니다.\n");
        }
        // 환승 정보
        List<SubwayRouteResponse.SubwayRouteDto.ExChangeInfo> exChangeInfoList =
                route.getExChangeInfoSet() != null ? route.getExChangeInfoSet().getExChangeInfo() : null;
        if (exChangeInfoList != null && !exChangeInfoList.isEmpty()) {
            sb.append("\n[환승 정보]\n");
            for (SubwayRouteResponse.SubwayRouteDto.ExChangeInfo ex : exChangeInfoList) {
                sb.append("- ")
                        .append(ex.getStartName()).append("역에서 ")
                        .append(ex.getLaneName()).append(" 노선으로 갈아타서, ")
                        .append(ex.getExName()).append("역까지 이동합니다.\n");
                // 혼잡도 정보가 존재할 경우 출력
                    if (ex.getStartCongestion() != null &&
                            !ex.getStartCongestion().isEmpty() &&
                            ex.getDayType() != null &&
                            ex.getRoundedTime() != null) {
                    sb.append("  ↳ 환승역 혼잡도: ")
                            .append(ex.getExName())
                            .append(" (")
                            .append(ex.getDayType()).append(", ")
                            .append(ex.getRoundedTime().format(TIME_FORMATTER)).append(" 기준)\n");
                }
            }
        } else {
            sb.append("\n[환승 정보] 없음\n");
        }

        return sb.toString();
    }
}