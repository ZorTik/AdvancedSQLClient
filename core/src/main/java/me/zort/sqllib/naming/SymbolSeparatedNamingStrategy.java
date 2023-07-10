package me.zort.sqllib.naming;

import lombok.AllArgsConstructor;
import me.zort.sqllib.api.options.NamingStrategy;

/**
 * A naming strategy that converts field names to column names.
 * This naming strategy converts names as snake_case.
 *
 * @author ZorTik
 */
@AllArgsConstructor
public class SymbolSeparatedNamingStrategy implements NamingStrategy {

  private final char symbol;
  private final boolean upperCase;

  /**
   * Creates a naming strategy with provided symbol that
   * converts names to lower case.
   *
   * @param symbol The symbol to separate words with.
   */
  public SymbolSeparatedNamingStrategy(char symbol) {
    this(symbol, false);
  }

  @Override
  public String fieldNameToColumn(String str) {
    if (str.isEmpty()) return "";

    char[] chars = str.toCharArray();
    StringBuilder sb = new StringBuilder();
    int index = 0;
    for (char c : chars) {
      if (Character.isUpperCase(c)
              && index > 0
              && !Character.isUpperCase(chars[index - 1])
              && (index == chars.length - 1 || !Character.isUpperCase(chars[index + 1]))
      ) {
        sb.append(symbol);
      }

      sb.append(upperCase ? Character.toUpperCase(c) : Character.toLowerCase(c));
      index++;
    }
    return sb.toString();
  }

}
