package dev.langchain4j.example.booking;

import java.time.LocalDate;

/**
 * 机票预订信息
 * @param bookingNumber 预订号
 * @param bookingBeginDate 预订开始日期
 * @param bookingEndDate 预订结束日期
 * @param customer 客户信息
 */
public record Booking(
        String bookingNumber,
        LocalDate bookingBeginDate,
        LocalDate bookingEndDate,
        Customer customer) {
}
