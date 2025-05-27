package com.groom.marky.service;

import com.groom.marky.common.constant.CsvType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SeoulInfoApiService {
    Map<String, String> apiCall(String serviceType);

    Map<String, String> csvImport(MultipartFile multipartFile, CsvType type);
}
