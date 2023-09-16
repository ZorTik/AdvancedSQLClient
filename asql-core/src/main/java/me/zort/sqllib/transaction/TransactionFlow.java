package me.zort.sqllib.transaction;

import lombok.*;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.util.Arrays;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionFlow {

  private final Transaction transaction;
  private final Step[] steps;
  private final Options options;
  private boolean executed = false;
  private int index = -1;

  public Result execute() {
    return executeNext(steps.length);
  }

  @SneakyThrows(SQLException.class)
  public Result executeNext(int count) {
    if (executed) throw new IllegalStateException("TransactionFlow already fully executed!");

    QueryResult[] results = new QueryResult[steps.length];
    int maxIndex = index + count;
    int lastIndex = index;
    for (int i = 0; i <= maxIndex; i++) {
      Step.Status status = steps[i].execute(transaction.getDatabaseConnection());
      if (status.equals(Step.Status.BREAK)) {
        if (options.rollbackOnFailure) {
          transaction.rollback();
          index = -1;
        }
        break;
      }
      results[i] = status.equals(Step.Status.SUCCESS) ? steps[i].getResult() : null;
      lastIndex = i;
    }
    boolean success = true;
    final int lastIndexFinal = lastIndex;
    while (lastIndex < results.length - 1) {
      success = false;
      results[++lastIndex] = null;
    }

    Result result = success
            ? new Result(true)
            : new Result(false,
            String.format("Transaction failed at step %d!", lastIndexFinal + 1));

    result.addAll(java.util.Arrays.asList(results));
    if (!result.isSuccessful()) result.setBrokenIndex(lastIndexFinal + 1);

    if (lastIndexFinal >= steps.length - 1) {
      executed = true;

      if (options.commitOnSuccess && result.isSuccessful())
        transaction.commit();

      if (options.autoClose) close();
    }
    if (result.isSuccessful()) index = lastIndexFinal;
    return result;
  }

  private void close() {
    transaction.close();
  }

  @SuppressWarnings("unused")
  public static final class Builder {
    private final Transaction transaction;
    private final Options options;
    private Step[] steps = new Step[0];

    public Builder(Transaction transaction) {
      this.transaction = transaction;
      this.options = new Options();
    }

    public @NotNull Builder step(final @NotNull QueryNode<?> node) {
      return step(node, false);
    }

    public @NotNull Builder step(final @NotNull QueryNode<?> node, boolean optional) {
      return step(new StepImpl(node, optional));
    }

    public @NotNull Builder step(final @NotNull TransactionFlow.Step step) {
      steps = Arrays.add(steps, step);
      return this;
    }

    public @NotNull Builder rollbackOnFailure(boolean rollbackOnFailure) {
      options.setRollbackOnFailure(rollbackOnFailure);
      return this;
    }

    public @NotNull Builder commitOnSuccess(boolean commitOnSuccess) {
      options.setCommitOnSuccess(commitOnSuccess);
      return this;
    }

    public @NotNull Builder autoClose(boolean autoClose) {
      options.setAutoClose(autoClose);
      return this;
    }

    public @NotNull TransactionFlow create() {
      return new TransactionFlow(transaction, steps, options);
    }
  }

  @NoArgsConstructor
  @Data
  public static final class Options {
    private boolean rollbackOnFailure = true;
    private boolean commitOnSuccess = true;
    private boolean autoClose = true;
  }

  public static final class Result extends QueryRowsResult<QueryResult> {
    @Setter(AccessLevel.PROTECTED)
    @Getter
    private int brokenIndex = -1;

    public Result(final boolean successful) {
      super(successful);
    }

    public Result(final boolean successful, String rejectMessage) {
      super(successful, rejectMessage);
    }

  }

  @RequiredArgsConstructor
  private static class StepImpl implements Step {
    private final Query query;
    @Getter
    private final boolean optional;
    @Getter
    private QueryResult result = null;

    @Override
    public Status execute(SQLDatabaseConnection connection) {
      if (!(query instanceof QueryNode)) throw new IllegalStateException("FlowStepImpl accepts only QueryNode!");

      QueryNode<?> node = (QueryNode<?>) query;
      node = node.getAncestor();
      QueryResult localResult = node.generatesResultSet()
              ? connection.query(node)
              : connection.exec(node);

      if (localResult.isSuccessful()) result = localResult;
      return localResult.isSuccessful() ? Status.SUCCESS : (optional ? Status.CONTINUE : Status.BREAK);
    }
  }

  public interface Step {

    Status execute(SQLDatabaseConnection connection);

    QueryResult getResult(); // Result if the #execute returned SUCCESS, otherwise null

    boolean isOptional();

    enum Status {
      SUCCESS, BREAK, CONTINUE
    }

  }
}
