package com.example.airBnbApp.service;

import com.example.airBnbApp.dto.*;
import com.stripe.model.Event;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    BookingDto initializeBooking(BookingRequest bookingRequest);

    BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList);


    String initiatePayments(Long bookingId);

    void capturePayment(Event event);
    void cancelBooking(Long bookingId);

    String getBookingStatus(Long bookingId);

    List<BookingsTableResponseDto> getAllBookingsByHotelId(Long hotelId);

    HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate);

    List<BookingDto> getMyBookings();
    BookingDto getBookingById(Long bookingId);
    BookingDto removeGuestFromBooking(Long bookingId, List<Long> guestIdList);
//    List<BookingsTableResponseDto> getAllBookingsByHotelId(Long hotelId);

}
