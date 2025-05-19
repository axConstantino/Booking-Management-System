package com.axconstantino.reservationsystem.roomtest;

import com.axconstantino.reservationsystem.rooms.controller.RoomController;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    private final RoomDTO testRoom = RoomTestUtils.createTestRoomDTO(1L);
    private final Page<RoomDTO> testPage = new PageImpl<>(Collections.singletonList(testRoom));

    @Test
    void getAllRooms_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").descending());
        when(roomService.getAll(any(Pageable.class))).thenReturn(testPage);

        ResponseEntity<Page<RoomDTO>> response = roomController.getAllRooms(pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
        verify(roomService, times(1)).getAll(pageable);
    }

    @Test
    void getRoomById_shouldReturnRoomWhenExists() {
        when(roomService.get(1L)).thenReturn(Optional.of(testRoom));

        ResponseEntity<RoomDTO> response = roomController.getRoomById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testRoom, response.getBody());
    }

    @Test
    void getRoomById_shouldReturnNotFoundWhenMissing() {
        when(roomService.get(999L)).thenReturn(Optional.empty());

        ResponseEntity<RoomDTO> response = roomController.getRoomById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void searchAvailableRooms_shouldReturnFilteredResults() throws MethodArgumentNotValidException {
        RoomFilterRequest validFilter = RoomTestUtils.createValidFilterRequest();
        when(roomService.getAvailableRooms(any(RoomFilterRequest.class))).thenReturn(testPage);

        ResponseEntity<Page<RoomDTO>> response = roomController.searchAvailableRooms(validFilter);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
        verify(roomService, times(1)).getAvailableRooms(validFilter);
    }

}