package me.zort.sqllib.api.model;

public class TableSchema {

    private final String table;
    private final String[] definitions;

    public TableSchema(String table, String[] definitions) {
        this.table = table;
        this.definitions = definitions;
    }

    public String getDefinition(int index) {
        return definitions[index];
    }

    public String[] getDefinitions() {
        return definitions;
    }

    public String getTable() {
        return table;
    }

    public int size() {
        return definitions.length;
    }

}
