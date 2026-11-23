package com.example.airBnbApp.service;

import com.example.airBnbApp.dto.RoomDto;
import com.example.airBnbApp.entity.Hotel;
import com.example.airBnbApp.entity.Room;
import com.example.airBnbApp.entity.User;
import com.example.airBnbApp.exception.ResourceNotFoundException;
import com.example.airBnbApp.exception.UnAuthorisedException;
import com.example.airBnbApp.repository.HotelRepository;
import com.example.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
//    private final RoomService roomService;
    private  final RoomRepository roomRepository;
    private  final ModelMapper modelMapper;
    private  final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final PricingUpdateService pricingUpdateService;

    @Override
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating a room in hotel with hotelID:{}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID"+hotelId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

//        if(!user.equals(hotel.getOwner())) {
//            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
//        }
        if (!user.getId().equals(hotel.getOwner().getId())) {
            throw new UnAuthorisedException(
                    "This user does not own this hotel with id: " + hotelId
            );
        }
       Room room = modelMapper.map(roomDto,Room.class);
       room.setHotel(hotel);
      room =  roomRepository.save(room);

//      if(hotel.getActive()){
//          inventoryService.intializeRoomForAYear(room);
//      }
        inventoryService.intializeRoomForAYear(room);
        pricingUpdateService.updateHotelPrices(hotel);

      return modelMapper.map(room,RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in a  hotel with hotelID:{}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID"+hotelId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
        }
        return hotel.getRooms()
                .stream()
                .map((element)->modelMapper.map(element,RoomDto.class)).collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting a rooms with  roomID:{}",roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID"+roomId));
        return modelMapper.map(room,RoomDto.class);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        log.info("deleting a rooms with  roomID:{}",roomId);
//        boolean roomexist = roomRepository.existsById(roomId);
//        if(!roomexist){
//           throw  new ResourceNotFoundException("room not found with ID"+roomId);
//        }
        Room room = roomRepository.findById(roomId).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID"+roomId));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+roomId);
        }

        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);
    }

    @Override
    @Transactional
    public RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto) {
        log.info("Updating the room with ID: {}", roomId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: "+hotelId));

        User user = getCurrentUser();
//        if(!user.equals(hotel.getOwner())) {
//            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
//        }
        if (!user.getId().equals(hotel.getOwner().getId())) {
            throw new UnAuthorisedException(
                    "This user does not own this hotel with id: " + hotelId
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: "+roomId));

        modelMapper.map(roomDto, room);
        room.setId(roomId);

//        TODO: if price or inventory is updated, then update the inventory for this room
        room = roomRepository.save(room);

        return modelMapper.map(room, RoomDto.class);
    }
    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
