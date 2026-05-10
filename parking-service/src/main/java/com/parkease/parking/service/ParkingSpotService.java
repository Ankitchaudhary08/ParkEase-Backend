package com.parkease.parking.service;

import com.parkease.parking.dto.AddSpotRequest;
import com.parkease.parking.entity.ParkingLot;
import com.parkease.parking.entity.ParkingSpot;
import com.parkease.parking.repository.ParkingLotRepository;
import com.parkease.parking.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingLotRepository lotRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SPOT_COUNT_KEY = "spot:available:lot:";

    @Transactional
    public ParkingSpot addSpot(Long lotId, AddSpotRequest request) {
        ParkingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found"));
        ParkingSpot spot = ParkingSpot.builder()
                .lot(lot)
                .spotNumber(request.getSpotNumber())
                .floor(request.getFloor())
                .spotType(request.getSpotType())
                .vehicleType(request.getVehicleType())
                .isHandicapped(request.isHandicapped())
                .isEVCharging(request.isEVCharging())
                .pricePerHour(request.getPricePerHour())
                .status(ParkingSpot.SpotStatus.AVAILABLE)
                .build();
        ParkingSpot saved = spotRepository.save(spot);
        lot.setTotalSpots(lot.getTotalSpots() + 1);
        lot.setAvailableSpots(lot.getAvailableSpots() + 1);
        lotRepository.save(lot);
        evictAvailabilityCache(lotId);
        return saved;
    }

    @Transactional
    public List<ParkingSpot> addBulkSpots(Long lotId, List<AddSpotRequest> requests) {
        return requests.stream().map(r -> addSpot(lotId, r)).toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingSpot> getSpotsByLot(Long lotId) {
        return spotRepository.findByLotLotId(lotId);
    }

    @Transactional(readOnly = true)
    public List<ParkingSpot> getAvailableSpots(Long lotId) {
        String cacheKey = SPOT_COUNT_KEY + lotId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return spotRepository.findByLotLotIdAndStatus(lotId, ParkingSpot.SpotStatus.AVAILABLE);
            }
        } catch (Exception ignored) {}
        List<ParkingSpot> spots = spotRepository.findByLotLotIdAndStatus(lotId, ParkingSpot.SpotStatus.AVAILABLE);
        try {
            redisTemplate.opsForValue().set(cacheKey, spots.size(), Duration.ofSeconds(30));
        } catch (Exception ignored) {}
        return spots;
    }

    @Transactional
    public ParkingSpot occupySpot(Long spotId) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));
        if (spot.getStatus() != ParkingSpot.SpotStatus.AVAILABLE && spot.getStatus() != ParkingSpot.SpotStatus.RESERVED) {
            throw new IllegalStateException("Spot is not available for occupation");
        }
        spot.setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        evictAvailabilityCache(spot.getLot().getLotId());
        return spotRepository.save(spot);
    }

    @Transactional
    public ParkingSpot reserveSpot(Long spotId) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));
        if (spot.getStatus() != ParkingSpot.SpotStatus.AVAILABLE) {
            throw new IllegalStateException("Spot is not available");
        }
        spot.setStatus(ParkingSpot.SpotStatus.RESERVED);
        evictAvailabilityCache(spot.getLot().getLotId());
        return spotRepository.save(spot);
    }

    @Transactional
    public ParkingSpot releaseSpot(Long spotId) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));
        spot.setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        evictAvailabilityCache(spot.getLot().getLotId());
        return spotRepository.save(spot);
    }

    @Transactional
    public ParkingSpot updateSpot(Long spotId, AddSpotRequest request) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));
        if (request.getPricePerHour() != null) spot.setPricePerHour(request.getPricePerHour());
        if (request.getSpotType() != null) spot.setSpotType(request.getSpotType());
        spot.setEVCharging(request.isEVCharging());
        spot.setHandicapped(request.isHandicapped());
        return spotRepository.save(spot);
    }

    @Transactional
    public void deleteSpot(Long spotId) {
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found"));
        Long lotId = spot.getLot().getLotId();
        spotRepository.deleteById(spotId);
        ParkingLot lot = lotRepository.findById(lotId).orElseThrow();
        lot.setTotalSpots(Math.max(0, lot.getTotalSpots() - 1));
        if (spot.getStatus() == ParkingSpot.SpotStatus.AVAILABLE) {
            lot.setAvailableSpots(Math.max(0, lot.getAvailableSpots() - 1));
        }
        lotRepository.save(lot);
        evictAvailabilityCache(lotId);
    }

    public int countAvailable(Long lotId) {
        String cacheKey = SPOT_COUNT_KEY + lotId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Integer count) return count;
        } catch (Exception ignored) {}
        int count = spotRepository.countByLotLotIdAndStatus(lotId, ParkingSpot.SpotStatus.AVAILABLE);
        try {
            redisTemplate.opsForValue().set(cacheKey, count, Duration.ofSeconds(30));
        } catch (Exception ignored) {}
        return count;
    }

    private void evictAvailabilityCache(Long lotId) {
        try {
            redisTemplate.delete(SPOT_COUNT_KEY + lotId);
        } catch (Exception ignored) {}
    }
}
