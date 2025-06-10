package com.groom.marky.service.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;

import java.lang.reflect.Type;

@Slf4j
public class CustomToolCallResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        log.info("Convert result to Json type");
        double start = System.currentTimeMillis();
        String resultToString =
//                result.toString();
        JsonParser.toJson(result);

        double end = System.currentTimeMillis();
        log.info("Convert result to Json took " + (end - start) + " ms");

        return resultToString;
    }
}
