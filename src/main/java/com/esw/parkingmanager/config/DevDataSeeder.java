package com.esw.parkingmanager.config;

import com.esw.parkingmanager.dto.ApartmentRequest;
import com.esw.parkingmanager.dto.CreateAuthorizationRequest;
import com.esw.parkingmanager.dto.CreateUserRequest;
import com.esw.parkingmanager.dto.PropertyRequest;
import com.esw.parkingmanager.dto.VehicleRequest;
import com.esw.parkingmanager.model.Apartment;
import com.esw.parkingmanager.model.AppUser;
import com.esw.parkingmanager.model.ParkingSpot;
import com.esw.parkingmanager.model.Property;
import com.esw.parkingmanager.model.Role;
import com.esw.parkingmanager.repository.AppUserRepository;
import com.esw.parkingmanager.repository.PropertyRepository;
import com.esw.parkingmanager.service.ApartmentService;
import com.esw.parkingmanager.service.ParkingAuthorizationService;
import com.esw.parkingmanager.service.ParkingSpotService;
import com.esw.parkingmanager.service.UserService;
import com.esw.parkingmanager.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Demo data for local development only. Never runs in production, which doesn't use the "dev" profile
@Component
@Profile("dev")
@Order(2)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    // Same password for every demo account. It only exists in the dev profile
    private static final String DEMO_PASSWORD = "demo-password-123";

    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;
    private final ApartmentService apartmentService;
    private final VehicleService vehicleService;
    private final ParkingSpotService parkingSpotService;
    private final ParkingAuthorizationService authorizationService;
    private final UserService userService;
    private final Clock clock;

    public DevDataSeeder(PropertyRepository propertyRepository,
                         AppUserRepository userRepository,
                         ApartmentService apartmentService,
                         VehicleService vehicleService,
                         ParkingSpotService parkingSpotService,
                         ParkingAuthorizationService authorizationService,
                         UserService userService,
                         Clock clock) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.apartmentService = apartmentService;
        this.vehicleService = vehicleService;
        this.parkingSpotService = parkingSpotService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.clock = clock;
    }

    // One transaction: if anything fails, nothing is left half created
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Only an empty database gets demo data, so restarts never duplicate it
        if (propertyRepository.count() > 0) {
            return;
        }

        AppUser superAdmin = userRepository.findFirstByRole(Role.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Dev data needs the first SUPER_ADMIN. Set FIRST_ADMIN_EMAIL and FIRST_ADMIN_PASSWORD"));
        LocalDate today = LocalDate.now(clock);

        // ---------- Westpark: one apartment per plate check verdict ----------
        Property westpark = propertyRepository.save(new PropertyRequest("Westpark Apartments", 20, 40, 24, null).toEntity());
        UUID wp = westpark.getId();
        List<ParkingSpot> spots = parkingSpotService.createBulk(wp, List.of("B1", "B2", "B7", "B8"));

        Apartment apt301 = apartmentService.create(wp, new ApartmentRequest("301", 3, 2).toEntity());
        Apartment apt302 = apartmentService.create(wp, new ApartmentRequest("302", 2, 2).toEntity());
        Apartment apt304 = apartmentService.create(wp, new ApartmentRequest("304", 2, 2).toEntity());

        // 301: reserved spot + permit, so AUTHORIZED
        vehicleService.create(wp, apt301.getId(), new VehicleRequest("KXM-2190", "Honda", "Civic", 2021, "Black").toEntity());
        vehicleService.create(wp, apt301.getId(), new VehicleRequest("RBD 4407", "Ford", "Escape", 2017, "Gray").toEntity());
        authorizationService.create(wp, apt301.getId(), new CreateAuthorizationRequest(spotId(spots, "B1"), today.plusMonths(2)));
        authorizationService.create(wp, apt301.getId(), new CreateAuthorizationRequest(null, null));

        // 302: a car with nothing active, the dangerous case
        vehicleService.create(wp, apt302.getId(), new VehicleRequest("LTR-8842", "Toyota", "Corolla", 2019, "White").toEntity());

        // 304: permit that ends today. Still AUTHORIZED, the last day counts
        vehicleService.create(wp, apt304.getId(), new VehicleRequest("JDX 5521", "Nissan", "Sentra", 2020, "Blue").toEntity());
        authorizationService.create(wp, apt304.getId(), new CreateAuthorizationRequest(null, today));

        // ---------- Bayou Lofts: proves a plate only counts in its own property ----------
        Property bayou = propertyRepository.save(new PropertyRequest("Bayou Lofts", 10, 20, 12, null).toEntity());
        Apartment apt1A = apartmentService.create(bayou.getId(), new ApartmentRequest("1A", 2, 2).toEntity());
        vehicleService.create(bayou.getId(), apt1A.getId(), new VehicleRequest("GHT-5531", "Jeep", "Wrangler", 2018, "Red").toEntity());
        authorizationService.create(bayou.getId(), apt1A.getId(), new CreateAuthorizationRequest(null, null));

        // ---------- One account per role ----------
        createUser(superAdmin, "operator@esw.test", Role.TOWING_OPERATOR, null, null, false);
        createUser(superAdmin, "admin@westpark.test", Role.PROPERTY_ADMIN, wp, null, false);
        createUser(superAdmin, "resident301@westpark.test", Role.RESIDENT, wp, apt301.getId(), false);
        // Keeps the temporary password, to test the forced change
        createUser(superAdmin, "resident302@westpark.test", Role.RESIDENT, wp, apt302.getId(), true);

        log.info("Dev data created: Westpark Apartments + Bayou Lofts, 4 demo accounts (see DevDataSeeder)");
    }

    private void createUser(AppUser creator, String email, Role role, UUID propertyId, UUID apartmentId,
                            boolean keepTemporaryPassword) {
        AppUser user = userService.create(creator, new CreateUserRequest(email, DEMO_PASSWORD, role, propertyId, apartmentId));
        if (!keepTemporaryPassword) {
            user.setMustChangePassword(false);
            userRepository.save(user);
        }
    }

    private UUID spotId(List<ParkingSpot> spots, String label) {
        return spots.stream()
                .filter(spot -> spot.getLabel().equals(label))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}