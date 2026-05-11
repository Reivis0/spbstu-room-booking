package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.dto.ChainBookingRequest;
import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Управление бронированиями")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final com.github.MadyarovGleb.booking_mvp.service.ChainBookingSagaOrchestrator chainOrchestrator;

    public BookingController(BookingService bookingService, com.github.MadyarovGleb.booking_mvp.service.ChainBookingSagaOrchestrator chainOrchestrator) {
        this.bookingService = bookingService;
        this.chainOrchestrator = chainOrchestrator;
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (UUID) auth.getPrincipal() : null;
    }

    private String getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
        }
        return null;
    }

    @Operation(summary = "Создать новое бронирование")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Бронирование создано", 
                    content = @Content(schema = @Schema(implementation = Booking.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "409", description = "Конфликт времени (аудитория занята)")
    })
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateBookingRequest body) {
        var userId = getCurrentUserId();
        if (userId == null) {
            logger.warn("Booking creation rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        if (body != null && body.getRoomId() != null) {
            MDC.put("room_id", body.getRoomId().toString());
        }
        logger.info("Booking creation started");
        var booking = bookingService.createBooking(userId, body);
        MDC.put("booking_id", booking.getId().toString());
        logger.info("Booking creation completed successfully");
        return ResponseEntity.status(201).body(booking);
    }

    @Operation(summary = "Создать атомарную цепочку бронирований", 
               description = "Бронирует несколько слотов за один раз. Если хотя бы один занят — отменяются все.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Цепочка создана"),
            @ApiResponse(responseCode = "409", description = "Конфликт в одном или нескольких интервалах")
    })
    @PostMapping("/chain")
    public ResponseEntity<?> createChain(@RequestBody ChainBookingRequest body) {
        var userId = getCurrentUserId();
        if (userId == null) {
            logger.warn("Chain booking creation rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        logger.info("Atomic chain booking creation started items_count={}", body.getItems() != null ? body.getItems().size() : 0);
        
        List<CreateBookingRequest> requests = new ArrayList<>();
        for (ChainBookingRequest.ChainBookingItem item : body.getItems()) {
            requests.add(CreateBookingRequest.builder()
                    .roomId(item.getRoomId())
                    .startsAt(item.getStartsAt())
                    .endsAt(item.getEndsAt())
                    .title(body.getTitle() != null ? body.getTitle() : body.getPurpose())
                    .build());
        }
        
        try {
            List<Booking> bookings = chainOrchestrator.execute(userId, requests);
            logger.info("Chain booking creation completed successfully count={}", bookings.size());
            return ResponseEntity.status(201).body(bookings);
        } catch (com.github.MadyarovGleb.booking_mvp.exception.BookingConflictException e) {
            logger.warn("Chain booking failed due to conflict: {}", e.getMessage());
            return ResponseEntity.status(409).body(e.getConflicts());
        } catch (Exception e) {
            logger.error("Chain booking failed unexpectedly", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @Operation(summary = "Получить все бронирования (только для админа)")
    @GetMapping
    public ResponseEntity<List<Booking>> getAll() {
        var role = getCurrentUserRole();
        if (role == null || !role.equals("admin")) {
            logger.warn("Get all bookings rejected due to insufficient permissions");
            return ResponseEntity.status(403).build();
        }
        List<Booking> bookings = bookingService.listAll();
        logger.info("All bookings retrieved successfully count={}", bookings.size());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Получить мои бронирования")
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> my() {
        var userId = getCurrentUserId();
        if (userId == null) {
            logger.warn("My bookings request rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        List<Booking> bookings = bookingService.listByUser(userId);
        logger.info("My bookings retrieved successfully count={}", bookings.size());
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Получить детали бронирования по ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@Parameter(description = "UUID бронирования") @PathVariable UUID id) {
        MDC.put("booking_id", id.toString());
        var booking = bookingService.getById(id);
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId != null) {
            MDC.put("user_id", userId.toString());
        }
        if (!booking.getUserId().equals(userId) && (role == null || !role.equals("admin"))) {
            logger.warn("Booking read rejected due to insufficient permissions");
            throw new ForbiddenException("forbidden");
        }
        logger.info("Booking retrieved successfully");
        return ResponseEntity.ok(booking);
    }

    @Operation(summary = "Обновить бронирование")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "UUID бронирования") @PathVariable UUID id, 
            @RequestBody CreateBookingRequest body) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) {
            logger.warn("Booking update rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        MDC.put("booking_id", id.toString());
        if (body != null && body.getRoomId() != null) {
            MDC.put("room_id", body.getRoomId().toString());
        }
        logger.info("Booking update started");
        var booking = bookingService.updateBooking(userId, role, id, body);
        logger.info("Booking update completed successfully");
        return ResponseEntity.ok(booking);
    }

    @Operation(summary = "Отменить/удалить бронирование")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(
            @Parameter(description = "UUID бронирования") @PathVariable UUID id, 
            @Parameter(description = "Причина отмены") @RequestParam(required = false) String reason) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) {
            logger.warn("Booking cancellation rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        MDC.put("booking_id", id.toString());
        logger.info("Booking cancellation started");
        bookingService.cancel(id, userId, role, reason);
        logger.info("Booking cancellation completed successfully");
        return ResponseEntity.noContent().build();
    }
}
