package com.mg.platform.repo;

import com.mg.platform.domain.DeviceActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceActivityAssignmentRepository extends JpaRepository<DeviceActivityAssignment, Long> {
    List<DeviceActivityAssignment> findByDeviceId(Long deviceId);
    List<DeviceActivityAssignment> findByDeviceIdAndStatus(Long deviceId, String status);
    List<DeviceActivityAssignment> findByActivityId(Long activityId);
    List<DeviceActivityAssignment> findByActivityIdAndStatus(Long activityId, String status);
    java.util.Optional<DeviceActivityAssignment> findByActivityIdAndDeviceId(Long activityId, Long deviceId);
}
