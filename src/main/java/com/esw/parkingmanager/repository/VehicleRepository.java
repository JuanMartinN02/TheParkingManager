package com.esw.parkingmanager.repository;

import com.esw.parkingmanager.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    // Number of vehicles in a given apartment
    long countByApartmentId(UUID apartmentId);

    // Does the vehicle exist in this property?
    boolean existsByPropertyIdAndPlate(UUID propertyId, String plate);

    // List of vehicles of an apartment
    List<Vehicle> findByApartmentId(UUID apartmentId);

    // Does this vehicle belong to this apartment?
    Optional<Vehicle> findByIdAndApartmentId(UUID id, UUID apartmentId);

    // return the vehicle entity by property ID and Vehicle Plate
    Optional<Vehicle> findByPropertyIdAndPlate(UUID propertyId, String plate);

    // For move-out / move-in
    long deleteByApartmentId(UUID apartmentId);

        // Counted by the database in one query, instead of loading every car
    @Query("""
            SELECT new com.esw.parkingmanager.repository.ApartmentVehicleCount(v.apartment.id, COUNT(v))
            FROM Vehicle v
            WHERE v.property.id = :propertyId
            GROUP BY v.apartment.id
            """)
    List<ApartmentVehicleCount> countByApartmentForProperty(@Param("propertyId") UUID propertyId);
}
