package com.example.airBnbApp.service;


import com.example.airBnbApp.dto.*;
import com.example.airBnbApp.entity.Inventory;
import com.example.airBnbApp.entity.Room;
import com.example.airBnbApp.entity.User;
import com.example.airBnbApp.exception.ResourceNotFoundException;
import com.example.airBnbApp.repository.HotelMinPricingRepository;
import com.example.airBnbApp.repository.InventoryRepository;
import com.example.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private  final ModelMapper modelMapper;
    private final HotelMinPricingRepository hotelMinPricingRepository;
    private  final RoomRepository roomRepository;

    @Override
    public void intializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate enedDate = today.plusYears(1);
        for(;!today.isAfter(enedDate);today=today.plusDays(1)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
    }

    @Override
    public void deleteAllInventories(Room room) {
//        LocalDate today = LocalDate.now();
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceResponseDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        log.info("Searching hotels for {} city, from {} to {}", hotelSearchRequest.getCity(), hotelSearchRequest.getStart(), hotelSearchRequest.getEnd());
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount =
                ChronoUnit.DAYS.between(hotelSearchRequest.getStart(), hotelSearchRequest.getEnd()) + 1;


        //bussiness logic - 90 days
        Page<HotelPriceDto> hotelPage = hotelMinPricingRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                hotelSearchRequest.getStart(), hotelSearchRequest.getEnd(), hotelSearchRequest.getRoomsCount(),
                dateCount, pageable);

        return hotelPage.map(hotelPriceDto -> {
            HotelPriceResponseDto hotelPriceResponseDto = modelMapper.map(hotelPriceDto.getHotel(), HotelPriceResponseDto.class);
            hotelPriceResponseDto.setPrice(hotelPriceDto.getPrice());
            return hotelPriceResponseDto;
        });

    }

    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting All inventory by room for room with id: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: "+roomId));

        User user = getCurrentUser();
//      if(!user.equals(room.getHotel().getOwner())) throw new AccessDeniedException("You are not the owner of room with id: "+roomId);
        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException(
                    "You are not the owner of room with id: " + roomId
            );
        }

        return inventoryRepository.findByRoomOrderByDate(room).stream()
                .map((element) -> modelMapper.map(element,
                        InventoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("Updating All inventory by room for room with id: {} between date range: {} - {}", roomId,
                updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: "+roomId));

        User user = getCurrentUser();
//        if(!user.equals(room.getHotel().getOwner())) throw new AccessDeniedException("You are not the owner of room with id: "+roomId);
        if (!Objects.equals(user.getId(), room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException(
                    "You are not the owner of room with id: " + roomId
            );
        }

        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId, updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate());

        inventoryRepository.updateInventory(roomId, updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate(), updateInventoryRequestDto.getClosed(),
                updateInventoryRequestDto.getSurgeFactor());
    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
