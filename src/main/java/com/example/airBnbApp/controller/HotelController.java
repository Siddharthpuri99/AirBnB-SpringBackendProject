package com.example.airBnbApp.controller;

import com.example.airBnbApp.dto.BookingDto;
import com.example.airBnbApp.dto.BookingsTableResponseDto;
import com.example.airBnbApp.dto.HotelDto;
import com.example.airBnbApp.dto.HotelReportDto;
import com.example.airBnbApp.entity.Hotel;
import com.example.airBnbApp.exception.ResourceNotFoundException;
import com.example.airBnbApp.repository.HotelRepository;
import com.example.airBnbApp.service.BookingService;
import com.example.airBnbApp.service.HotelService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Slf4j
@RequestMapping(path = "/admin/hotels")
@RequiredArgsConstructor
public class HotelController {
    private final HotelRepository hotelRepository;

    private  final HotelService hotelService;
    private  final BookingService bookingService;

    @PostMapping
    public ResponseEntity<HotelDto> CreateNewHotel(@RequestBody HotelDto hotelDto){
        log.info("Atempting to create a new hotel with name:{} ", hotelDto.getName());
        HotelDto hotel = hotelService.createNewHotel(hotelDto);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long hotelId){
        HotelDto hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }
    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDto> updateHotelById(@PathVariable Long hotelId,@RequestBody HotelDto hotelDto){
        HotelDto hotel = hotelService.updateHotelById(hotelId,hotelDto);
        return ResponseEntity.ok(hotel);

    }
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId){
        hotelService.DeleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{hotelId}/activate")
    public ResponseEntity<Void> activateHotelById(@PathVariable Long hotelId){
        hotelService.activateHotel(hotelId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping
    public ResponseEntity<List<HotelDto>> getAllHotels(){
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/{hotelId}/bookings")
//    @Operation(summary = "Get all bookings of a hotel", tags = {"Admin Bookings"})
    public ResponseEntity<List<BookingsTableResponseDto>> getAllBookingsByHotelId(@PathVariable Long hotelId) {
        return ResponseEntity.ok(bookingService.getAllBookingsByHotelId(hotelId));
    }
    @GetMapping("/{hotelId}/reports")
    public ResponseEntity<HotelReportDto> getHotelReport(@PathVariable Long hotelId,
                                                         @RequestParam(required = false) LocalDate startDate,
                                                         @RequestParam(required = false) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        return ResponseEntity.ok(bookingService.getHotelReport(hotelId, startDate, endDate));
    }

}
