package com.quickserv.quickserv.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    public List<String> getAllLocations() {
        return List.of(
                "Mumbai",
                "Pune",
                "Bengaluru",
                "Chennai",
                "Hyderabad",
                "Delhi",
                "Kolkata",
                "Ahmedabad"
        );
    }
}

