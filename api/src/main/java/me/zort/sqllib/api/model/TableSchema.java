package me.zort.sqllib.api.model;

import me.zort.sqllib.util.Pair;

public class TableSchema {

    private final String table;
    private final ColumnDefinition[] definitions;

    public TableSchema(String table, ColumnDefinition[] definitions) {
        this.table = table;
        this.definitions = definitions;
    }

    public ColumnDefinition getDefinitionDetails(int index) {
        return definitions[index];
    }

    public String getDefinitionName(int index) {
        return getDefinitionDetails(index).getName();
    }

    public String getDefinitionType(int index) {
        return getDefinitionDetails(index).getType();
    }

    public String getDefinition(int index) {
        ColumnDefinition details = getDefinitionDetails(index);
        return details.getName() + " " + details.getType();
    }

    public String[] getDefinitions() {
        String[] definitions = new String[this.definitions.length];
        for (int i = 0; i < definitions.length; i++) {
            definitions[i] = getDefinition(i);
        }
        return definitions;
    }

    public String getTable() {
        return table;
    }

    public int size() {
        return definitions.length;
    }

}
