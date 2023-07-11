package me.zort.sqllib.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark whether a field in table mapping
 * entity represents nullable column.
 *
 * <pre>
 *     public class TableEntity {
 *          &#064;PrimaryKey(autoIncrement = true)
 *          private Integer id;
 *          &#064;NullableField(nullable = false)
 *          &#064;Id
 *          private String name;
 *     }
 *
 *     new SQLTableRepositoryBuilder<TableEntity, String>()
 *                 .withConnection(connection)
 *                 .withTableName(tableName)
 *                 .withTypeClass(TableEntity.class)
 *                 .build()
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NullableField {

  boolean nullable() default true;

}
