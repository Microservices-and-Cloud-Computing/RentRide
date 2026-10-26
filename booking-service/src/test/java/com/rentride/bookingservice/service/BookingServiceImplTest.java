package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.exception.BookingConflictException;
import com.rentride.bookingservice.exception.BookingNotFoundException;
import com.rentride.bookingservice.exception.InvalidBookingDatesException;
import com.rentride.bookingservice.exception.UnauthorizedBookingAccessException;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleIntegrationService vehicleIntegrationService;

    @Mock
    private BookingReferenceGenerator bookingReferenceGenerator;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private VehicleResponseDto availableVehicle;
    private BookingCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        availableVehicle = VehicleResponseDto.builder()
                .id(1L)
                .providerId(10L)
                .make("Toyota")
                .model("Aqua")
                .dailyRentalRate(new BigDecimal("8000.00"))
                .status("AVAILABLE")
                .location("Colombo")
                .licensePlateNumber("WP-CBA-1234")
                .build();

        validRequest = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .pickupNotes("Airport pickup")
                .build();
    }

    @Test
    @DisplayName("Should successfully create booking and lock vehicle via Feign")
    void testCreateBooking_Success() {
        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), anyList(), eq(validRequest.getEndDate()), eq(validRequest.getStartDate())
        )).thenReturn(false);
        when(bookingReferenceGenerator.generate()).thenReturn("RR-BK-20261026-TEST01");

        Booking savedBooking = Booking.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .startDate(validRequest.getStartDate())
                .endDate(validRequest.getEndDate())
                .totalDays(3)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .pickupNotes("Airport pickup")
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(50L, validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getBookingReference()).isEqualTo("RR-BK-20261026-TEST01");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(response.getTotalDays()).isEqualTo(3);
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("24000.00"));
        assertThat(response.getVehicleDetails()).isNotNull();
        assertThat(response.getVehicleDetails().getMake()).isEqualTo("Toyota");

        verify(vehicleIntegrationService).updateVehicleStatus(eq(1L), eq("RENTED"), any());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw VehicleUnavailableException when vehicle is not AVAILABLE")
    void testCreateBooking_VehicleUnavailable_ThrowsException() {
        VehicleResponseDto rentedVehicle = VehicleResponseDto.builder()
                .id(1L)
                .status("RENTED")
                .dailyRentalRate(new BigDecimal("8000.00"))
                .build();

        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(rentedVehicle);

        assertThatThrownBy(() -> bookingService.createBooking(50L, validRequest))
                .isInstanceOf(VehicleUnavailableException.class)
                .hasMessageContaining("not available for reservation");

        verify(bookingRepository, never()).save(any());
        verify(vehicleIntegrationService, never()).updateVehicleStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw BookingConflictException when overlapping reservation exists")
    void testCreateBooking_BookingConflict_ThrowsException() {
        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), anyList(), eq(validRequest.getEndDate()), eq(validRequest.getStartDate())
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(50L, validRequest))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("already has an active reservation overlapping");

        verify(bookingRepository, never()).save(any());
        verify(vehicleIntegrationService, never()).updateVehicleStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw InvalidBookingDatesException when start date is in the past")
    void testCreateBooking_PastStartDate_ThrowsException() {
        BookingCreateRequest pastRequest = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(50L, pastRequest))
                .isInstanceOf(InvalidBookingDatesException.class)
                .hasMessageContaining("cannot be in the past");
    }

    @Test
    @DisplayName("Should throw InvalidBookingDatesException when duration exceeds 30 days")
    void testCreateBooking_DurationExceeds30Days_ThrowsException() {
        BookingCreateRequest longRequest = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(35))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(50L, longRequest))
                .isInstanceOf(InvalidBookingDatesException.class)
                .hasMessageContaining("exceeds maximum allowed limit of 30 days");
    }

    @Test
    @DisplayName("Should execute compensating rollback on vehicle-service if database save fails")
    void testCreateBooking_DatabaseFailure_ExecutesCompensatingRollback() {
        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), anyList(), eq(validRequest.getEndDate()), eq(validRequest.getStartDate())
        )).thenReturn(false);
        when(bookingReferenceGenerator.generate()).thenReturn("RR-BK-20261026-TEST01");
        when(bookingRepository.save(any(Booking.class))).thenThrow(new RuntimeException("DB Connection Dropped"));

        assertThatThrownBy(() -> bookingService.createBooking(50L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Connection Dropped");

        // First locked to RENTED
        verify(vehicleIntegrationService).updateVehicleStatus(eq(1L), eq("RENTED"), any());
        // Then compensated back to AVAILABLE
        verify(vehicleIntegrationService).updateVehicleStatus(eq(1L), eq("AVAILABLE"), any());
    }

    @Test
    @DisplayName("Should return booking by ID when requested by booking customer")
    void testGetBookingById_OwnerCustomer_Success() {
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("RR-BK-123")
                .customerId(50L)
                .providerId(20L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalDays(2)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("16000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(vehicleIntegrationService.getVehicleById(1L)).thenReturn(availableVehicle);

        BookingResponse response = bookingService.getBookingById(10L, 50L, "ROLE_CUSTOMER");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCustomerId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should throw UnauthorizedBookingAccessException when user is not owner and not admin")
    void testGetBookingById_UnauthorizedUser_ThrowsException() {
        Booking booking = Booking.builder()
                .id(10L)
                .customerId(50L)
                .providerId(20L)
                .vehicleId(1L)
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(10L, 999L, "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedBookingAccessException.class)
                .hasMessageContaining("do not have permission");
    }

    @Test
    @DisplayName("Should allow ADMIN role to access any booking")
    void testGetBookingById_AdminRole_Success() {
        Booking booking = Booking.builder()
                .id(10L)
                .customerId(50L)
                .providerId(20L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .totalDays(1)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("8000.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBookingById(10L, 999L, "ROLE_ADMIN");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException when booking ID does not exist")
    void testGetBookingById_NotFound_ThrowsException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(999L, 50L, "ROLE_CUSTOMER"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("Booking reservation not found with ID: 999");
    }

    @Test
    @DisplayName("Should return customer booking history")
    void testGetBookingsByCustomerId_Success() {
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("RR-BK-123")
                .customerId(50L)
                .providerId(20L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalDays(2)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("16000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsByCustomerId(50L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerId()).isEqualTo(50L);
    }
}

