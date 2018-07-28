package com.reactlibrary;

import com.couchbase.lite.AbstractQuery;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JsonQuery extends AbstractQuery {
    private Map<String, Object> jsonSchema;
    private Database database;

    JsonQuery(Map<String, Object> jsonSchema, Database database) {
        super();
        this.jsonSchema = jsonSchema;
        this.database = database;
    }

    public Database getDatabase() {
        return this.database;
    }

    protected Map<String, Object> _asJSON() {
        return this.jsonSchema;
    }

    protected Map<String, Integer> generateColumnNames() throws CouchbaseLiteException {
        Map<String, Integer> map = new HashMap<>();
        int count = ((List)this.jsonSchema.get("WHAT")).size();

        for (int i = 0; i < count; i++) {
            map.put(String.format("f%d", i), i);
        }

        return map;
    }
}
