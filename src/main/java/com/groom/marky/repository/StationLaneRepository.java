package com.groom.marky.repository;


import com.groom.marky.domain.request.StationLane;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StationLaneRepository  extends JpaRepository<StationLane, Long> {

    Optional<StationLane> findByStationNm(String stationNm);

    List<StationLane> findAllByStationNm(String stationNm);

    Optional<StationLane> findFirstByStationNmContaining(String stationNm);
}
