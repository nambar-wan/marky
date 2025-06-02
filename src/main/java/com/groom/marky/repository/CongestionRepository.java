package com.groom.marky.repository;

import com.groom.marky.domain.request.SubwayCongestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface CongestionRepository extends JpaRepository<SubwayCongestion, Long> {
    Optional<SubwayCongestion> findByStationNameAndDirectionAndDayTypeAndTimeSlot(
            String stationName,
            String direction,
            String dayType,
            LocalTime timeSlot
    );
}
