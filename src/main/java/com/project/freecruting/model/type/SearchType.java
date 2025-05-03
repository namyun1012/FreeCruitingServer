package com.project.freecruting.model.type;

public enum SearchType {
    TITLE("title"),
    CONTENT("content"),
    AUTHOR("author"),
    ALL("all");

    private String value;

    SearchType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static SearchType fromString(String text) {
        for (SearchType type : SearchType.values()) {
            if (type.getValue().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + text);
    }
}
