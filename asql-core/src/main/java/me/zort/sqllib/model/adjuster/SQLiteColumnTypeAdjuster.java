package me.zort.sqllib.model.adjuster;

import me.zort.sqllib.model.SQLColumnTypeAdjuster;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLiteColumnTypeAdjuster implements SQLColumnTypeAdjuster {
  private static final Pattern sizePattern = Pattern.compile("(.+)\\(\\d+\\)");

  @Override
  public String adjust(final String type) {
    final String subject = type.split(" ")[0];
    final String suffix = subject.equals(type)
            ? ""
            : " " + String.join(" ", subarray(type.split(" ")));
    final Matcher matcher = sizePattern.matcher(subject);
    return (matcher.matches() ? matcher.group(1) : subject) + suffix;
  }

  private static String[] subarray(String[] array) {
    return Arrays.copyOfRange(array, 1, array.length);
  }
}
