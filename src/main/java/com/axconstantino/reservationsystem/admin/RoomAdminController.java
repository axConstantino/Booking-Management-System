package com.axconstantino.reservationsystem.admin;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomUpdateDTO;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("admin/rooms")
@RestController
@RequiredArgsConstructor
@Validated
public class RoomAdminController {
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<Page<RoomDTO>> getRooms(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RoomDTO> rooms = roomService.getAll(pageable);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO newRoom) {
        RoomDTO savedRoom = roomService.create(newRoom);
        return ResponseEntity.ok(savedRoom);
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomUpdateDTO updateRequest) {
        RoomDTO updatedRoom = roomService.updateRoom(roomId, updateRequest);
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        roomService.delete(roomId);
        return ResponseEntity.noContent().build();
    }
}
