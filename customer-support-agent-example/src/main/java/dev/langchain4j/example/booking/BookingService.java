package dev.langchain4j.example.booking;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 机票预订服务
 */
@Component
public class BookingService {

    /**
     * 客户信息
     */
    private static final Customer CUSTOMER = new Customer("John", "Doe");

    /**
     * 预订号
     */
    private static final String BOOKING_NUMBER = "MS-777";
    /**
     * 机票预订信息
     */
    private static final Booking BOOKING = new Booking(
            BOOKING_NUMBER,
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 12, 31),
            CUSTOMER
    );

    /**
     * 预订数据
     */
    private static final Map<String, Booking> BOOKINGS = new HashMap<>() {{
        put(BOOKING_NUMBER, BOOKING);
    }};

    /**
     * 获取预订详情
     * @param bookingNumber 预订号
     * @param customerName 客户姓名
     * @param customerSurname 客户姓氏
     * @return 机票预订信息
     */
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        this.ensureExists(bookingNumber, customerName, customerSurname);

        // Imitating DB lookup
        return BOOKINGS.get(bookingNumber);
    }

    /**
     * 取消预订
     * @param bookingNumber 预订号
     * @param customerName 客户姓名
     * @param customerSurname 客户姓氏
     */
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        this.ensureExists(bookingNumber, customerName, customerSurname);

        // Imitating booking cancellation
        BOOKINGS.remove(bookingNumber);
    }

    private void ensureExists(String bookingNumber, String customerName, String customerSurname) {
        // Imitating DB lookup

        // 预订号和客户信息，必须一致
        Booking booking = BOOKINGS.get(bookingNumber);
        if (booking == null) {
            throw new BookingNotFoundException(bookingNumber);
        }

        Customer customer = booking.customer();
        if (!customer.name().equals(customerName)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        if (!customer.surname().equals(customerSurname)) {
            throw new BookingNotFoundException(bookingNumber);
        }
    }
}
