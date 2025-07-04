package com.groom.marky.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class CafeRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	public List<String> findByIdWhenReviewIsExist(List<String> placeIds, double minRating) {


		if (placeIds == null || placeIds.isEmpty()) {
			return List.of();
		}

		String sql = """
            SELECT metadata->>'googlePlaceId' AS google_place_id 
            FROM vector_store 
            WHERE metadata->>'googlePlaceId' = ANY(?)
            AND content LIKE '%에 위치한 카페%'
            AND content NOT LIKE '%아직까지 등록된 리뷰가 없습니다.%'
            AND (metadata ->> 'rating')::numeric >= ?            
            """;

		List<String> result =  jdbcTemplate.query(
				sql,
				new Object[]{placeIds.toArray(new String[0]), minRating},
				(rs, rowNum) -> rs.getString("google_place_id")
		);

//        System.out.println("result 값 비교 시작");
//        for(int i=0; i<result.size(); i++) {
//            boolean found = false;
//            for(int j=0; j<placeIds.size(); j++) {
//                if(result.get(i).equals(placeIds.get(j))) {
//                    found = true; break;
//                }
//            }
//            if(!found) System.out.println(result.get(i) + "을 찾지 못함");
//        }
//        System.out.println("result 값 비교 끝");

		return result;
	}
}
