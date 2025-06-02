package com.groom.marky.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ActivityMetadataRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<String> findByActivityTypeAndPlaceIds(
            String activityType, List<String> placeIds) {

        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT id, content, metadata 
            FROM vector_store 
            WHERE metadata->>'activity_type' = ? 
            AND metadata->>'googlePlaceId' = ANY(?)
            """;

        String[] placeIdArray = placeIds.toArray(new String[0]);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, activityType, placeIdArray);

        return results.stream()
                .map(row -> {
                    try {
                        Object metadataObj = row.get("metadata");
                        if (metadataObj instanceof PGobject pgObject) {
                            Map<String, Object> metadata = objectMapper.readValue(pgObject.getValue(), Map.class);
                            return (String) metadata.get("googlePlaceId");
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}