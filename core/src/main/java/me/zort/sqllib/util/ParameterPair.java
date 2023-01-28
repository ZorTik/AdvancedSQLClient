package me.zort.sqllib.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Parameter;

@RequiredArgsConstructor
@Getter
public class ParameterPair {

    private final Parameter parameter;
    private final Object value;

}
