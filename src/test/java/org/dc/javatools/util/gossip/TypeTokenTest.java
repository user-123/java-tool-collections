package org.dc.javatools.util.gossip;

import org.dc.javatools.util.validation.asserts.Assert;

import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TypeTokenTest {

    public static <T> void main(String[] args) {
        TypeToken<List<String>> listTypeToken = new TypeToken<>() {};
        System.out.println("Type: " + listTypeToken.getType());
        System.out.println("Raw Type: " + listTypeToken.getRawType());

        TypeToken<Map<String, Integer>> mapTypeToken = new TypeToken<>() {};
        System.out.println("Type: " + mapTypeToken.getType());
        System.out.println("Raw Type: " + mapTypeToken.getRawType());

        //判斷型別相容性
        TypeToken<List<String>> stringListType = new TypeToken<>() {};
        TypeToken<List<?>> wildcardListType = new TypeToken<>() {};
        System.out.println(stringListType.isAssignableFrom(wildcardListType));

        System.out.println("\n\n\n====================\n\n");

        TypeToken typeToken1 = TypeToken.of(String.class);
        TypeToken typeToken2 = TypeToken.of(int.class);
        TypeToken typeToken3 = TypeToken.of(Map.class);
        TypeToken typeToken4 = TypeToken.of(List.class);
        TypeToken typeToken5 = TypeToken.of(Stack.class);
        TypeToken typeToken6 = TypeToken.of(Assert.class);
        TypeToken typeToken7 = TypeToken.of(IllegalAccessException.class);
        TypeToken typeToken8 = TypeToken.of(AssertionError.class);
        TypeToken typeToken9 = new TypeToken<List<Integer>>() {};
        TypeToken<List<Integer>> typeToken10 = new TypeToken<List<Integer>>() {};
        TypeToken<List<Integer>> typeToken11 = new TypeToken<>() {};
        TypeToken<List> typeToken12 = new TypeToken<>() {};
        TypeToken<List<?>> typeToken13 = new TypeToken<>() {};
        TypeToken<List<Object>> typeToken14 = new TypeToken<>() {};
        TypeToken<List<T>> typeToken15 = new TypeToken<>() {};
        TypeToken<Void> typeToken16 = new TypeToken<>() {};
        TypeToken<T> typeToken17 = new TypeToken<>() {};
        TypeToken<TypeToken<TypeToken<T>>> typeToken18 = new TypeToken<>() {};
        TypeToken<TypeToken> typeToken19 = new TypeToken<>() {};
        TypeToken<TypeToken> typeToken20 = TypeToken.of(TypeToken.class);
        //TypeToken typeToken = new TypeToken() {};	//illegal

        TypeToken[] typeTokens = {typeToken1, typeToken2, typeToken3, typeToken4, typeToken5,
                typeToken6, typeToken7, typeToken8, typeToken9, typeToken10,
                typeToken11, typeToken12, typeToken13, typeToken14, typeToken15,
                typeToken16, typeToken17, typeToken18, typeToken19, typeToken20};

        for (TypeToken typeToken : typeTokens) {
            System.out.println("Instance: " + typeToken);
            System.out.println("Type: " + typeToken.getType());
            System.out.println("Raw Type: " + typeToken.getRawType());
        }
    }

}
