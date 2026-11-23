package com.example.airBnbApp.service;

import com.example.airBnbApp.dto.GuestDto;
import com.example.airBnbApp.entity.Guest;
import com.example.airBnbApp.entity.User;
import com.example.airBnbApp.repository.GuestRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

//import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestServiceImpl implements GuestService{
    private  final ModelMapper modelMapper;
    private  final GuestRepo guestRepository;
    @Override
    public List<GuestDto> getAllGuests() {
        User user = getCurrentUser();
        log.info("Fetching all guests of user with id: {}", user.getId());
        List<Guest> guests = guestRepository.findByUser(user);
        return guests.stream()
                .map(guest -> modelMapper.map(guest, GuestDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public GuestDto addNewGuest(GuestDto guestDto) {
        log.info("Adding new guest: {}", guestDto);
        User user = getCurrentUser();
        Guest guest = modelMapper.map(guestDto, Guest.class);
        guest.setUser(user);
        Guest savedGuest = guestRepository.save(guest);
        log.info("Guest added with ID: {}", savedGuest.getId());
        return modelMapper.map(savedGuest, GuestDto.class);
    }

    @Override
    public void updateGuest(Long guestId, GuestDto guestDto) {
        log.info("Updating guest with ID: {}", guestId);
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new EntityNotFoundException("Guest not found"));

        User user = getCurrentUser();
        if (!Objects.equals(user.getId(), guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the owner of this guest");
        }

        modelMapper.map(guestDto, guest);
        guest.setUser(user);
        guest.setId(guestId);

        guestRepository.save(guest);
        log.info("Guest with ID: {} updated successfully", guestId);
    }

    @Override
    public void deleteGuest(Long guestId) {
        log.info("Deleting guest with ID: {}", guestId);
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new EntityNotFoundException("Guest not found"));

        User user = getCurrentUser();
        if (!Objects.equals(user.getId(), guest.getUser().getId())) {
            throw new AccessDeniedException("You are not the owner of this guest");
        }

        guestRepository.deleteById(guestId);
        log.info("Guest with ID: {} deleted successfully", guestId);
    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
