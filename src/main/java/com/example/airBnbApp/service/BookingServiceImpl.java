package com.example.airBnbApp.service;
import com.example.airBnbApp.dto.*;
import com.example.airBnbApp.entity.*;
import com.example.airBnbApp.enums.BookingStatus;
import com.example.airBnbApp.exception.ResourceNotFoundException;
import com.example.airBnbApp.exception.UnAuthorisedException;
import com.example.airBnbApp.repository.*;
import com.example.airBnbApp.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private  final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final GuestRepo guestRepo;
    private final CheckoutService checkoutService;
    private final PricingService pricingService;
//    private  final GuestRepo guestRepo;



    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public BookingDto initializeBooking(BookingRequest bookingRequest) {
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId()).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with hotelid{}"+bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId()).orElseThrow(()->new ResourceNotFoundException("Room not found with roomid"+bookingRequest.getRoomId()));
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(),bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(),bookingRequest.getRoomsCount());

        long daysCount= ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate())+1;
        if(inventoryList.size()!=daysCount){
            throw new IllegalStateException("Room is not avaialble anymore");
        }
        inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));
//        for(Inventory inv:inventoryList){
//            inv.setReservedCount(inv.getReservedCount()+bookingRequest.getRoomsCount());
//        }
        inventoryRepository.saveAll(inventoryList);


        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkoutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .build();


        booking = bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }

//    @Override
//    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
//        Booking booking = bookingRepository.findById(bookingId).orElseThrow(()-> new ResourceNotFoundException("Booking not found with id:"+bookingId));
//        User user = getCurrentUser();
//
//        System.out.println("===== DEBUG =====");
//        System.out.println("Logged User ID = " + user.getId());
//        System.out.println("Booking User ID = " + booking.getUser().getId());
//        System.out.println("User Object = " + user);
//        System.out.println("Booking User Object = " + booking.getUser());
//        System.out.println("=================");
//
//        if (!user.getId().equals(booking.getUser().getId())) {
//            throw new UnAuthorisedException(
//                    "Booking does not belong to this user with id: " + user.getId()
//            );
//        }
//
//
//
//        if (!user.equals(booking.getUser())) {
//            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
//        }
//        if(hasBookingExpired(booking)){
//            throw new IllegalStateException("Booking has already expired");
//        }
//        if(booking.getBookingStatus()!=BookingStatus.RESERVED){
//            throw  new IllegalStateException("BOOKING IS NOT UNDER RESERVED STATE,CANNOT ADD GUESTS");
//        }
//
//        for(GuestDto guestDto:guestDtoList){
//            Guest guest = modelMapper.map(guestDto,Guest.class);
//            guest.setUser(user);
//            guest = guestRepo.save(guest);
//            booking.getGuests().add(guest);
//
//        }
//        booking.setBookingStatus(BookingStatus.GUEST_ADDED);
//        booking = bookingRepository.save(booking);
//        return modelMapper.map(booking,BookingDto.class);
//    }
@Override
public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Booking not found with id: " + bookingId));

    User user = getCurrentUser();

    // Debug (optional - remove in production)
    System.out.println("===== DEBUG =====");
    System.out.println("Logged User ID = " + user.getId());
    System.out.println("Booking User ID = " + (booking.getUser() != null ? booking.getUser().getId() : null));
    System.out.println("=================");

    // Ownership check (ONLY ONE CHECK)
    if (booking.getUser() == null ||
            !user.getId().equals(booking.getUser().getId())) {

        throw new UnAuthorisedException(
                "Booking does not belong to this user with id: " + user.getId()
        );
    }

    // Expiry check
    if (hasBookingExpired(booking)) {
        throw new IllegalStateException("Booking has already expired");
    }

    // Status check
    if (booking.getBookingStatus() != BookingStatus.RESERVED) {
        throw new IllegalStateException(
                "BOOKING IS NOT UNDER RESERVED STATE, CANNOT ADD GUESTS"
        );
    }

    // Add guests
    for (GuestDto guestDto : guestDtoList) {
        Guest guest = modelMapper.map(guestDto, Guest.class);
        guest.setUser(user);
        guest = guestRepo.save(guest);
        booking.getGuests().add(guest);
    }

    booking.setBookingStatus(BookingStatus.GUEST_ADDED);
    booking = bookingRepository.save(booking);

    return modelMapper.map(booking, BookingDto.class);
}

//    @Override
//    public String initiatePayments(Long bookingId) {
//        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
//                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
//        );
//        User user = getCurrentUser();
//        if (!user.equals(booking.getUser())) {
//            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
//        }
//        if (hasBookingExpired(booking)) {
//            throw new IllegalStateException("Booking has already expired");
//        }
//
//        String sessionUrl = checkoutService.getCheckoutSession(booking,
//                frontendUrl+"/payments/success" +bookingId +"/status",
//                frontendUrl+"/payments/failure" +bookingId +"/status");
//
//        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
//        bookingRepository.save(booking);
//
//        return sessionUrl;
//    }
@Override
@Transactional
public String initiatePayments(Long bookingId) {

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Booking not found with id: " + bookingId));

    User user = getCurrentUser();

    // Debug (optional)
    System.out.println("Logged User ID = " + user.getId());
    System.out.println("Booking User ID = " + (booking.getUser() != null ? booking.getUser().getId() : null));

    // Ownership check (CORRECT WAY)
    if (booking.getUser() == null ||
            !user.getId().equals(booking.getUser().getId())) {

        throw new UnAuthorisedException(
                "Booking does not belong to this user with id: " + user.getId()
        );
    }

    // Expiry check
    if (hasBookingExpired(booking)) {
        throw new IllegalStateException("Booking has already expired");
    }

    if (frontendUrl == null || frontendUrl.isBlank()) {
        throw new RuntimeException("frontend.url is not configured properly");
    }

    String successUrl = frontendUrl + "/payments/success/" + bookingId;
    String failureUrl = frontendUrl + "/payments/failure/" + bookingId;

    String sessionUrl = checkoutService.getCheckoutSession(
            booking,
            successUrl,
            failureUrl
    );

    // Update status
    booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
    bookingRepository.save(booking);

    return sessionUrl;
}
//@Override
//@Transactional
//public void capturePayment(Event event) {
//
//    log.info("Event Type = {}", event.getType());
//
//    if (!"checkout.session.completed".equals(event.getType())) {
//        log.warn("Unhandled event type: {}", event.getType());
//        return;
//    }
//
//    try {
//
//        // 🔥 FORCE SAFE JSON extraction
//        String rawJson = event.getDataObjectDeserializer()
//                .getRawJson();
//
//
//        com.fasterxml.jackson.databind.ObjectMapper mapper =
//                new com.fasterxml.jackson.databind.ObjectMapper();
//
//        com.fasterxml.jackson.databind.JsonNode node =
//                mapper.readTree(rawJson);
//
//        String sessionId = node.get("id").asText();
//
//        log.info("Extracted Session ID = {}", sessionId);
//
//        Booking booking = bookingRepository
//                .findByPaymentSessionId(sessionId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Booking not found for session ID: " + sessionId));
//
//        booking.setBookingStatus(BookingStatus.CONFIRMED);
//        bookingRepository.save(booking);
//
//        inventoryRepository.confirmBooking(
//                booking.getRoom().getId(),
//                booking.getCheckInDate(),
//                booking.getCheckoutDate(),
//                booking.getRoomsCount()
//        );
//
//        log.info("Booking CONFIRMED successfully: {}", booking.getId());
//
//    } catch (Exception ex) {
//        log.error("Failed processing checkout.session.completed", ex);
//        throw new RuntimeException(ex);
//    }
//}
//@Override
//@Transactional
//public void capturePayment(Event event) {
//    if ("checkout.session.completed".equals(event.getType())) {
//        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
//        if (session == null) return;
//
//        String sessionId = session.getId();
//        Booking booking =
//                bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
//                        new ResourceNotFoundException("Booking not found for session ID: "+sessionId));
//
//        booking.setBookingStatus(BookingStatus.CONFIRMED);
//        bookingRepository.save(booking);
//
//        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
//                booking.getCheckoutDate(), booking.getRoomsCount());
//
//        inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
//                booking.getCheckoutDate(), booking.getRoomsCount());
//
//        log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
//    } else {
//        log.warn("Unhandled event type: {}", event.getType());
//    }
//}
@Override
@Transactional
public void capturePayment(Event event) {
    log.info("Received Stripe Webhook event: {}", event.getType());

    if ("checkout.session.completed".equals(event.getType())) {
        try {
            // Get the helper deserializer directly from the event wrapper
            com.stripe.model.EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

            com.stripe.model.checkout.Session session = null;

            // 1. Try to get it cleanly
            if (deserializer.getObject().isPresent()) {
                session = (com.stripe.model.checkout.Session) deserializer.getObject().get();
            } else {
                log.warn("API version mismatch detected. Forcing unsafe deserialization fallback.");
                // 2. Fallback: Use deserializeUnsafe() which doesn't need EventData or GSON at all
                session = (com.stripe.model.checkout.Session) deserializer.deserializeUnsafe();
            }

            if (session == null) {
                log.error("Critical: Deserializer completely failed to unpack the Stripe Session.");
                return;
            }

            String sessionId = session.getId();
            log.info("Successfully extracted Session ID: {}", sessionId);

            Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found for session ID: " + sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking ID {} updated status to CONFIRMED.", booking.getId());

            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckoutDate(), booking.getRoomsCount());

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckoutDate(), booking.getRoomsCount());

            log.info("Successfully locked inventory and confirmed Booking ID: {}", booking.getId());

        } catch (Exception e) {
            log.error("Exception thrown processing checkout.session.completed payload", e);
            System.out.println(e);
        }
    } else {
        log.warn("Unhandled event type: {}", event.getType());
    }
}
    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId));

        User user = getCurrentUser();

        // DEBUG LOGS
        System.out.println("===== CANCEL BOOKING DEBUG =====");
        System.out.println("Authenticated User ID = " + user.getId());

        if (booking.getUser() != null) {
            System.out.println("Booking Owner ID = " + booking.getUser().getId());
        } else {
            System.out.println("Booking Owner = NULL");
        }

        System.out.println("Booking Status = " + booking.getBookingStatus());
        System.out.println("================================");

        // Ownership Check
        if (booking.getUser() == null ||
                !user.getId().equals(booking.getUser().getId())) {

            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: "
                            + user.getId());
        }

        // Status Check
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed bookings can be cancelled");
        }

        // Cancel Booking
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release Inventory
        inventoryRepository.findAndLockReservedInventory(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckoutDate(),
                booking.getRoomsCount()
        );

        inventoryRepository.cancelBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckoutDate(),
                booking.getRoomsCount()
        );

        // Refund Payment
        try {

            Session session = Session.retrieve(
                    booking.getPaymentSessionId());

            RefundCreateParams refundParams =
                    RefundCreateParams.builder()
                            .setPaymentIntent(session.getPaymentIntent())
                            .build();

            Refund refund = Refund.create(refundParams);

            System.out.println("Refund Created: " + refund.getId());

        } catch (StripeException e) {
            throw new RuntimeException(
                    "Failed to process refund", e);
        }
    }

    @Override
    public String getBookingStatus(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));

        User user = getCurrentUser();

        if (booking.getUser() == null ||
                !user.getId().equals(booking.getUser().getId())) {

            throw new UnAuthorisedException(
                    "Booking does not belong to this user with id: " + user.getId()
            );
        }

        return booking.getBookingStatus().name();
    }
    @Override
    @Transactional
    public BookingDto removeGuestFromBooking(Long bookingId, List<Long> guestIdList) {

        log.info("Removing guests with ids: {} from booking with id: {}", guestIdList, bookingId);

        // Fetch the booking or throw an exception if not found
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // Get the current user and verify ownership
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: " + user.getId());
        }

        // Check if the booking has expired
        if (hasBookingExpired(booking)) {
            throw new IllegalStateException("Booking has already expired");
        }

        // Validate and remove each guest
        for (Long guestId : guestIdList) {
            Guest guest = guestRepo.findById(guestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
            if (!booking.getGuests().remove(guest)) {
                throw new IllegalStateException("Guest with id: " + guestId + " is not associated with this booking");
            }
        }

        // Save the updated booking
        booking = bookingRepository.save(booking);

        // Map and return the updated booking as a DTO
        return modelMapper.map(booking, BookingDto.class);
    }




    @Override
    public List<BookingsTableResponseDto> getAllBookingsByHotelId(Long hotelId) {
//        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
//                "found with ID: "+hotelId));
//        User user = getCurrentUser();
//
//        log.info("Getting all booking for the hotel with ID: {}", hotelId);
//
////        if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);
//        if (!Objects.equals(user.getId(), hotel.getOwner().getId())) {
//            throw new AccessDeniedException(
//                    "You are not the owner of hotel with id: " + hotelId
//            );
//        }
//
//        List<Booking> bookings = bookingRepository.findByHotel(hotel);
//
//        return bookings.stream()
//                .map((element) -> modelMapper.map(element, BookingDto.class))
//                .collect(Collectors.toList());
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                "found with ID: "+hotelId));
        User user = getCurrentUser();

        log.info("Getting all booking for the hotel with ID: {}", hotelId);

        if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);

        List<BookingStatus> statuses = Arrays.asList(
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED,
                BookingStatus.PAYMENT_PENDING
        );
        List<Booking> bookings = bookingRepository.findByHotelAndBookingStatusInOrderByCreatedAtDesc(hotel, statuses);

        return bookings.stream()
                .map((element) -> {
                    var modified =  modelMapper.map(element, BookingsTableResponseDto.class);
                    modified.setRoomType(element.getRoom().getType());
                    return modified;
                })
                .collect(Collectors.toList());

    }

    @Override
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {


            Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                    "found with ID: "+hotelId));
            User user = getCurrentUser();

            log.info("Generating report for hotel with ID: {}", hotelId);

//            if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);
        if (!user.getId().equals(hotel.getOwner().getId())) {
            throw new AccessDeniedException(
                    "You are not the owner of hotel with id: " + hotelId
            );
        }

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

            Long totalConfirmedBookings = bookings
                    .stream()
                    .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                    .count();

            BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                    .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                    .map(Booking::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgRevenue = totalConfirmedBookings == 0 ? BigDecimal.ZERO :
                    totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);

            return new HotelReportDto(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
        }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = getCurrentUser();

        return bookingRepository.findByUser(user)
                .stream().
                map((element) -> {
                    return modelMapper.map(element, BookingDto.class);
                })
                .collect(Collectors.toList());
    }

    @Override
    public BookingDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        return modelMapper.map(booking, BookingDto.class);
    }



    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
