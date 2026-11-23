package com.example.airBnbApp.dto;

import com.example.airBnbApp.entity.Hotel;
import com.example.airBnbApp.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HotellInfoDto {
    private HotelDto hotel;
    private List<RoomDto> rooms;
}
