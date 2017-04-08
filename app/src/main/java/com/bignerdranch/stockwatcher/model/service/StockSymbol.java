package com.bignerdranch.stockwatcher.model.service;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class StockSymbol {

    @SerializedName("Symbol")
    private String symbol;

    @SerializedName("Name")
    private String name;
}
