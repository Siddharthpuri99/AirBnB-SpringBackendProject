package com.example.airBnbApp.service;

import com.example.airBnbApp.dto.RoomDto;
import com.example.airBnbApp.entity.Room;

import java.util.List;

public interface RoomService {
    RoomDto createNewRoom(Long hotelId,RoomDto roomDto);
    List<RoomDto> getAllRoomsInHotel(Long hotelId);
    RoomDto getRoomById(Long roomId);
    void deleteRoomById(Long roomId);

    RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto);
}
