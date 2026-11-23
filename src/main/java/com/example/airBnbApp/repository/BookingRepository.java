package com.example.airBnbApp.repository;

import com.example.airBnbApp.dto.BookingDto;
import com.example.airBnbApp.entity.Booking;
import com.example.airBnbApp.entity.Hotel;
import com.example.airBnbApp.entity.User;
import com.example.airBnbApp.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Booking> findByUser(User user);

    List<Booking> findByHotelAndBookingStatusInOrderByCreatedAtDesc(Hotel hotel, List<BookingStatus> statuses);
}
