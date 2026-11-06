package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.exception.BookingConflictException;
import com.rentride.bookingservice.exception.InvalidBookingDatesException;
import com.rentride.bookingservice.exception.VehicleUnavailableException;
import com.rentride.bookingservice.repository.BookingRepository;
import com.rentride.bookingservice.util.BookingReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleIntegrationService vehicleIntegrationService;

    @Mock
    private BookingReferenceGenerator bookingReferenceGenerator;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private VehicleResponseDto availableVehicle;

    @BeforeEach
    void setUp() {
        availableVehicle = VehicleResponseDto.builder()
                .id(1L)
                .providerId(2L)
                .make("Toyota")
                .model("Aqua")
                .dailyRentalRate(new BigDecimal("10000.00"))
                .status("AVAILABLE")
                .location("Colombo")
                .licensePlateNumber("WP-CBA-1234")
                .build();
    }

    @Test
    @DisplayName("testCreateBooking_Success: Mock Vehicle-Service returning available vehicle, assert PENDING_PAYMENT")
    void testCreateBooking_Success() {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 4))
                .pickupNotes("Arriving around 10:00 AM")
                .build();

        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), anyList(), eq(request.getEndDate()), eq(request.getStartDate())
        )).thenReturn(false);
        when(bookingReferenceGenerator.generate()).thenReturn("RR-BK-20261101-ABC123");

        Booking savedBooking = Booking.builder()
                .id(100L)
                .bookingReference("RR-BK-20261101-ABC123")
                .customerId(10L)
                .providerId(2L)
                .vehicleId(1L)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(3)
                .dailyRate(new BigDecimal("10000.00"))
                .totalCost(new BigDecimal("30000.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .pickupNotes("Arriving around 10:00 AM")
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCustomerId()).isEqualTo(10L);
        assertThat(response.getProviderId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(response.getBookingReference()).isEqualTo("RR-BK-20261101-ABC123");
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("30000.00"));

        verify(vehicleIntegrationService).updateVehicleStatus(eq(1L), eq("RENTED"), eq("Locked by Booking: RR-BK-20261101-ABC123"));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("testCreateBooking_InvalidDates_Returns400: Pass startDate > endDate, assert InvalidBookingDatesException")
    void testCreateBooking_InvalidDates_Returns400() {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 5))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(10L, request))
                .isInstanceOf(InvalidBookingDatesException.class)
                .hasMessageContaining("cannot be before start date");

        verify(bookingRepository, never()).save(any());
        verify(vehicleIntegrationService, never()).updateVehicleStatus(any(), any(), any());
    }

    @Test
    @DisplayName("testCreateBooking_VehicleAlreadyRented_Returns409: Mock vehicle status RENTED or MAINTENANCE")
    void testCreateBooking_VehicleAlreadyRented_Returns409() {
        VehicleResponseDto rentedVehicle = VehicleResponseDto.builder()
                .id(1L)
                .providerId(2L)
                .status("RENTED")
                .dailyRentalRate(new BigDecimal("10000.00"))
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 4))
                .build();

        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(rentedVehicle);

        assertThatThrownBy(() -> bookingService.createBooking(10L, request))
                .isInstanceOf(VehicleUnavailableException.class)
                .hasMessageContaining("not available for reservation");

        verify(bookingRepository, never()).save(any());
        verify(vehicleIntegrationService, never()).updateVehicleStatus(any(), any(), any());
    }

    @Test
    @DisplayName("testCreateBooking_OverlappingDates_Returns409: Overlapping date reservation exists, assert BookingConflictException")
    void testCreateBooking_OverlappingDates_Returns409() {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.of(2026, 11, 3))
                .endDate(LocalDate.of(2026, 11, 6))
                .build();

        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), anyList(), eq(request.getEndDate()), eq(request.getStartDate())
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(10L, request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("already has an active reservation overlapping");

        verify(bookingRepository, never()).save(any());
        verify(vehicleIntegrationService, never()).updateVehicleStatus(any(), any(), any());
    }
}

