package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import room_service.TimeSlot;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для RoomService")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AvailabilityEngineClient availability;

    @Mock
    private RedisService redis;

    @InjectMocks
    private RoomService roomService;

    private UUID roomId;
    private UUID buildingId;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        testRoom = Room.builder()
                .id(roomId)
                .name("101")
                .buildingId(buildingId)
                .capacity(30)
                .floor(1)
                .features("{\"projector\": true, \"whiteboard\": true}")
                .build();
    }

    // --- GET BY ID ---

    @Test
    @DisplayName("Получение комнаты по ID: Успех (из кэша)")
    void getById_ShouldReturnFromCache_WhenCached() {
        when(redis.get(anyString(), eq(Room.class))).thenReturn(testRoom);

        Room result = roomService.getById(roomId);

        assertThat(result).isEqualTo(testRoom);
        verify(redis).get(anyString(), eq(Room.class));
        verify(roomRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Получение комнаты по ID: Успех (из БД)")
    void getById_ShouldReturnFromDatabase_WhenNotCached() {
        when(redis.get(anyString(), eq(Room.class))).thenReturn(null);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));

        Room result = roomService.getById(roomId);

        assertThat(result).isEqualTo(testRoom);
        verify(roomRepository).findById(roomId);
        verify(redis).set(anyString(), eq(testRoom), any());
    }

    @Test
    @DisplayName("Получение комнаты по ID: Ошибка NotFound")
    void getById_ShouldThrowNotFound_WhenRoomDoesNotExist() {
        when(redis.get(anyString(), eq(Room.class))).thenReturn(null);
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getById(roomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Room not found");
    }

    // --- SEARCH ---

    @Test
    @DisplayName("Поиск комнат: Успех (без фильтра доступности)")
    void search_ShouldReturnRooms_WithoutAvailabilityFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> roomPage = new PageImpl<>(List.of(testRoom), pageable, 1);
        
        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(roomPage);

        Page<Room> result = roomService.search(buildingId, 20, 50, null, null, "SPBSTU", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testRoom);
        verify(roomRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Поиск комнат: Успех (с фильтром доступности)")
    void search_ShouldReturnAvailableRooms_WithAvailabilityFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> roomPage = new PageImpl<>(List.of(testRoom), pageable, 1);
        
        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(roomPage);
        
        TimeSlot slot = TimeSlot.newBuilder()
                .setStartTime("10:00")
                .setEndTime("11:00")
                .build();
        when(availability.computeIntervals(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(slot));

        Page<Room> result = roomService.search(
                buildingId, 20, 50, null, null, "SPBSTU", 
                "2026-04-27T10:00:00Z", "2026-04-27T11:00:00Z", pageable
        );

        assertThat(result.getContent()).hasSize(1);
        verify(availability).computeIntervals(anyString(), eq("2026-04-27"), eq("10:00"), eq("11:00"));
    }

    @Test
    @DisplayName("Поиск комнат: Маппинг университета (SPBSTU -> spbptu)")
    void search_ShouldMapUniversityCode_FromFrontendToDatabase() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> roomPage = new PageImpl<>(List.of(testRoom), pageable, 1);
        
        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(roomPage);

        roomService.search(null, null, null, null, null, "SPBSTU", null, null, pageable);

        // Verify that the specification was called (university mapping happens inside)
        verify(roomRepository).findAll(any(Specification.class), eq(pageable));
    }

    // --- GET AVAILABILITY ---

    @Test
    @DisplayName("Получение доступности: Успех")
    void getAvailability_ShouldReturnTimeSlots() {
        TimeSlot slot1 = TimeSlot.newBuilder()
                .setStartTime("09:00")
                .setEndTime("10:00")
                .build();
        TimeSlot slot2 = TimeSlot.newBuilder()
                .setStartTime("14:00")
                .setEndTime("15:00")
                .build();
        
        when(availability.computeIntervals(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(slot1, slot2));

        var result = roomService.getAvailability(roomId, "2026-04-27", 60);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartTime()).isEqualTo("09:00");
        assertThat(result.get(1).getStartTime()).isEqualTo("14:00");
        verify(availability).computeIntervals(roomId.toString(), "2026-04-27", "09:00", "21:00");
    }

    @Test
    @DisplayName("Получение доступности: Пустой список")
    void getAvailability_ShouldReturnEmptyList_WhenNoSlotsAvailable() {
        when(availability.computeIntervals(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        var result = roomService.getAvailability(roomId, "2026-04-27", 60);

        assertThat(result).isEmpty();
    }

    // --- GET ROOM SCHEDULE ---

    @Test
    @DisplayName("Получение расписания комнаты: Успех")
    void getRoomSchedule_ShouldReturnScheduleWithOccupiedAndFreeSlots() {
        AvailabilityEngineClient.OccupiedSlot occupied = new AvailabilityEngineClient.OccupiedSlot(
                "booking_123", "10:00", "11:00", "user123", "Meeting"
        );
        
        when(availability.occupiedIntervals(anyString(), anyString()))
                .thenReturn(List.of(occupied));

        var result = roomService.getRoomSchedule(roomId, "2026-04-27");

        assertThat(result.getDate()).isEqualTo("2026-04-27");
        assertThat(result.getRoomId()).isEqualTo(roomId);
        assertThat(result.getSlots()).isNotEmpty();
        
        // Should have free slots before and after the occupied slot
        assertThat(result.getSlots().stream().anyMatch(s -> s.getStatus().equals("free"))).isTrue();
        assertThat(result.getSlots().stream().anyMatch(s -> s.getStatus().equals("occupied"))).isTrue();
    }

    @Test
    @DisplayName("Получение расписания комнаты: Различие между booking и schedule")
    void getRoomSchedule_ShouldDifferentiateBetweenBookingAndSchedule() {
        AvailabilityEngineClient.OccupiedSlot booking = new AvailabilityEngineClient.OccupiedSlot(
                "booking_123", "10:00", "11:00", "user123", "User Booking"
        );
        AvailabilityEngineClient.OccupiedSlot schedule = new AvailabilityEngineClient.OccupiedSlot(
                "import_456", "14:00", "15:00", "system", "Lecture"
        );
        
        when(availability.occupiedIntervals(anyString(), anyString()))
                .thenReturn(List.of(booking, schedule));

        var result = roomService.getRoomSchedule(roomId, "2026-04-27");

        var bookingSlot = result.getSlots().stream()
                .filter(s -> s.getFrom().equals("10:00"))
                .findFirst()
                .orElseThrow();
        var scheduleSlot = result.getSlots().stream()
                .filter(s -> s.getFrom().equals("14:00"))
                .findFirst()
                .orElseThrow();

        assertThat(bookingSlot.getType()).isEqualTo("booking");
        assertThat(scheduleSlot.getType()).isEqualTo("schedule");
    }
}
