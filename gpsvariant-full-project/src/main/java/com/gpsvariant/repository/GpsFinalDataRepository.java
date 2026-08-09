package com.gpsvariant.repository;

import com.gpsvariant.entity.GpsFinalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GpsFinalDataRepository extends JpaRepository<GpsFinalData, Long> {
}
