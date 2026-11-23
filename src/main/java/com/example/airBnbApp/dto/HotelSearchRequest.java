package com.example.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class HotelSearchRequest {
    private String city;
    private LocalDate start;
    private LocalDate end;
    private  Integer roomsCount;
    private  Integer page=0;
    private  Integer size=10;


}
