package com.gpsvariant.repository;

import com.gpsvariant.entity.GpsImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GpsImageRepository extends JpaRepository<GpsImage, Long> {
    Optional<GpsImage> findByIdAndUserId(Long id, Long userId);
}
