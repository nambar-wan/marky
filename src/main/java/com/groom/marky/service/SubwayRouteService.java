package com.groom.marky.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.domain.request.StationLane;
import com.groom.marky.domain.request.SubwayCongestion;
import com.groom.marky.domain.response.SubwayRouteResponse;
import com.groom.marky.repository.CongestionRepository;
import com.groom.marky.repository.StationLaneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
public class SubwayRouteService {

    @Autowired
    private OdysseyService odysseyService;
    @Autowired
    private StationLaneRepository stationLaneRepository;
    @Autowired
    private CongestionRepository congestionRepository;

    public SubwayRouteResponse.SubwayRouteDto findShortestRoute(String origin, String destination) {

        //입력값 정제
        String[] StationsList = extractStationNames(origin,destination);
        String startStation = StationsList[0];
        String endStation = StationsList[1];


        List<StationLane> startLanes = stationLaneRepository.findAllByStationNm(startStation);
        List<StationLane> endLanes = stationLaneRepository.findAllByStationNm(endStation);

        Set<String> startLineNums = startLanes.stream().map(StationLane::getLineNum).collect(Collectors.toSet());
        Set<String> endLineNums = endLanes.stream().map(StationLane::getLineNum).collect(Collectors.toSet());

        startLineNums.retainAll(endLineNums); //교집합
        Optional<String> commonLine = startLineNums.stream().findFirst();
        String originLineNum = commonLine.orElse(null);

        StationLane startLane = startLanes.stream()
                .filter(lane -> lane.getLineNum().equals(originLineNum))
                .findFirst()
                .orElse(null);

        StationLane endLane = endLanes.stream()
                .filter(lane -> lane.getLineNum().equals(originLineNum))
                .findFirst()
                .orElse(null);

        String startId = (startLane != null) ? startLane.getFrCode() : null;
        String endId = (endLane != null) ? endLane.getFrCode() : null;


    String startLineNum = originLineNum;

    if (startId == null || endId == null) {
        startId = stationLaneRepository.findFirstByStationNmContaining(startStation)
                .map(station -> station.getFrCode())
                .orElse(null);
         endId = stationLaneRepository.findFirstByStationNmContaining(endStation)
                .map(station -> station.getFrCode())
                .orElse(null);

         startLineNum = stationLaneRepository
                .findFirstByStationNmContaining(startStation)
                .map(StationLane::getLineNum)
                .orElse(null);

    }
        if (startId == null || endId == null) {
            throw new IllegalArgumentException("입력한 역 이름이 존재하지 않습니다.");
        }
        try {
            // API 호출 및 응답 파싱
            String jsonResponse = odysseyService.callSubwayPathApi(startId, endId);
            ObjectMapper mapper = new ObjectMapper();
            SubwayRouteResponse response = mapper.readValue(jsonResponse, SubwayRouteResponse.class);
            SubwayRouteResponse.SubwayRouteDto route = response.getResult();

            if (route == null) throw new RuntimeException("API 응답에 result 필드가 없습니다.");

            //1. 상하선 방향 판별 로직
            String directionType = Direction(startLineNum,startId, endId);
            log.info("[SubwayRouteService] 상하선 방향: {}", directionType);
            route.setDirectionType(directionType);

            // 2. 요일/시간대 결정
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            String dayType = convertDayOfweekToDayType(currentDay);
            LocalTime roundedTime = getRoundedTimeSlot(LocalTime.now());
            // 요일, 시간 값 입력
            route.setDayType(dayType);
            route.setRoundedTime(roundedTime);

            // 3. 출발역 혼잡도 조회
            Optional<SubwayCongestion> congestionInfo = congestionRepository
                    .findByStationNameAndDirectionAndDayTypeAndTimeSlot(startStation, directionType, dayType, roundedTime);
            congestionInfo.ifPresent(congestion -> {
                String congestionLabel = convertCongestionToLabel(congestion.getCongestionLevel());
                route.setStartCongestion(congestionLabel);
                log.info("[출발역 혼잡도] {}역 혼잡도: {}", startStation, congestionLabel);
            });



            // 환승역 혼잡도 조회
            List<SubwayRouteResponse.SubwayRouteDto.ExChangeInfo> exChangeInfos = route.getExChangeInfoSet().getExChangeInfo();
            if (exChangeInfos != null && !exChangeInfos.isEmpty()) {
                setCongestionInfoForExchanges(exChangeInfos, directionType, dayType, roundedTime);
            }


            return route;


        } catch (IOException e) {
            log.info("[SubwayRouteService] JSON 파싱 중 오류 발생");
            throw new RuntimeException("JSON 파싱 중 오류 발생", e);
        } catch (Exception e) {
            log.info("[SubwayRouteService] 경로 조회 중 알 수 없는 오류 발생");
            throw new RuntimeException("경로 조회 중 알 수 없는 오류 발생", e);
        }
    }

    private void setCongestionInfoForExchanges(List<SubwayRouteResponse.SubwayRouteDto.ExChangeInfo> exChangeInfos
            ,String directionType, String dayType, LocalTime roundedTime) {
        for (SubwayRouteResponse.SubwayRouteDto.ExChangeInfo exChangeInfo : exChangeInfos) {
            Optional<SubwayCongestion> congestion = congestionRepository
                    .findByStationNameAndDirectionAndDayTypeAndTimeSlot(
                            exChangeInfo.getExName(),
                            directionType,
                            dayType,
                            roundedTime
                    );


            congestion.ifPresent(c -> {
                String congestionLabel = convertCongestionToLabel(c.getCongestionLevel());
                exChangeInfo.setStartCongestion(congestionLabel);
                exChangeInfo.setDirection(directionType);
                exChangeInfo.setDayType(dayType);
                exChangeInfo.setRoundedTime(roundedTime);
                log.info("[환승역 혼잡도] {}역 혼잡도: {}", exChangeInfo.getExName(), exChangeInfo.getStartCongestion());
            });
        }
    }



    private String[] extractStationNames(String origin, String destination) {
        String cleanOrigin = origin.replaceAll("(역)$","").trim();
        String cleanDestination = destination.replaceAll("(역)$","").trim();
        return new String[]{cleanOrigin,cleanDestination};
    }

    private String Direction(String lineNumber, String startFrCode, String endFrCode) {
        int start = Integer.parseInt(startFrCode);
        int end = Integer.parseInt(endFrCode);

        if (lineNumber.equals("02호선")) {
            return (start < end) ? "내선" : "외선";
        } else {
            return (start < end) ? "상선" : "하선";
        }
    }

    private LocalTime getRoundedTimeSlot(LocalTime currentTime) {
        int minute = currentTime.getMinute();
        int roundedMinute = (minute < 30) ? 0 : 30;
        return currentTime.withMinute(roundedMinute).truncatedTo(ChronoUnit.MINUTES);
    }

    private String convertCongestionToLabel(double congestionLevel) {
        if (congestionLevel <= 34.0) {
            return "여유";
        } else if (congestionLevel <= 70.0) {
            return "보통";
        } else if (congestionLevel <= 100.0) {
            return "혼잡";
        } else if (congestionLevel <= 130.0) {
            return "매우 혼잡";
        } else {
            return "최악";
        }
    }

    private String convertDayOfweekToDayType(DayOfWeek dayOfweek) {
        switch (dayOfweek) {
            case SATURDAY -> {
                return "토요일";
            }
            case SUNDAY -> {
                return "일요일";
            }
            default -> {
                return "평일";
            }
        }
    }

}
