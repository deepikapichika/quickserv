package com.quickserv.quickserv.dto.search;

import java.math.BigDecimal;

public class ServiceSearchResultDto {

    private final Long providerId;
    private final String providerName;
    private final Double providerRating;
    private final Long serviceId;
    private final String serviceName;
    private final BigDecimal servicePrice;
    private final Long categoryId;
    private final String categoryName;
    private final Long subcategoryId;
    private final String subcategoryName;
    private final String location;
    private final String bookUrl;

    public ServiceSearchResultDto(Long providerId,
                                  String providerName,
                                  Double providerRating,
                                  Long serviceId,
                                  String serviceName,
                                  BigDecimal servicePrice,
                                  Long categoryId,
                                  String categoryName,
                                  Long subcategoryId,
                                  String subcategoryName,
                                  String location,
                                  String bookUrl) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerRating = providerRating;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.servicePrice = servicePrice;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.subcategoryId = subcategoryId;
        this.subcategoryName = subcategoryName;
        this.location = location;
        this.bookUrl = bookUrl;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public Double getProviderRating() {
        return providerRating;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public BigDecimal getServicePrice() {
        return servicePrice;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getSubcategoryId() {
        return subcategoryId;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public String getLocation() {
        return location;
    }

    public String getBookUrl() {
        return bookUrl;
    }
}
