package com.example.airBnbApp.controller;

import com.example.airBnbApp.dto.*;
import com.example.airBnbApp.repository.InventoryRepository;
import com.example.airBnbApp.service.HotelService;
import com.example.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/hotels")
public class HotelBrowseController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
//    @Operation(summary = "Search hotels", tags = {"Browse Hotels"})
    public ResponseEntity<Page<HotelPriceResponseDto>> searchHotels(
            @RequestParam String city,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam Integer roomsCount,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        HotelSearchRequest hotelSearchRequest = new HotelSearchRequest(city, startDate, endDate, roomsCount, page, size);
        var pageResult = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(pageResult);
    }
    @GetMapping("/{hotelId}/info")
   public  ResponseEntity<HotellInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }


}
