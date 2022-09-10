package me.zort.sqllib.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a field as a linked field.
 * Linked fields in a mapped class are automatically filled with
 * values obtained from another tables. This is simple
 * representation of one-to-one relationship.
 * <p>
 * Target mapping type needs to have {@link PrimaryKey} annotation
 * set! Otherwise, there is no way to detect id field.
 * <p>
 * Field annotated with this annotation is subject of blocking
 * fetch operation automatically performed while mapping object
 * from database.
 * <p>
 * Usage:
 * <p>
 * \ @LinkedOne(localColumn = "player_id", targetTable = "players")
 * \ private Player player;
 *
 * @author ZorTik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LinkedOne {

    /**
     * This represents name of current table column where is
     * stored id of the linked object in target table.
     * @return The column name.
     */
    String localColumn();

    /**
     * This represents name of the target table where is
     * stored the linked object.
     * @return The table name.
     */
    String targetTable();

}
