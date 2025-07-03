package com.chicu.trader.trading.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class DoubleListConverter implements AttributeConverter<List<Double>, String> {

    @Override
    public String convertToDatabaseColumn(List<Double> attribute) {
        return attribute == null ? null :
                attribute.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public List<Double> convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? List.of() :
                Arrays.stream(dbData.split(","))
                        .map(String::trim)
                        .map(Double::parseDouble)
                        .toList();
    }
}
