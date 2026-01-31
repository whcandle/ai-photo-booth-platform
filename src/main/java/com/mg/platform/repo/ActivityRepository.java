package com.mg.platform.repo;

import com.mg.platform.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    @Query("SELECT a FROM Activity a JOIN FETCH a.merchant WHERE a.merchant.id = :merchantId")
    List<Activity> findByMerchantId(@Param("merchantId") Long merchantId);
    
    @Query("SELECT a FROM Activity a JOIN FETCH a.merchant WHERE a.merchant.id = :merchantId AND a.status = :status")
    List<Activity> findByMerchantIdAndStatus(@Param("merchantId") Long merchantId, @Param("status") String status);
}
