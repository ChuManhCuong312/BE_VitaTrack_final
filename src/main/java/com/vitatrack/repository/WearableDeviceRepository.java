package com.vitatrack.repository;
import com.vitatrack.entity.WearableDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WearableDeviceRepository extends JpaRepository<WearableDevice, Long> {
    List<WearableDevice> findByUserId(Long userId);
    List<WearableDevice> findByUserIdAndIsActiveTrue(Long userId);
}
