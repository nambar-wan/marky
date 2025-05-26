package com.groom.marky.controller;

import com.groom.marky.common.constant.CsvType;
import com.groom.marky.service.SeoulInfoApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seoul")
public class SeoulInfoController {

    private final SeoulInfoApiService seoulInfoApiService;
    private static final String FESTIVAL = "culturalEventInfo";
    private static final String GARDEN = "SearchParkInfoService";

    @Autowired
    public SeoulInfoController(SeoulInfoApiService seoulInfoApiService) {
        this.seoulInfoApiService = seoulInfoApiService;
    }

    @GetMapping("/festival")
    public ResponseEntity<?> searchFestival() {
        log.info("in festival controller");
        Map<String, String> festivalInfo = seoulInfoApiService.apiCall(FESTIVAL);
        log.info("festivalInfo {}", festivalInfo.size());

        return new ResponseEntity<>(festivalInfo, HttpStatus.OK);
    }

    @GetMapping("/garden")
    public ResponseEntity<?> searchGarden() {
        log.info("in garden controller");
        Map<String, String> gardenInfo = seoulInfoApiService.apiCall(GARDEN);
        log.info("gardenInfo {}", gardenInfo.size());

        return new ResponseEntity<>(gardenInfo, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(@RequestParam("type") CsvType type,
                                       @RequestParam("file") MultipartFile file) {
        log.info("CSV Upload: type={}, file name={}", type, file.getOriginalFilename());
        Map<String, String> result = seoulInfoApiService.csvImport(file, type);
        return ResponseEntity.ok(result);
    }
}
