package com.parkease.booking.service;

import com.parkease.booking.dto.RegisterVehicleRequest;
import com.parkease.booking.entity.Vehicle;
import com.parkease.booking.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock VehicleRepository vehicleRepository;

    @InjectMocks VehicleService vehicleService;

    Vehicle savedVehicle;

    @BeforeEach
    void setUp() {
        savedVehicle = Vehicle.builder()
                .vehicleId(1L).ownerId(10L)
                .licensePlate("MH01AB1234")
                .make("Maruti").model("Swift").color("Red")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER)
                .isEV(false).isActive(true)
                .build();
    }

    // ── registerVehicle ──────────────────────────────────────────────────────

    @Test
    void registerVehicle_success_savesVehicle() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .licensePlate("mh01ab1234").make("Maruti").model("Swift")
                .color("Red").vehicleType(Vehicle.VehicleType.FOUR_WHEELER)
                .isEV(false).build();

        given(vehicleRepository.existsByLicensePlate("mh01ab1234")).willReturn(false);
        given(vehicleRepository.save(any(Vehicle.class))).willReturn(savedVehicle);

        Vehicle result = vehicleService.registerVehicle(req, 10L);

        assertThat(result.getOwnerId()).isEqualTo(10L);
        assertThat(result.getMake()).isEqualTo("Maruti");
        then(vehicleRepository).should().save(any(Vehicle.class));
    }

    @Test
    void registerVehicle_uppercasesLicensePlate_beforeSaving() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .licensePlate("mh01ab1234")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER).build();

        given(vehicleRepository.existsByLicensePlate("mh01ab1234")).willReturn(false);
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.registerVehicle(req, 10L);

        assertThat(result.getLicensePlate()).isEqualTo("MH01AB1234");
    }

    @Test
    void registerVehicle_setsOwnerIdFromParameter() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .licensePlate("GJ01XY9999")
                .vehicleType(Vehicle.VehicleType.TWO_WHEELER).build();

        given(vehicleRepository.existsByLicensePlate("GJ01XY9999")).willReturn(false);
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.registerVehicle(req, 55L);

        assertThat(result.getOwnerId()).isEqualTo(55L);
    }

    @Test
    void registerVehicle_setsIsActiveTrue() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .licensePlate("DL01AA0001")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER).build();

        given(vehicleRepository.existsByLicensePlate("DL01AA0001")).willReturn(false);
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.registerVehicle(req, 1L);

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void registerVehicle_throwsIllegalArgument_whenPlateAlreadyRegistered() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .licensePlate("MH01AB1234")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER).build();

        given(vehicleRepository.existsByLicensePlate("MH01AB1234")).willReturn(true);

        assertThatThrownBy(() -> vehicleService.registerVehicle(req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("License plate already registered");

        then(vehicleRepository).should(never()).save(any());
    }

    // ── getVehiclesByOwner ───────────────────────────────────────────────────

    @Test
    void getVehiclesByOwner_returnsOnlyActiveVehicles() {
        given(vehicleRepository.findByOwnerIdAndIsActive(10L, true))
                .willReturn(List.of(savedVehicle));

        List<Vehicle> result = vehicleService.getVehiclesByOwner(10L);

        assertThat(result).hasSize(1).allMatch(Vehicle::isActive);
    }

    @Test
    void getVehiclesByOwner_returnsEmptyList_whenOwnerHasNoVehicles() {
        given(vehicleRepository.findByOwnerIdAndIsActive(99L, true))
                .willReturn(List.of());

        assertThat(vehicleService.getVehiclesByOwner(99L)).isEmpty();
    }

    // ── getVehicleById ───────────────────────────────────────────────────────

    @Test
    void getVehicleById_returnsVehicle_whenFound() {
        given(vehicleRepository.findById(1L)).willReturn(Optional.of(savedVehicle));

        Vehicle result = vehicleService.getVehicleById(1L);

        assertThat(result.getVehicleId()).isEqualTo(1L);
        assertThat(result.getLicensePlate()).isEqualTo("MH01AB1234");
    }

    @Test
    void getVehicleById_throwsRuntimeException_whenNotFound() {
        given(vehicleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicleById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vehicle not found");
    }

    // ── updateVehicle ────────────────────────────────────────────────────────

    @Test
    void updateVehicle_updatesOnlyProvidedFields() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .color("Blue").isEV(true).build();

        given(vehicleRepository.findById(1L)).willReturn(Optional.of(savedVehicle));
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.updateVehicle(1L, req);

        assertThat(result.getColor()).isEqualTo("Blue");
        assertThat(result.isEV()).isTrue();
        assertThat(result.getMake()).isEqualTo("Maruti");   // unchanged
        assertThat(result.getModel()).isEqualTo("Swift");   // unchanged
    }

    @Test
    void updateVehicle_doesNotOverwriteMake_whenRequestMakeIsNull() {
        RegisterVehicleRequest req = RegisterVehicleRequest.builder()
                .model("Baleno").build();

        given(vehicleRepository.findById(1L)).willReturn(Optional.of(savedVehicle));
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.updateVehicle(1L, req);

        assertThat(result.getMake()).isEqualTo("Maruti");
        assertThat(result.getModel()).isEqualTo("Baleno");
    }

    // ── deleteVehicle ────────────────────────────────────────────────────────

    @Test
    void deleteVehicle_softDeletesBySettingIsActiveFalse() {
        given(vehicleRepository.findById(1L)).willReturn(Optional.of(savedVehicle));
        given(vehicleRepository.save(any(Vehicle.class))).willAnswer(inv -> inv.getArgument(0));

        vehicleService.deleteVehicle(1L);

        assertThat(savedVehicle.isActive()).isFalse();
        then(vehicleRepository).should().save(savedVehicle);
    }

    @Test
    void deleteVehicle_throwsRuntimeException_whenVehicleNotFound() {
        given(vehicleRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.deleteVehicle(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vehicle not found");
    }
}
