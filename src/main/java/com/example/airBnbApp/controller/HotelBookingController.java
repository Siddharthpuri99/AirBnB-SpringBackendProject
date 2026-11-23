package com.example.airBnbApp.controller;

import com.example.airBnbApp.dto.*;
import com.example.airBnbApp.service.BookingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Book;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
@Slf4j
public class HotelBookingController {
    private final BookingService bookingService;
    @PostMapping("/init")
    public ResponseEntity<BookingDto>initializeBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(bookingService.initializeBooking(bookingRequest));
    }
    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId, @RequestBody List<GuestDto> guestDtoList){
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guestDtoList));
    }
    @PostMapping("/{bookingId}/payments")
//    @Operation(summary = "Initiate payments flow for the booking", tags = {"Booking Flow"})
    public ResponseEntity<BookingPaymentInitResponseDto> initiatePayment(@PathVariable Long bookingId) {
        String sessionUrl = bookingService.initiatePayments(bookingId);
        return ResponseEntity.ok(new BookingPaymentInitResponseDto(sessionUrl));
    }
    @PostMapping("/{bookingId}/cancel")
//    @Operation(summary = "Cancel the booking", tags = {"Booking Flow"})
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{bookingId}/status")
    public ResponseEntity<Map<String,String>> getBookingStatus(@PathVariable Long bookingId) {
      return ResponseEntity.ok(Map.of("status",bookingService.getBookingStatus(bookingId)));
    }
    @GetMapping("/{bookingId}")
//    @Operation(summary = "Get the booking by Id", tags = {"Booking Flow"})
    public ResponseEntity<BookingDto> getBookingById(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }
    @PostMapping("/{bookingId}/removeGuests")
//    @Operation(summary = "Remove a guest Id from the booking", tags = {"Booking Flow"})
    public ResponseEntity<BookingDto> removeGuest(@PathVariable Long bookingId,
                                                  @RequestBody List<Long> guestIdList) {
        return ResponseEntity.ok(bookingService.removeGuestFromBooking(bookingId, guestIdList));
    }




}
