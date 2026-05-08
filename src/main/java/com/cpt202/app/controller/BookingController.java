package com.cpt202.app.controller;



import com.cpt202.app.service.BookingService;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;



    // Using constructor injection to ensure clear dependencies
    public BookingController(BookingService bookingService
    ) {
        this.bookingService = bookingService;


    }

    /**
     * [Place Order: Core Booking Logic]
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request, Principal principal) {
        try {
            // [Before modification] Booking newOrder = bookingService.createBooking(...);

            // [After modification]: The variable type must exactly match the return type of the Service
            BookingService.BookingResponse response = bookingService.createBooking(
                    principal.getName(),
                    request.specialistId(),
                    request.slotId(),
                    request.notes()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * [Cancel Order] Both users and specialists can operate
     */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestParam String reason, Principal principal) {
        try {
            bookingService.cancelBooking(orderId, reason, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("AUTHORIZED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * [Specialist Confirm Order]
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId, Principal principal) {
        try {
            bookingService.confirmOrder(orderId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order confirmed successfully"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * [Complete Order]
     */
    @PostMapping("/complete/{orderId}")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId, Principal principal) {
        try {
            bookingService.completeOrder(orderId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Order marked as completed"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * [Query: My booking history (Customer perspective)]

     */
    @GetMapping("/myOrders")
    public ResponseEntity<?> getMyOrders(Principal principal) {
        try {
            // Service has handled everything, the Controller only needs to "direct"
            List<BookingService.BookingResponse> response = bookingService.getOrdersByCustomerResponse(principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * [Query: Bookings received by specialist (Specialist perspective)]

     */
    @GetMapping("/specialist/my-bookings")
    public ResponseEntity<?> getSpecialistOrders(Principal principal) {
        try {
            // This line handles everything: business logic, validation, data conversion
            List<BookingService.BookingResponse> response = bookingService.getSpecialistOrders(principal.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // If there is insufficient permission, catch the exception here and return 403
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }


    // Unified exception handler
    private ResponseEntity<?> handleException(Exception e) {
        if (e instanceof IllegalStateException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    /**
     * DTO for frontend request
     */
    public record BookingRequest(
            Long specialistId,
            Long slotId,
            String notes
    ) {}
}