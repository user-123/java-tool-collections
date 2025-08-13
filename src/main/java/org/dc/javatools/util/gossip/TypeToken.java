package org.dc.javatools.util.gossip;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

@Getter
//@EqualsAndHashCode
public class TypeToken<T> {
    private final Type type;

    //建構函數，捕捉泛型類型
    protected TypeToken() {
        this.type = capture();
    }

    private TypeToken(Type type) {
        this.type = type;
    }

    /**
     * 建立一個 TypeToken 的實例(用於靜態方法中)
     */
    public static <T> TypeToken<T> of(Class<T> type) {
        return new TypeToken<>(type);
    }

    /**
     * 捕獲當前類別的泛型類型
     */
    private Type capture() {
        //取得目前類別的直接超類別(即泛型父類別)
        Type superclass = getClass().getGenericSuperclass();
        //檢查是否為帶有參數的泛型類型
        if (superclass instanceof ParameterizedType) {
            return ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }
        throw new IllegalArgumentException("無法擷取泛型類型，確保子類別正確繼承TypeToken");
    }

    //取得泛型的原始型別(class object)
    public Class<?> getRawType() {
        return getRawType(this.type);    //return getRawType(this.getType());
    }

    private Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof TypeVariable) {    //對於泛型變量，請嘗試解析其上界
            Type bound = ((TypeVariable<?>) type).getBounds()[0];
            return getRawType(bound);
        }
        throw new IllegalStateException("未知类型: " + type);
    }

    /**
     * 判斷當前類型是否為另一個類型的子類型
     */
    public boolean isAssignableFrom(TypeToken<?> other) {
        Objects.requireNonNull(other, "other不能為null");
        return isAssignableFrom(other.getType(), this.type);
    }

    private boolean isAssignableFrom(Type from, Type to) {
        if (to instanceof Class<?>) {
            return ((Class<?>) to).isAssignableFrom(getRawType(from));
        }
        if (to instanceof ParameterizedType parameterizedTo) {
            //檢查參數化類型的相容性
            if (from instanceof ParameterizedType parameterizedFrom) {
                //原始型別必須相容
                if (!((Class<?>) parameterizedTo.getRawType()).isAssignableFrom((Class<?>) parameterizedFrom.getRawType())) {
                    return false;
                }
                //泛型參數也需要匹配(這裡只是簡化匹配，實際上可以遞歸深入檢查)
                Type[] fromArgs = parameterizedFrom.getActualTypeArguments();
                Type[] toArgs = parameterizedTo.getActualTypeArguments();
                for (int i = 0; i < toArgs.length; i++) {
                    if (!toArgs[i].equals(fromArgs[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "TypeToken<%s>".formatted(this.type);
    }

}
