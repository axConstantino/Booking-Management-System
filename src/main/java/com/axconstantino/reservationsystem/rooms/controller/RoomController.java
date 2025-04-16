package com.axconstantino.reservationsystem.rooms.controller;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final RoomService service;

    @GetMapping
    public ResponseEntity<Page<RoomDTO>> getAllRooms(Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("{roomId}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long roomId) {
        return service.get(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<Page<RoomDTO>> searchAvailableRooms(@Valid @ModelAttribute RoomFilterRequest filterRequest) {
        return ResponseEntity.ok(service.getAvailableRooms(filterRequest));
    }
}
