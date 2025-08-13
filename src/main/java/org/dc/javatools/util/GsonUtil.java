package org.dc.javatools.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * jsonStr to Map
     * @param json String
     * @return Map<String, T>
     */
    public static <T> Map<String, T> toMap(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, T>>(){}.getType());
    }

    /**
     * Object to Map
     * @param object Object
     * @return Map Map
     */
    public static <T> Map<String, T> objToMap(Object object) {
        return gson.fromJson(gson.toJson(object), new TypeToken<Map<String, T>>(){}.getType());
    }

}
