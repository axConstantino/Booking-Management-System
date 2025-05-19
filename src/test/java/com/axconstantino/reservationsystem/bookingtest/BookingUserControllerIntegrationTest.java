package com.axconstantino.reservationsystem.bookingtest;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import com.axconstantino.reservationsystem.booking.database.repository.BookingRepository;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingUserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;

    private User testUser;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("user@test.com")
                .orElseThrow(() -> new IllegalStateException("User not found"));

        testRoom = roomRepository.findByName("Standard Room")
                .orElseThrow(() -> new IllegalStateException("Room not found"));

        bookingRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createBooking_ValidRequest_ReturnsCreated() throws Exception {
        BookingRequest request = new BookingRequest(
                testRoom.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)
        );

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(testRoom.getId().toString()))
                .andExpect(jsonPath("$.startDate", startsWith(LocalDate.now().plusDays(1).toString())))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createBooking_InvalidDates_ReturnsBadRequest() throws Exception {
        BookingRequest request = new BookingRequest(
                testRoom.getId(),
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateBooking_ValidUpdate_ReturnsUpdatedBooking() throws Exception {
        Booking existingBooking = createTestBooking(BookingStatus.PENDING);

        BookingRequest updateRequest = new BookingRequest(
                testRoom.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7)
        );

        mockMvc.perform(put("/bookings/" + existingBooking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate", startsWith(LocalDate.now().plusDays(5).toString())));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void cancelBooking_ValidRequest_ReturnsNoContent() throws Exception {
        Booking booking = createTestBooking(BookingStatus.CONFIRMED);

        mockMvc.perform(delete("/bookings/" + booking.getId()))
                .andExpect(status().isNoContent());
    }

    private Booking createTestBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setUser(testUser);
        booking.setRoom(testRoom);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setTotalPrice(BigDecimal.valueOf(199.98));
        booking.setBookingStatus(status);
        return bookingRepository.save(booking);
    }

    private static String asJsonString(final Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}