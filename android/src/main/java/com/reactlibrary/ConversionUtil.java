/**
 *  ConversionUtil.java
 *  react-native-eval
 *
 *  Created by Andy Prock on 9/24/15.
 */

package com.reactlibrary;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Helper utilities to convert from react-native types to POJOs
 */
/* package */ final class ConversionUtil {
    /**
     * toObject extracts a value from a {@link ReadableMap} by its key,
     * and returns a POJO representing that object.
     *
     * @param readableMap The Map to containing the value to be converted
     * @param key The key for the value to be converted
     * @return The converted POJO
     */
    public static Object toObject(@Nullable ReadableMap readableMap, String key) {
        if (readableMap == null) {
            return null;
        }

        Object result;

        ReadableType readableType = readableMap.getType(key);
        switch (readableType) {
            case Null:
                result = null;
                break;
            case Boolean:
                result = readableMap.getBoolean(key);
                break;
            case Number:
                // Can be int or double.
                double tmp = readableMap.getDouble(key);
                if (tmp == (int) tmp) {
                    result = (int) tmp;
                } else {
                    result = tmp;
                }
                break;
            case String:
                result = readableMap.getString(key);
                break;
            case Map:
                result = toMap(readableMap.getMap(key));
                break;
            case Array:
                result = toList(readableMap.getArray(key));
                break;
            default:
                throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
        }

        return result;
    }

    /**
     * toMap converts a {@link ReadableMap} into a HashMap.
     *
     * @param readableMap The ReadableMap to be conveted.
     * @return A HashMap containing the data that was in the ReadableMap.
     */
    public static Map<String, Object> toMap(@Nullable ReadableMap readableMap) {
        if (readableMap == null) {
            return null;
        }

        com.facebook.react.bridge.ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        if (!iterator.hasNextKey()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            result.put(key, toObject(readableMap, key));
        }

        return result;
    }

    /**
     * toList converts a {@link ReadableArray} into an ArrayList.
     *
     * @param readableArray The ReadableArray to be conveted.
     * @return An ArrayList containing the data that was in the ReadableArray.
     */
    public static List<Object> toList(@Nullable ReadableArray readableArray) {
        if (readableArray == null) {
            return null;
        }

        List<Object> result = new ArrayList<>(readableArray.size());
        for (int index = 0; index < readableArray.size(); index++) {
            ReadableType readableType = readableArray.getType(index);
            switch (readableType) {
                case Null:
                    result.add(String.valueOf(index));
                    break;
                case Boolean:
                    result.add(readableArray.getBoolean(index));
                    break;
                case Number:
                    // Can be int or double.
                    double tmp = readableArray.getDouble(index);
                    if (tmp == (int) tmp) {
                        result.add((int) tmp);
                    } else {
                        result.add(tmp);
                    }
                    break;
                case String:
                    result.add(readableArray.getString(index));
                    break;
                case Map:
                    result.add(toMap(readableArray.getMap(index)));
                    break;
                case Array:
                    result = toList(readableArray.getArray(index));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object with index: " + index + ".");
            }
        }

        return result;
    }

    public static WritableArray toWritableArray(Object[] array) {
        WritableArray writableArray = Arguments.createArray();

        for (int i = 0; i < array.length; i++) {
            Object value = array[i];

            if (value == null) {
                writableArray.pushNull();
            }
            if (value instanceof Boolean) {
                writableArray.pushBoolean((Boolean) value);
            }
            if (value instanceof Double) {
                writableArray.pushDouble((Double) value);
            }
            if (value instanceof Integer) {
                writableArray.pushInt((Integer) value);
            }
            if (value instanceof String) {
                writableArray.pushString((String) value);
            }
            if (value instanceof Map) {
                writableArray.pushMap(ConversionUtil.toWritableMap((Map<String, Object>) value));
            }
            if (value.getClass().isArray()) {
                writableArray.pushArray(ConversionUtil.toWritableArray((Object[]) value));
            }
            if (value instanceof List) {
                writableArray.pushArray(ConversionUtil.toWritableArray(((List) value).toArray()));
            }
        }

        return writableArray;
    }

    public static WritableMap toWritableMap(Map<String, Object> map) {
        WritableMap writableMap = Arguments.createMap();
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();
            Object value = pair.getValue();

            if (value == null) {
                writableMap.putNull((String) pair.getKey());
            } else if (value instanceof Boolean) {
                writableMap.putBoolean((String) pair.getKey(), (Boolean) value);
            } else if (value instanceof Double) {
                writableMap.putDouble((String) pair.getKey(), (Double) value);
            } else if (value instanceof Integer) {
                writableMap.putInt((String) pair.getKey(), (Integer) value);
            } else if (value instanceof String) {
                writableMap.putString((String) pair.getKey(), (String) value);
            } else if (value instanceof Map) {
                writableMap.putMap((String) pair.getKey(), ConversionUtil.toWritableMap((Map<String, Object>) value));
            } else if (value.getClass() != null && value.getClass().isArray()) {
                writableMap.putArray((String) pair.getKey(), ConversionUtil.toWritableArray((Object[]) value));
            } else if (value instanceof List) {
                writableMap.putArray((String) pair.getKey(), ConversionUtil.toWritableArray(((List) value).toArray()) );
            }
        }

        return writableMap;
    }
}
