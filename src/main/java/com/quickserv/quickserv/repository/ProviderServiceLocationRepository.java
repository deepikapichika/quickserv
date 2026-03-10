package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.ProviderServiceLocation;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderServiceLocationRepository extends JpaRepository<ProviderServiceLocation, Long> {

    List<ProviderServiceLocation> findByProvider(User provider);

    List<ProviderServiceLocation> findByProviderAndIsActiveTrue(User provider);

    Optional<ProviderServiceLocation> findByProviderAndIsPrimaryTrue(User provider);

    Optional<ProviderServiceLocation> findByProviderAndLocationNameIgnoreCase(User provider, String locationName);

    boolean existsByProviderAndLocationNameIgnoreCase(User provider, String locationName);

    boolean existsByProviderAndLocationNameIgnoreCaseAndIsActiveTrue(User provider, String locationName);
}

