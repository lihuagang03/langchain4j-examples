package dev.langchain4j.example;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.booking.Booking;
import dev.langchain4j.example.booking.BookingService;
import org.springframework.stereotype.Component;

/**
 * 机票预订工具
 */
@Component
public class BookingTools {

    /**
     * 机票预订服务
     */
    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 获取预订详情
     * @param bookingNumber 预订号
     * @param customerName 客户姓名
     * @param customerSurname 客户姓
     * @return 机票预订信息
     */
    // 工具注解
    @Tool
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return bookingService.getBookingDetails(bookingNumber, customerName, customerSurname);
    }

    /**
     * 取消预订
     * @param bookingNumber 预订号
     * @param customerName 客户姓名
     * @param customerSurname 客户姓
     */
    @Tool
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        bookingService.cancelBooking(bookingNumber, customerName, customerSurname);
    }
}
