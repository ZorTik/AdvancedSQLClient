package me.zort.sqllib.mapping;

import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.mapping.StatementMappingResultAdapter;
import me.zort.sqllib.api.mapping.StatementMappingStrategy;
import me.zort.sqllib.api.mapping.StatementMappingFactory;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class DefaultStatementMappingFactory implements StatementMappingFactory {
  private final DefaultResultAdapter resultAdapter = new DefaultResultAdapter();

  @Override
  public <T> StatementMappingStrategy<T> strategy(Class<T> interfaceClass, Supplier<SQLConnection> connectionFactory) {
    if (!interfaceClass.isInterface() || !Modifier.isAbstract(interfaceClass.getModifiers()))
      throw new IllegalArgumentException("The given class is not an interface or is not abstract");
    return new DefaultStatementMapping<>(connectionFactory);
  }

  @Override
  public StatementMappingResultAdapter resultAdapter() {
    return resultAdapter;
  }
}
