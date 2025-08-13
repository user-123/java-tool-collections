package org.dc.javatools.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Deprecated(forRemoval = true, since = "2025.08.14 03:38:13")
public class GsonUtil {
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (src, typeOfSrc, context) -> LocalDateTime.parse(src.getAsString(), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (src, typeOfSrc, context) -> LocalDate.parse(src.getAsString(), DateTimeFormatter.ofPattern("yyyy/MM/dd")))
                .create();
    }

    private GsonUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * obj to json
     * @param object Object
     * @return jsonStr String
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * jsonStr to javaBean
     * @param json String
     * @param clazz Class<T>
     * @return javaBean
     */
    public static <T> T toJavaBean(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * jsonStr to Map   (注意!!使用泛型會遇到型別擦除問題)
     * @param json String
     * @return Map<String, T>
     */
    public static Map<String, Object> toMap(String json) {
        return toMap(json, new TypeToken<Map<String, Object>>(){}.getType());
    }

    /**
     * Object to Map   (注意!!使用泛型會遇到型別擦除問題)
     * @param object Object
     * @return Map Map
     */
    public static Map<String, Object> objToMap(Object object) {
        return toMap(gson.toJson(object));
    }





    public static <Po, K, V> Map<K, V> poToMap(Po po, Class<K> keyClass, Class<V> valueClass) {
        return toMap(toJson(po), keyClass, valueClass);
    }

    /**
     * json string to po list
     * @param json string
     * @param poClass
     * @return List&lt;Po&gt;
     */
    public static <Po> List<Po> toList(String json, Class<Po> poClass) {
        return toList(json, TypeToken.getParameterized(List.class, poClass).getType());
    }

    /**
     * json string to map
     * @param json
     * @param keyClass
     * @param valueClass
     * @return Map&lt;K, V&gt;
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyClass, Class<V> valueClass) {
        return toMap(json, TypeToken.getParameterized(Map.class, keyClass, valueClass).getType());
    }

    public static <Po> List<Po> toList(String json, Type listType) {
        return toObject(json, listType);
    }

    public static <K, V> Map<K, V> toMap(String json, Type mapType) {
        return toObject(json, mapType);
    }

    public static <T> T toObject(String json, Type type) {
        return gson.fromJson(json, type);
    }

}
