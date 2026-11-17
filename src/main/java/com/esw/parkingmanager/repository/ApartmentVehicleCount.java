package com.esw.parkingmanager.repository;

import java.util.UUID;

// One row of a "count cars per apartment" query: which apartment, how many cars
public record ApartmentVehicleCount(UUID apartmentId, Long count) {
}