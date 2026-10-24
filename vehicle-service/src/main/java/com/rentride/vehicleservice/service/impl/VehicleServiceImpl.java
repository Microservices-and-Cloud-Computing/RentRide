package com.rentride.vehicleservice.service.impl;

import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
import com.rentride.vehicleservice.exception.DuplicateResourceException;
import com.rentride.vehicleservice.exception.ResourceNotFoundException;
import com.rentride.vehicleservice.exception.UnauthorizedAccessException;
import com.rentride.vehicleservice.repository.CategoryRepository;
import com.rentride.vehicleservice.repository.VehicleRepository;
import com.rentride.vehicleservice.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link VehicleService} providing business operations for
 * vehicle onboarding, provider ownership enforcement, inventory queries, and availability management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public VehicleResponse createVehicle(Long providerId, VehicleCreateRequest request) {
        log.info("Registering new vehicle for providerId={}, plate={}", providerId, request.getLicensePlateNumber());

        // Verify category existence
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle category not found with id: " + request.getCategoryId()));

        // Validate license plate uniqueness
        String normalizedPlate = request.getLicensePlateNumber().trim().toUpperCase();
        if (vehicleRepository.existsByLicensePlateNumber(normalizedPlate)) {
            log.warn("Vehicle registration rejected: license plate '{}' already exists", normalizedPlate);
            throw new DuplicateResourceException("Vehicle with license plate number '" + normalizedPlate + "' already exists");
        }

        // Map and persist new vehicle entity
        Vehicle vehicle = Vehicle.builder()
                .providerId(providerId)
                .make(request.getMake().trim())
                .model(request.getModel().trim())
                .manufacturingYear(request.getManufacturingYear())
                .licensePlateNumber(normalizedPlate)
                .seatingCapacity(request.getSeatingCapacity())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .dailyRentalRate(request.getDailyRentalRate())
                .status(VehicleStatus.AVAILABLE)
                .location(request.getLocation().trim())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .category(category)
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Successfully registered vehicle id={} for providerId={}", savedVehicle.getId(), providerId);

        return mapToResponse(savedVehicle);
    }

    @Override
    public VehicleResponse updateVehicle(Long vehicleId, Long providerId, VehicleUpdateRequest request) {
        log.info("Updating vehicle id={} by providerId={}", vehicleId, providerId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        // Enforce provider ownership
        if (!vehicle.getProviderId().equals(providerId)) {
            log.warn("Ownership violation: providerId={} attempted to update vehicle id={} owned by providerId={}",
                    providerId, vehicleId, vehicle.getProviderId());
            throw new UnauthorizedAccessException("Provider " + providerId + " is not authorized to modify vehicle " + vehicleId);
        }

        vehicle.setDailyRentalRate(request.getDailyRentalRate());
        vehicle.setLocation(request.getLocation().trim());
        vehicle.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        vehicle.setSeatingCapacity(request.getSeatingCapacity());
        vehicle.setStatus(request.getStatus());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("Successfully updated vehicle id={}", updatedVehicle.getId());

        return mapToResponse(updatedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long vehicleId) {
        log.debug("Fetching vehicle details for id={}", vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        return mapToResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByProvider(Long providerId) {
        log.debug("Fetching vehicle fleet for providerId={}", providerId);

        return vehicleRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteVehicle(Long vehicleId, Long providerId) {
        log.info("Deleting / decommissioning vehicle id={} by providerId={}", vehicleId, providerId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        // Enforce provider ownership
        if (!vehicle.getProviderId().equals(providerId)) {
            log.warn("Ownership violation: providerId={} attempted to delete vehicle id={} owned by providerId={}",
                    providerId, vehicleId, vehicle.getProviderId());
            throw new UnauthorizedAccessException("Provider " + providerId + " is not authorized to delete vehicle " + vehicleId);
        }

        // Soft-delete by setting status to DECOMMISSIONED
        vehicle.setStatus(VehicleStatus.DECOMMISSIONED);
        vehicleRepository.save(vehicle);
        log.info("Successfully decommissioned vehicle id={}", vehicleId);
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .providerId(vehicle.getProviderId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .manufacturingYear(vehicle.getManufacturingYear())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .categoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : null)
                .categoryId(vehicle.getCategory() != null ? vehicle.getCategory().getId() : null)
                .seatingCapacity(vehicle.getSeatingCapacity())
                .fuelType(vehicle.getFuelType())
                .transmission(vehicle.getTransmission())
                .dailyRentalRate(vehicle.getDailyRentalRate())
                .status(vehicle.getStatus())
                .location(vehicle.getLocation())
                .imageUrl(vehicle.getImageUrl())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
