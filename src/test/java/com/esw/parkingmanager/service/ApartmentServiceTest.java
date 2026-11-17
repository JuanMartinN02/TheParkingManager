package com.esw.parkingmanager.service;

import com.esw.parkingmanager.dto.ApartmentOverview;
import com.esw.parkingmanager.dto.MoveOutResponse;
import com.esw.parkingmanager.model.*;
import com.esw.parkingmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {

    private static final UUID PROPERTY_A = UUID.randomUUID();
    private static final UUID APARTMENT_301 = UUID.randomUUID();
    private static final UUID APARTMENT_302 = UUID.randomUUID();
    // Nov 14 in Houston
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T18:00:00Z"), ZoneId.of("America/Chicago"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Mock private ApartmentRepository apartmentRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private ParkingAuthorizationRepository authorizationRepository;
    @Mock private AppUserRepository userRepository;
    

    private ApartmentService service;

    @BeforeEach
    void setUp() {
            service = new ApartmentService(apartmentRepository, propertyRepository, vehicleRepository,
                authorizationRepository, userRepository, CLOCK);
    }

    @Test
    void moveOutClearsTheApartment() {
        Apartment apartment = apartmentWithStatus(ApartmentStatus.ACTIVE);
        AppUser resident1 = resident();
        AppUser resident2 = resident();
        when(authorizationRepository.deleteByApartmentId(APARTMENT_301)).thenReturn(2L);
        when(vehicleRepository.deleteByApartmentId(APARTMENT_301)).thenReturn(3L);
        when(userRepository.findByApartmentIdAndActiveTrue(APARTMENT_301)).thenReturn(List.of(resident1, resident2));

        MoveOutResponse response = service.moveOut(PROPERTY_A, APARTMENT_301);

        assertThat(apartment.getStatus()).isEqualTo(ApartmentStatus.MOVED_OUT);
        assertThat(resident1.isActive()).isFalse();
        assertThat(resident2.isActive()).isFalse();
        assertThat(response.authorizationsRemoved()).isEqualTo(2);
        assertThat(response.vehiclesRemoved()).isEqualTo(3);
        assertThat(response.accountsDeactivated()).isEqualTo(2);
    }

    @Test
    void secondMoveOutIsRejectedAndDeletesNothing() {
        apartmentWithStatus(ApartmentStatus.MOVED_OUT);

        assertThatThrownBy(() -> service.moveOut(PROPERTY_A, APARTMENT_301))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        // Nothing may be touched when the move-out is refused
        verifyNoInteractions(vehicleRepository, authorizationRepository, userRepository);
    }

    @Test
    void moveInReactivatesTheApartment() {
        apartmentWithStatus(ApartmentStatus.MOVED_OUT);
        when(apartmentRepository.save(any(Apartment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.moveIn(PROPERTY_A, APARTMENT_301).getStatus()).isEqualTo(ApartmentStatus.ACTIVE);
    }

    @Test
    void moveInOnActiveApartmentIsRejected() {
        apartmentWithStatus(ApartmentStatus.ACTIVE);

        assertThatThrownBy(() -> service.moveIn(PROPERTY_A, APARTMENT_301))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void duplicateUnitNumberIsRejectedOnCreate() {
        Property property = new Property();
        ReflectionTestUtils.setField(property, "id", PROPERTY_A);
        when(propertyRepository.findById(PROPERTY_A)).thenReturn(Optional.of(property));
        when(apartmentRepository.existsByPropertyIdAndUnitNumber(PROPERTY_A, "301")).thenReturn(true);

        Apartment duplicate = new Apartment();
        // Extra spaces on purpose: still the same unit
        duplicate.setUnitNumber(" 301 ");

        assertThatThrownBy(() -> service.create(PROPERTY_A, duplicate))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(apartmentRepository, never()).save(any());
    }

    @Test
    void updateCanKeepItsOwnUnitNumber() {
        apartmentWithStatus(ApartmentStatus.ACTIVE);
        when(apartmentRepository.existsByPropertyIdAndUnitNumberAndIdNot(PROPERTY_A, "301", APARTMENT_301)).thenReturn(false);
        when(vehicleRepository.countByApartmentId(APARTMENT_301)).thenReturn(0L);
        when(apartmentRepository.save(any(Apartment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Apartment changes = new Apartment();
        changes.setUnitNumber("301");
        changes.setMaxVehicles(2);
        changes.setMaxVisitorPasses(2);

        assertThat(service.update(PROPERTY_A, APARTMENT_301, changes).getUnitNumber()).isEqualTo("301");
    }

    @Test
    void renameToAnExistingUnitIsRejected() {
        apartmentWithStatus(ApartmentStatus.ACTIVE);
        when(apartmentRepository.existsByPropertyIdAndUnitNumberAndIdNot(PROPERTY_A, "302", APARTMENT_301)).thenReturn(true);

        Apartment changes = new Apartment();
        changes.setUnitNumber("302");
        changes.setMaxVehicles(2);
        changes.setMaxVisitorPasses(2);

        assertThatThrownBy(() -> service.update(PROPERTY_A, APARTMENT_301, changes))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(apartmentRepository, never()).save(any());
    }

    @Test
    void overviewCombinesCarCountsAndActiveParkingPerApartment() {
        Apartment withCars = apartment(APARTMENT_301, "301");
        Apartment empty = apartment(APARTMENT_302, "302");
        when(apartmentRepository.findByPropertyId(PROPERTY_A)).thenReturn(List.of(withCars, empty));
        when(vehicleRepository.countByApartmentForProperty(PROPERTY_A))
                .thenReturn(List.of(new ApartmentVehicleCount(APARTMENT_301, 2L)));
        ParkingAuthorization permit = new ParkingAuthorization();
        permit.setApartment(withCars);
        when(authorizationRepository.findActiveForProperty(PROPERTY_A, TODAY)).thenReturn(List.of(permit));

        List<ApartmentOverview> overview = service.overview(PROPERTY_A);

        assertThat(overview.get(0).vehicleCount()).isEqualTo(2);
        assertThat(overview.get(0).activeAuthorizations()).hasSize(1);
        assertThat(overview.get(0).activeAuthorizations().get(0).spotLabel()).isNull();
        // Missing from both queries, still listed with zeros instead of a crash
        assertThat(overview.get(1).vehicleCount()).isZero();
        assertThat(overview.get(1).activeAuthorizations()).isEmpty();
    }

    // ---------- Helpers ----------

    // Builds the apartment and makes the fake repository return it, like getOne finds it in the database
    private Apartment apartmentWithStatus(ApartmentStatus status) {
        Apartment apartment = new Apartment();
        ReflectionTestUtils.setField(apartment, "id", APARTMENT_301);
        apartment.setUnitNumber("301");
        apartment.setStatus(status);
        when(apartmentRepository.findByIdAndPropertyId(APARTMENT_301, PROPERTY_A)).thenReturn(Optional.of(apartment));
        return apartment;
    }

    private AppUser resident() {
        AppUser resident = new AppUser();
        resident.setRole(Role.RESIDENT);
        return resident;
    }

    private Apartment apartment(UUID id, String unitNumber) {
        Apartment apartment = new Apartment();
        ReflectionTestUtils.setField(apartment, "id", id);
        apartment.setUnitNumber(unitNumber);
        return apartment;
    }

}