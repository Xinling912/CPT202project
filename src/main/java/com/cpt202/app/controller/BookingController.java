package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookingController {
    @Autowired
    private BookingRepository bookingRepository;//用于操作数据库的实例

    @GetMapping("/getbookings")
    public String getBooking(){
        return "hello,hello";
    }



}
