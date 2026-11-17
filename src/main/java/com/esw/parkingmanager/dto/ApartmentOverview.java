package com.esw.parkingmanager.dto;

import com.esw.parkingmanager.model.ApartmentStatus;

import java.util.List;
import java.util.UUID;

// One row of the admin's apartment list: the apartment plus its car count and active parking
public record ApartmentOverview(UUID id,
                                String unitNumber,
                                ApartmentStatus status,
                                Integer maxVehicles,
                                Integer maxVisitorPasses,
                                long vehicleCount,
                                List<AuthorizationSummary> activeAuthorizations) {
}