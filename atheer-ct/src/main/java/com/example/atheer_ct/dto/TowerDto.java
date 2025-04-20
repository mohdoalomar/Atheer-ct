package com.example.atheer_ct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TowerDto {
    private Long id;

    private String tawalId;
    private String siteName;
    private double latitude;
    private double longitude;
    private int totalHeight;
    private String power;
    private String clutter;
}
