package com.parkease.parking.service;

import com.parkease.parking.dto.AddSpotRequest;
import com.parkease.parking.entity.ParkingLot;
import com.parkease.parking.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceTest {

    @Mock ParkingSpotRepository spotRepository;
    @Mock ParkingLotRepository lotRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    @InjectMocks ParkingSpotService parkingSpotService;

    ParkingLot lot;
    ParkingSpot availableSpot;

    @BeforeEach
    void setUp() {
        lot = ParkingLot.builder()
                .lotId(1L).totalSpots(10).availableSpots(5).build();

        availableSpot = ParkingSpot.builder()
                .spotId(1L).lot(lot)
                .spotNumber("A1").floor(1)
                .spotType(ParkingSpot.SpotType.COMPACT)
                .vehicleType(ParkingSpot.VehicleType.FOUR_WHEELER)
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .pricePerHour(50.0).build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── addSpot ──────────────────────────────────────────────────────────────

    @Test
    void addSpot_savesSpot_andIncrementsBothLotCounters() {
        AddSpotRequest req = AddSpotRequest.builder()
                .spotNumber("A1").floor(1)
                .spotType(ParkingSpot.SpotType.COMPACT)
                .vehicleType(ParkingSpot.VehicleType.FOUR_WHEELER)
                .pricePerHour(50.0).build();

        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(spotRepository.save(any(ParkingSpot.class))).willReturn(availableSpot);
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        ParkingSpot result = parkingSpotService.addSpot(1L, req);

        assertThat(result.getSpotNumber()).isEqualTo("A1");
        assertThat(lot.getTotalSpots()).isEqualTo(11);
        assertThat(lot.getAvailableSpots()).isEqualTo(6);
    }

    @Test
    void addSpot_setsStatusToAvailable() {
        AddSpotRequest req = AddSpotRequest.builder()
                .spotNumber("B2").floor(2)
                .spotType(ParkingSpot.SpotType.LARGE)
                .vehicleType(ParkingSpot.VehicleType.HEAVY)
                .pricePerHour(80.0).build();

        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        ParkingSpot result = parkingSpotService.addSpot(1L, req);

        assertThat(result.getStatus()).isEqualTo(ParkingSpot.SpotStatus.AVAILABLE);
    }

    @Test
    void addSpot_throwsRuntimeException_whenLotNotFound() {
        given(lotRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingSpotService.addSpot(99L, new AddSpotRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Lot not found");
    }

    // ── addBulkSpots ─────────────────────────────────────────────────────────

    @Test
    void addBulkSpots_addsEachSpotIndividually() {
        AddSpotRequest r1 = AddSpotRequest.builder().spotNumber("C1").floor(1)
                .spotType(ParkingSpot.SpotType.COMPACT).vehicleType(ParkingSpot.VehicleType.FOUR_WHEELER)
                .pricePerHour(50.0).build();
        AddSpotRequest r2 = AddSpotRequest.builder().spotNumber("C2").floor(1)
                .spotType(ParkingSpot.SpotType.COMPACT).vehicleType(ParkingSpot.VehicleType.FOUR_WHEELER)
                .pricePerHour(50.0).build();

        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        List<ParkingSpot> result = parkingSpotService.addBulkSpots(1L, List.of(r1, r2));

        assertThat(result).hasSize(2);
        then(spotRepository).should(times(2)).save(any(ParkingSpot.class));
    }

    // ── occupySpot ───────────────────────────────────────────────────────────

    @Test
    void occupySpot_success_fromAvailableStatus() {
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingSpot result = parkingSpotService.occupySpot(1L);

        assertThat(result.getStatus()).isEqualTo(ParkingSpot.SpotStatus.OCCUPIED);
    }

    @Test
    void occupySpot_success_fromReservedStatus() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.RESERVED);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingSpot result = parkingSpotService.occupySpot(1L);

        assertThat(result.getStatus()).isEqualTo(ParkingSpot.SpotStatus.OCCUPIED);
    }

    @Test
    void occupySpot_throwsIllegalState_whenAlreadyOccupied() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));

        assertThatThrownBy(() -> parkingSpotService.occupySpot(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available for occupation");
    }

    // ── reserveSpot ──────────────────────────────────────────────────────────

    @Test
    void reserveSpot_success_whenAvailable() {
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingSpot result = parkingSpotService.reserveSpot(1L);

        assertThat(result.getStatus()).isEqualTo(ParkingSpot.SpotStatus.RESERVED);
    }

    @Test
    void reserveSpot_throwsIllegalState_whenAlreadyOccupied() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));

        assertThatThrownBy(() -> parkingSpotService.reserveSpot(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Spot is not available");
    }

    @Test
    void reserveSpot_throwsIllegalState_whenAlreadyReserved() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.RESERVED);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));

        assertThatThrownBy(() -> parkingSpotService.reserveSpot(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── releaseSpot ──────────────────────────────────────────────────────────

    @Test
    void releaseSpot_setsStatusToAvailable_fromOccupied() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(spotRepository.save(any(ParkingSpot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingSpot result = parkingSpotService.releaseSpot(1L);

        assertThat(result.getStatus()).isEqualTo(ParkingSpot.SpotStatus.AVAILABLE);
    }

    @Test
    void releaseSpot_throwsRuntimeException_whenSpotNotFound() {
        given(spotRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingSpotService.releaseSpot(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Spot not found");
    }

    // ── deleteSpot ───────────────────────────────────────────────────────────

    @Test
    void deleteSpot_decrementsBothCounters_whenSpotWasAvailable() {
        lot.setTotalSpots(10);
        lot.setAvailableSpots(5);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        parkingSpotService.deleteSpot(1L);

        assertThat(lot.getTotalSpots()).isEqualTo(9);
        assertThat(lot.getAvailableSpots()).isEqualTo(4);
    }

    @Test
    void deleteSpot_decrementsOnlyTotal_whenSpotWasOccupied() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        lot.setTotalSpots(10);
        lot.setAvailableSpots(3);
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        parkingSpotService.deleteSpot(1L);

        assertThat(lot.getTotalSpots()).isEqualTo(9);
        assertThat(lot.getAvailableSpots()).isEqualTo(3); // unchanged
    }

    @Test
    void deleteSpot_doesNotGoBelowZero_forAvailableSpots() {
        availableSpot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        lot.setTotalSpots(1);
        lot.setAvailableSpots(0); // already 0, should stay 0
        given(spotRepository.findById(1L)).willReturn(Optional.of(availableSpot));
        given(lotRepository.findById(1L)).willReturn(Optional.of(lot));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(lot);

        parkingSpotService.deleteSpot(1L);

        assertThat(lot.getAvailableSpots()).isEqualTo(0);
    }

    // ── countAvailable ───────────────────────────────────────────────────────

    @Test
    void countAvailable_returnsFromCache_whenCacheHit() {
        given(valueOps.get("spot:available:lot:1")).willReturn(12);

        int count = parkingSpotService.countAvailable(1L);

        assertThat(count).isEqualTo(12);
        then(spotRepository).should(never())
                .countByLotLotIdAndStatus(any(), any());
    }

    @Test
    void countAvailable_queriesRepository_whenCacheMiss() {
        given(valueOps.get("spot:available:lot:1")).willReturn(null);
        given(spotRepository.countByLotLotIdAndStatus(1L, ParkingSpot.SpotStatus.AVAILABLE))
                .willReturn(7);

        int count = parkingSpotService.countAvailable(1L);

        assertThat(count).isEqualTo(7);
        then(spotRepository).should().countByLotLotIdAndStatus(1L, ParkingSpot.SpotStatus.AVAILABLE);
    }

    // ── getAvailableSpots ────────────────────────────────────────────────────

    @Test
    void getAvailableSpots_returnsSpotList_fromRepository() {
        given(valueOps.get("spot:available:lot:1")).willReturn(null);
        given(spotRepository.findByLotLotIdAndStatus(1L, ParkingSpot.SpotStatus.AVAILABLE))
                .willReturn(List.of(availableSpot));

        List<ParkingSpot> result = parkingSpotService.getAvailableSpots(1L);

        assertThat(result).hasSize(1)
                .allMatch(s -> s.getStatus() == ParkingSpot.SpotStatus.AVAILABLE);
    }
}
