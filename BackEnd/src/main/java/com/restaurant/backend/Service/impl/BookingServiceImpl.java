package com.restaurant.backend.Service.impl;

import com.restaurant.backend.Entity.Booking;
import com.restaurant.backend.Entity.RestaurantTable;
import com.restaurant.backend.Repository.BookingRepository;
import com.restaurant.backend.Repository.RestaurantTableRepository;
import com.restaurant.backend.Service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final com.restaurant.backend.Service.OrderService orderService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              RestaurantTableRepository restaurantTableRepository,
                              @org.springframework.context.annotation.Lazy com.restaurant.backend.Service.OrderService orderService) {
        this.bookingRepository = bookingRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithRelationships();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    @Override
    public Booking createBooking(Booking booking) {
        // Kiểm tra tính khả dụng của bàn
        if (booking.getTable() != null) {
            validateTableAvailability(booking.getTable().getId(), booking.getDate(), booking.getTime());
        }

        if (booking.getStatus() == null) {
            booking.setStatus("PENDING");
        }

        return bookingRepository.save(booking);
    }

    @Override
    public Booking updateBooking(Long id, Booking booking) {
        Booking existingBooking = findById(id);
        existingBooking.setDate(booking.getDate());
        existingBooking.setTime(booking.getTime());
        existingBooking.setGuests(booking.getGuests());
        existingBooking.setNote(booking.getNote());
        existingBooking.setStatus(booking.getStatus());

        // Kiểm tra bàn mới nếu có thay đổi
        if (booking.getTable() != null &&
            !booking.getTable().getId().equals(existingBooking.getTable().getId())) {
            validateTableAvailability(booking.getTable().getId(), booking.getDate(), booking.getTime());
            existingBooking.setTable(booking.getTable());
        }

        return bookingRepository.save(existingBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(long id) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingByCustomerId(long customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        return bookings.isEmpty() ? null : bookings.get(bookings.size() - 1);
    }

    @Override
    public Booking assignTable(Long id, Long tableId) {
        Booking booking = findById(id);
        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));

        validateTableAvailability(tableId, booking.getDate(), booking.getTime());

        booking.setTable(table);
        return bookingRepository.save(booking);
    }

    @Override
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    @Override
    public Booking updateStatus(Long id, String status) {
        try {
            Booking booking = findById(id);
            if (booking == null) {
                throw new RuntimeException("Booking not found with id: " + id);
            }

            // Khi xác nhận booking (approve), kiểm tra tính khả dụng của bàn trước
            if ("CONFIRMED".equals(status) && booking.getTable() != null) {
                validateTableAvailability(booking.getTable().getId(), booking.getDate(), booking.getTime());
                booking.getTable().setStatus("PENDING_CHECKIN");
                restaurantTableRepository.save(booking.getTable());
            }

            // Khi hủy booking, reset trạng thái bàn nếu nó đang là PENDING_CHECKIN
            if ("CANCELED".equals(status) && booking.getTable() != null && "PENDING_CHECKIN".equals(booking.getTable().getStatus())) {
                booking.getTable().setStatus("AVAILABLE");
                restaurantTableRepository.save(booking.getTable());
            }

            booking.setStatus(status);
            return bookingRepository.save(booking);
        } catch (Exception e) {
            System.err.println("Error updating booking status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update booking status: " + e.getMessage());
        }
    }

    @Override
    public Booking checkInBooking(Long id) {
        Booking booking = findById(id);
        booking.setStatus("COMPLETED");

        // Khi check-in, bàn chuyển thành OCCUPIED (khách hàng có mặt và có thể gọi món)
        if (booking.getTable() != null) {
            RestaurantTable table = booking.getTable();
            table.setStatus("OCCUPIED");
            restaurantTableRepository.save(table);

            // Tự động tạo đơn hàng ban đầu cho bàn khi khách check-in
            if (booking.getCustomer() != null) {
                orderService.getOrCreateActiveOrder(table.getId(), booking.getCustomer().getId());
            }
        }

        return bookingRepository.save(booking);
    }

    // Các method bổ sung cho hệ thống cải tiến

    /**
     * Tìm bàn khả dụng cho ngày và giờ cụ thể
     */
    @Transactional(readOnly = true)
    public List<RestaurantTable> findAvailableTables(LocalDate date, LocalTime time, int guests) {
        List<RestaurantTable> allTables = restaurantTableRepository.findAll();

        return allTables.stream()
                .filter(table -> isTableAvailable(table.getId(), date, time))
                .filter(table -> table.getCapacity() >= guests)
                .collect(Collectors.toList());
    }

    /**
     * Đề xuất bàn phù hợp dựa trên số lượng khách
     */
    @Transactional(readOnly = true)
    public List<RestaurantTable> suggestTables(int guests) {
        return restaurantTableRepository.findAll().stream()
                .filter(table -> "AVAILABLE".equals(table.getStatus()))
                .filter(table -> table.getCapacity() >= guests)
                .sorted((t1, t2) -> Integer.compare(t1.getCapacity(), t2.getCapacity())) // Prefer smaller suitable tables
                .collect(Collectors.toList());
    }

    /**
     * Check if a table is available at specific date and time
     */
    @Transactional(readOnly = true)
    public boolean isTableAvailable(Long tableId, LocalDate date, LocalTime time) {
        // Assume 2-hour booking duration
        LocalTime endTime = time.plusHours(2);

        List<Booking> existingBookings = bookingRepository.findBookingsOnDate(tableId, date);

        // Check for overlapping bookings
        for (Booking existing : existingBookings) {
            LocalTime existingEndTime = existing.getTime().plusHours(2);
            // Check if time ranges overlap: startA < endB AND endA > startB
            if (time.isBefore(existingEndTime) && endTime.isAfter(existing.getTime())) {
                return false; // Conflict found
            }
        }

        return true; // No conflicts
    }

    /**
     * Validate table availability before booking
     */
    private void validateTableAvailability(Long tableId, LocalDate date, LocalTime time) {
        if (!isTableAvailable(tableId, date, time)) {
            throw new RuntimeException("Table is not available at the requested date and time");
        }
    }

    /**
     * Get booking by booking code (for check-in)
     */
    @Transactional(readOnly = true)
    public Booking getBookingByCode(String bookingCode) {
        // Assuming we add a bookingCode field to Booking entity
        // For now, return null - this would need database changes
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByTable(Long tableId) {
        return bookingRepository.findByTable_Id(tableId);
    }
}
