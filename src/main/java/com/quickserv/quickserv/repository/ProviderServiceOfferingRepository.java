package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.ProviderServiceOffering;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.entity.SubService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderServiceOfferingRepository extends JpaRepository<ProviderServiceOffering, Long> {

    List<ProviderServiceOffering> findByProvider(User provider);

    List<ProviderServiceOffering> findByProviderAndSubService_Category_Id(User provider, Long categoryId);

    Optional<ProviderServiceOffering> findByProviderAndSubService(User provider, SubService subService);

    boolean existsByProviderAndSubService(User provider, SubService subService);

    List<ProviderServiceOffering> findBySubService(SubService subService);

    List<ProviderServiceOffering> findByIsAvailableTrue();
}

