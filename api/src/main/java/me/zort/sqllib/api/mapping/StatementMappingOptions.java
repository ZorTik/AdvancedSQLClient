package me.zort.sqllib.api.mapping;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StatementMappingOptions {

  @Nullable
  String getTable();

  class Builder {
    // Default values
    private String table = null;

    public @NotNull Builder table(final @Nullable String table) {
      this.table = table;
      return this;
    }

    public @NotNull StatementMappingOptions build() {
      return new LocalOptionsImplementation(table);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  class LocalOptionsImplementation implements StatementMappingOptions {
    private String table;
  }

}
