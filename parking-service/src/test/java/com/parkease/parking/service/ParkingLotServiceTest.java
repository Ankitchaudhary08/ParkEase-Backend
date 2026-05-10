package com.parkease.parking.service;

import com.parkease.parking.dto.CreateLotRequest;
import com.parkease.parking.entity.ParkingLot;
import com.parkease.parking.repository.ParkingLotRepository;
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
class ParkingLotServiceTest {

    @Mock ParkingLotRepository lotRepository;

    @InjectMocks ParkingLotService parkingLotService;

    ParkingLot pendingLot;
    ParkingLot approvedLot;

    @BeforeEach
    void setUp() {
        pendingLot = ParkingLot.builder()
                .lotId(1L).name("Central Park").address("1 Main St").city("Mumbai")
                .latitude(19.07).longitude(72.87)
                .totalSpots(50).availableSpots(0)
                .managerId(5L).isOpen(false).hourlyRate(60.0)
                .approvalStatus(ParkingLot.ApprovalStatus.PENDING)
                .build();

        approvedLot = ParkingLot.builder()
                .lotId(2L).name("Bay View").address("5 Bay Rd").city("Pune")
                .totalSpots(30).availableSpots(15)
                .managerId(5L).isOpen(true).hourlyRate(40.0)
                .approvalStatus(ParkingLot.ApprovalStatus.APPROVED)
                .build();
    }

    // ── createLot ────────────────────────────────────────────────────────────

    @Test
    void createLot_savesLotWithPendingStatus() {
        CreateLotRequest req = CreateLotRequest.builder()
                .name("Central Park").address("1 Main St").city("Mumbai")
                .latitude(19.07).longitude(72.87)
                .totalSpots(50).hourlyRate(60.0).build();

        given(lotRepository.save(any(ParkingLot.class))).willReturn(pendingLot);

        ParkingLot result = parkingLotService.createLot(req, 5L);

        assertThat(result.getApprovalStatus()).isEqualTo(ParkingLot.ApprovalStatus.PENDING);
        then(lotRepository).should().save(argThat(l ->
                l.getManagerId().equals(5L) &&
                l.getAvailableSpots() == 0 &&
                !l.isOpen() &&
                l.getApprovalStatus() == ParkingLot.ApprovalStatus.PENDING));
    }

    @Test
    void createLot_setsIsOpenFalse_byDefault() {
        CreateLotRequest req = CreateLotRequest.builder()
                .name("Test Lot").address("Addr").city("Delhi")
                .totalSpots(20).hourlyRate(30.0).build();

        given(lotRepository.save(any(ParkingLot.class))).willReturn(pendingLot);

        ParkingLot result = parkingLotService.createLot(req, 3L);

        assertThat(result.isOpen()).isFalse();
    }

    // ── getLotById ───────────────────────────────────────────────────────────

    @Test
    void getLotById_returnsLot_whenFound() {
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot));

        ParkingLot result = parkingLotService.getLotById(1L);

        assertThat(result.getName()).isEqualTo("Central Park");
        assertThat(result.getLotId()).isEqualTo(1L);
    }

    @Test
    void getLotById_throwsRuntimeException_whenNotFound() {
        given(lotRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> parkingLotService.getLotById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parking lot not found: 99");
    }

    // ── getLotsByCity ────────────────────────────────────────────────────────

    @Test
    void getLotsByCity_returnsExactMatches_whenAvailable() {
        ParkingLot mumbaiLot = ParkingLot.builder()
                .lotId(3L).city("Mumbai")
                .approvalStatus(ParkingLot.ApprovalStatus.APPROVED).build();

        given(lotRepository.findByCityIgnoreCaseAndApprovalStatus("Mumbai", ParkingLot.ApprovalStatus.APPROVED))
                .willReturn(List.of(mumbaiLot));

        List<ParkingLot> result = parkingLotService.getLotsByCity("Mumbai");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLotId()).isEqualTo(3L);
    }

    @Test
    void getLotsByCity_fallsBackToContainsSearch_whenNoExactMatch() {
        given(lotRepository.findByCityIgnoreCaseAndApprovalStatus("Mum", ParkingLot.ApprovalStatus.APPROVED))
                .willReturn(List.of());
        given(lotRepository.findByCityIgnoreCaseContainingAndApprovalStatus("Mum", ParkingLot.ApprovalStatus.APPROVED))
                .willReturn(List.of(approvedLot));

        List<ParkingLot> result = parkingLotService.getLotsByCity("Mum");

        assertThat(result).hasSize(1);
    }

    // ── toggleOpen ───────────────────────────────────────────────────────────

    @Test
    void toggleOpen_togglingClosedLot_opensIt() {
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot)); // isOpen=false
        given(lotRepository.save(any(ParkingLot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingLot result = parkingLotService.toggleOpen(1L);

        assertThat(result.isOpen()).isTrue();
    }

    @Test
    void toggleOpen_togglingOpenLot_closesIt() {
        given(lotRepository.findById(2L)).willReturn(Optional.of(approvedLot)); // isOpen=true
        given(lotRepository.save(any(ParkingLot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingLot result = parkingLotService.toggleOpen(2L);

        assertThat(result.isOpen()).isFalse();
    }

    // ── approveLot ───────────────────────────────────────────────────────────

    @Test
    void approveLot_setsApprovedStatus_andOpensLot() {
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot));
        given(lotRepository.save(any(ParkingLot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingLot result = parkingLotService.approveLot(1L, true, null);

        assertThat(result.getApprovalStatus()).isEqualTo(ParkingLot.ApprovalStatus.APPROVED);
        assertThat(result.isOpen()).isTrue();
    }

    @Test
    void approveLot_setsRejectedStatus_withReason() {
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot));
        given(lotRepository.save(any(ParkingLot.class))).willAnswer(inv -> inv.getArgument(0));

        ParkingLot result = parkingLotService.approveLot(1L, false, "Incomplete documents");

        assertThat(result.getApprovalStatus()).isEqualTo(ParkingLot.ApprovalStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Incomplete documents");
        assertThat(result.isOpen()).isFalse(); // rejected lots stay closed
    }

    // ── decrementAvailable ───────────────────────────────────────────────────

    @Test
    void decrementAvailable_decrementsCount_whenSpotsAreAvailable() {
        pendingLot.setAvailableSpots(5);
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(pendingLot);

        parkingLotService.decrementAvailable(1L);

        assertThat(pendingLot.getAvailableSpots()).isEqualTo(4);
        then(lotRepository).should().save(pendingLot);
    }

    @Test
    void decrementAvailable_doesNothing_whenAvailableSpotsIsZero() {
        pendingLot.setAvailableSpots(0);
        given(lotRepository.findById(1L)).willReturn(Optional.of(pendingLot));

        parkingLotService.decrementAvailable(1L);

        assertThat(pendingLot.getAvailableSpots()).isEqualTo(0);
        then(lotRepository).should(never()).save(any());
    }

    // ── incrementAvailable ───────────────────────────────────────────────────

    @Test
    void incrementAvailable_incrementsCount() {
        approvedLot.setAvailableSpots(10);
        given(lotRepository.findById(2L)).willReturn(Optional.of(approvedLot));
        given(lotRepository.save(any(ParkingLot.class))).willReturn(approvedLot);

        parkingLotService.incrementAvailable(2L);

        assertThat(approvedLot.getAvailableSpots()).isEqualTo(11);
    }

    // ── getPendingApproval ───────────────────────────────────────────────────

    @Test
    void getPendingApproval_returnsOnlyPendingLots() {
        given(lotRepository.findByApprovalStatus(ParkingLot.ApprovalStatus.PENDING))
                .willReturn(List.of(pendingLot));

        List<ParkingLot> result = parkingLotService.getPendingApproval();

        assertThat(result).hasSize(1)
                .allMatch(l -> l.getApprovalStatus() == ParkingLot.ApprovalStatus.PENDING);
    }

    // ── getAllLots ────────────────────────────────────────────────────────────

    @Test
    void getAllLots_returnsAllLotsFromRepository() {
        given(lotRepository.findAll()).willReturn(List.of(pendingLot, approvedLot));

        List<ParkingLot> result = parkingLotService.getAllLots();

        assertThat(result).hasSize(2);
    }

    // ── deleteLot ────────────────────────────────────────────────────────────

    @Test
    void deleteLot_callsRepositoryDeleteById() {
        willDoNothing().given(lotRepository).deleteById(1L);

        parkingLotService.deleteLot(1L);

        then(lotRepository).should().deleteById(1L);
    }
}
