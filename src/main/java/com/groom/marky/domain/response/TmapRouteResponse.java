package com.groom.marky.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmapRouteResponse {


    @JsonProperty("fare")
    private FareDto fare;
    private int totalTime;
    private List<LegDto> legs;
    private int totalWalkTime;
    private int totalWalkDistance;
    private int transferCount;
    private int totalDistance;
    private int pathType;

    @Data
    public static class FareDto {
        private RegularFareDto regular;
    }

    @Data
    public static class RegularFareDto {
        private int totalFare;
        private CurrencyDto currency;
    }

    @Data
    public static class CurrencyDto {
        private String symbol;
        private String currency;
        private String currencyCode;
    }

    @Data
    public static class LegDto {
        private String mode;
        private int sectionTime;
        private double distance;
        private StartEndDto start;
        private StartEndDto end;

        private List<StepDto> steps;
        @JsonProperty("Lane")                       // mode == "WALK"
        private List<LaneDto> lane;                // mode == "SUBWAY"
        private PassStopListDto passStopList;      // mode == "SUBWAY"
        private PassShapeDto passShape;            // mode == "SUBWAY"

        private String routeColor;
        private String route;
        private String routeId;
        private int type;
        private int service;
    }

    @Data
    public static class StartEndDto {
        private String name;
        private double lon;
        private double lat;
    }

    @Data
    public static class StepDto {
        private String streetName;
        private double distance;
        private String description;
        private String linestring;
    }

    @Data
    public static class LaneDto {
        private String routeColor;
        private String route;
        private int routeId;
        private int type;
        private String service;
    }

    @Data
    public static class PassStopListDto {
        private List<StationDto> stationList;
    }

    @Data
    public static class StationDto {
        private int index;
        private String stationName;
        private String lon;
        private String lat;
        private String stationID;
    }

    @Data
    public static class PassShapeDto {
        private String linestring;
    }
}
