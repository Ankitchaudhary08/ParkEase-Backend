package com.parkease.parking.service;

import com.parkease.parking.dto.*;
import com.parkease.parking.entity.ParkingLot;
import com.parkease.parking.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository lotRepository;

    @Transactional
    public ParkingLot createLot(CreateLotRequest request, Long managerId) {
        ParkingLot lot = ParkingLot.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalSpots(request.getTotalSpots())
                .availableSpots(0)
                .managerId(managerId)
                .isOpen(false)
                .hourlyRate(request.getHourlyRate())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .imageUrl(request.getImageUrl())
                .approvalStatus(ParkingLot.ApprovalStatus.PENDING)
                .build();
        return lotRepository.save(lot);
    }

    public ParkingLot getLotById(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Parking lot not found: " + lotId));
    }

    @Cacheable(value = "lots_by_city", key = "#city.toLowerCase()")
    public List<ParkingLot> getLotsByCity(String city) {
        List<ParkingLot> exact = lotRepository.findByCityIgnoreCaseAndApprovalStatus(city, ParkingLot.ApprovalStatus.APPROVED);
        if (!exact.isEmpty()) return exact;
        return lotRepository.findByCityIgnoreCaseContainingAndApprovalStatus(city, ParkingLot.ApprovalStatus.APPROVED);
    }

    @Transactional
    @CacheEvict(value = "lots_by_city", allEntries = true)
    public ParkingLot updateLot(Long lotId, UpdateLotRequest request) {
        ParkingLot lot = getLotById(lotId);
        if (request.getName() != null) lot.setName(request.getName());
        if (request.getAddress() != null) lot.setAddress(request.getAddress());
        if (request.getOpenTime() != null) lot.setOpenTime(request.getOpenTime());
        if (request.getCloseTime() != null) lot.setCloseTime(request.getCloseTime());
        if (request.getImageUrl() != null) lot.setImageUrl(request.getImageUrl());
        return lotRepository.save(lot);
    }

    @Transactional
    public ParkingLot toggleOpen(Long lotId) {
        ParkingLot lot = getLotById(lotId);
        lot.setOpen(!lot.isOpen());
        return lotRepository.save(lot);
    }

    @Transactional
    @CacheEvict(value = "lots_by_city", allEntries = true)
    public ParkingLot approveLot(Long lotId, boolean approved, String reason) {
        ParkingLot lot = getLotById(lotId);
        lot.setApprovalStatus(approved ? ParkingLot.ApprovalStatus.APPROVED : ParkingLot.ApprovalStatus.REJECTED);
        if (!approved) lot.setRejectionReason(reason);
        if (approved) lot.setOpen(true);
        return lotRepository.save(lot);
    }

    @Transactional
    public void decrementAvailable(Long lotId) {
        ParkingLot lot = getLotById(lotId);
        if (lot.getAvailableSpots() > 0) {
            lot.setAvailableSpots(lot.getAvailableSpots() - 1);
            lotRepository.save(lot);
        }
    }

    @Transactional
    public void incrementAvailable(Long lotId) {
        ParkingLot lot = getLotById(lotId);
        lot.setAvailableSpots(lot.getAvailableSpots() + 1);
        lotRepository.save(lot);
    }

    public List<ParkingLot> getNearbyLots(double lat, double lon, double radiusKm) {
        return lotRepository.findNearby(lat, lon, radiusKm);
    }

    public List<ParkingLot> getLotsByManager(Long managerId) {
        return lotRepository.findByManagerId(managerId);
    }

    public List<ParkingLot> getPendingApproval() {
        return lotRepository.findByApprovalStatus(ParkingLot.ApprovalStatus.PENDING);
    }

    public List<ParkingLot> getAllLots() {
        return lotRepository.findAll();
    }

    @Transactional
    public void deleteLot(Long lotId) {
        lotRepository.deleteById(lotId);
    }
}
