package me.zort.sqllib.api.options;

/**
 * A naming strategy that converts field names to column names.
 *
 * @author ZorTik
 */
public interface NamingStrategy {

    String fieldNameToColumn(String str);

}
