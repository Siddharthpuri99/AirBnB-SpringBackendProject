package com.example.airBnbApp.repository;

import com.example.airBnbApp.dto.GuestDto;
import com.example.airBnbApp.entity.Guest;
import com.example.airBnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepo extends JpaRepository<Guest,Long> {
    List<Guest> findByUser(User user);
}
