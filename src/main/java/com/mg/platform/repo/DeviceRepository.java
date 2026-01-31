package com.mg.platform.repo;

import com.mg.platform.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    @Query("SELECT d FROM Device d JOIN FETCH d.merchant WHERE d.merchant.id = :merchantId AND d.deviceCode = :deviceCode")
    Optional<Device> findByMerchantIdAndDeviceCode(@Param("merchantId") Long merchantId, @Param("deviceCode") String deviceCode);
    
    @Query("SELECT d FROM Device d JOIN FETCH d.merchant WHERE d.merchant.id = :merchantId")
    List<Device> findByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT d FROM Device d JOIN FETCH d.merchant WHERE d.deviceCode = :deviceCode")
    Optional<Device> findByDeviceCode(@Param("deviceCode") String deviceCode);
}
