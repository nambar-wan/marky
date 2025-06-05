package com.groom.marky.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CafeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<String> findByIdWhenReviewIsExist(List<String> placeIds) {

        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }

        String sql = """            
            SELECT id 
            FROM vector_store 
            WHERE metadata->>'googlePlaceId' = ANY(?)
            AND content LIKE '%에 위치한 카페%'
            AND content NOT LIKE '%아직까지 등록된 리뷰가 없습니다.%'
            """;

        return jdbcTemplate.query(
                sql,
                new Object[]{placeIds.toArray(new String[0])},
                (rs, rowNum) -> rs.getString("id")
        );
    }
}