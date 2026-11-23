package com.example.airBnbApp.dto;

import com.example.airBnbApp.entity.Hotel;
import com.example.airBnbApp.strategy.PricingStrategy;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceDto {
    private Hotel hotel;
    private Double price;
}

