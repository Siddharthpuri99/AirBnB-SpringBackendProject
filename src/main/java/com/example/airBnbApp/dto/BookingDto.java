package com.example.airBnbApp.dto;

import com.example.airBnbApp.entity.*;
import com.example.airBnbApp.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    private Long id;
//    private Hotel hotel;
//    private Room room;
//    private User user;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private  LocalDate checkoutDate;
    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;
    private BookingStatus bookingStatus;
    private Set<Guest> guests;
    private BigDecimal amount;
}
