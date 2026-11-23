package com.example.airBnbApp.service;

import com.example.airBnbApp.dto.HotelDto;
import com.example.airBnbApp.dto.HotellInfoDto;
import com.example.airBnbApp.entity.Hotel;

import java.util.List;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);
    HotelDto getHotelById(Long id);
    HotelDto updateHotelById(Long id,HotelDto hotelDto);
    void DeleteHotelById(Long id);
    void activateHotel(Long id);

    HotellInfoDto getHotelInfoById(Long id);

    List<HotelDto> getAllHotels();
}
