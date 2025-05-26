package com.groom.marky.common.constant;

public enum CsvType {
    TRAIL("산책길"),
    GALLERY("미술관"),
    THEATER("영화관"),
    THEMEPARK("놀이공원");

    private final String description;

    CsvType(String description) {
        this.description = description;
    }
}
