package me.zort.sqllib.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PrimaryKey {

    private final String column;
    private final String value;

}
