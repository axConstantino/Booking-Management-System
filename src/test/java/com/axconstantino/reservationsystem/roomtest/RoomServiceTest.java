package com.axconstantino.reservationsystem.roomtest;

import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.dto.RoomUpdateDTO;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @MockitoBean
    private Cloudinary cloudinary;

    @Mock
    Uploader uploader;

    @Test
    void testGetAvailableRooms() {
        RoomFilterRequest request = new RoomFilterRequest();
        request.setStartDate(LocalDate.of(2024, 1, 10));
        request.setEndDate(LocalDate.of(2024, 1, 12));
        request.setCapacity(2);
        request.setSortBy("pricePerNight");
        request.setOrder("asc");
        request.setPage(0);
        request.setSize(10);

        Page<RoomDTO> result = roomService.getAvailableRooms(request);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isGreaterThan(0);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Standard Room");
    }

    @Test
    void testUpdateRoom() {
        Room existing = roomRepository.findAll().stream().findFirst().orElseThrow();
        RoomUpdateDTO updateDTO = new RoomUpdateDTO();
        updateDTO.setDescription("Updated description");
        updateDTO.setPricePerNight(149.99);

        RoomDTO updated = roomService.updateRoom(existing.getId(), updateDTO);

        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getPricePerNight()).isEqualTo(149.99);
    }

    @Test
    @Transactional
    void testUploadImages() throws Exception {
        Room room = roomRepository.findAll().get(0);
        Long roomId = room.getId();

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "http://cloudinary.com/image.jpg");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(uploadResult);

        roomService.uploadImages(roomId, new MultipartFile[]{mockFile});

        Room updatedRoom = roomRepository.findById(roomId).orElseThrow();
        assertFalse(updatedRoom.getImages().isEmpty());
        assertTrue(updatedRoom.getImages().stream()
                .anyMatch(img -> img.getImageUrl().equals("http://cloudinary.com/image.jpg")));
    }
}

