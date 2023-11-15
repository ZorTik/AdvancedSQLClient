package me.zort.sqllib.mapping;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.mapping.MappingProxyInstance;
import me.zort.sqllib.api.mapping.StatementMappingFactory;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.api.mapping.StatementMappingRegistry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MappingProvider {

    private final transient StatementMappingRegistry mappingRegistry;
    private transient Supplier<SQLConnection> connectionFactory = null;
    private transient StatementMappingFactory mappingFactory;

    public MappingProvider() {
        this.mappingRegistry = new MappingRegistryImpl();
        this.mappingFactory = new DefaultStatementMappingFactory();
    }

    /**
     * Constructs a mapping proxy based on provided interface.
     * The interface should follow rules for creating mapping repositories
     * in this library.
     *
     * @param mappingInterface Interface to create mapping repository for.
     * @param <T>              Type of mapping repository.
     * @return Mapping repository.
     * @see SQLDatabaseConnection#createProxy(Class, StatementMappingOptions)
     */
    public final <T> T createProxy(Class<T> mappingInterface) {
        return createProxy(mappingInterface, new StatementMappingOptions.Builder().build());
    }

    /**
     * Constructs a mapping repository based on provided interface.
     * The interface should follow rules for creating mapping repositories
     * in this library.
     * <p>
     * Example:
     * <pre>
     *     &#64;Table("users")
     *     public interface MyRepository {
     *          &#64;Select("*")
     *          &#64;Where(&#64;Where.Condition(column = "firstname", value = "{First Name}"))
     *          &#64;Limit(1)
     *          Optional&lt;User&gt; getUser(&#64;Placeholder("First Name") String firstName);
     *
     *          &#64;Select
     *          List&lt;User&gt; getUsers();
     *
     *          &#64;Delete
     *          QueryResult deleteUsers();
     *     }
     *
     *     SQLDatabaseConnection connection = ...;
     *     MyRepository repository = connection.createGate(MyRepository.class);
     *
     *     Optional&lt;User&gt; user = repository.getUser("John");
     * </pre>
     *
     * @param mappingInterface Interface to create mapping repository for.
     * @param <T>              Type of mapping repository.
     * @return Mapping repository.
     */
    @SuppressWarnings("unchecked")
    public final <T> T createProxy(final @NotNull Class<T> mappingInterface, final @NotNull StatementMappingOptions options) {
        Objects.requireNonNull(mappingInterface, "Mapping interface cannot be null!");
        Objects.requireNonNull(options, "Options cannot be null!");

        AtomicReference<MappingProxyInstance<T>> instanceReference = new AtomicReference<>();
        T rawInstance = (T) Proxy.newProxyInstance(mappingInterface.getClassLoader(),
                new Class[]{mappingInterface}, (proxy, method, args) -> instanceReference.get().invoke(proxy, method, args));
        instanceReference.set(new ProxyInstanceImpl<>(mappingInterface,
                options,
                mappingFactory.strategy(mappingInterface, connectionFactory),
                mappingFactory.resultAdapter()));

        MappingProxyInstance<T> proxyInstanceWrapper = instanceReference.get();
        mappingRegistry.registerProxy(proxyInstanceWrapper);
        return rawInstance;
    }

    /**
     * Sets a mapping to use when using {@link SQLDatabaseConnection#createProxy(Class, StatementMappingOptions)}.
     *
     * @param mappingFactory Mapping factory to use.
     */
    public void setProxyMapping(final @NotNull StatementMappingFactory mappingFactory) {
        this.mappingFactory = Objects.requireNonNull(mappingFactory, "Mapping factory cannot be null!");
    }

    public void setMConnectionFactory(Supplier<SQLConnection> connectionFactory) {
        if (this.connectionFactory != null) {
            throw new IllegalStateException("Connection factory is already set!");
        }
        this.connectionFactory = connectionFactory;
    }

    @ApiStatus.Experimental
    public StatementMappingRegistry getMappingRegistry() {
        return mappingRegistry;
    }

}
