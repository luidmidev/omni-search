package io.github.luidmidev.omnisearch.jpa;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
class ReflectionUtils {
    static List<Field> getAllFields(Class<?> clazz) {
        // Obtiene campos de la clase actual
        var fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));

        // Obtiene campos de la clase padre recursivamente
        var superClass = clazz.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            fields.addAll(getAllFields(clazz.getSuperclass()));
            superClass = superClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Gets the wrapper type for a primitive class.
     *
     * @param primitiveType the primitive type class
     * @return the corresponding wrapper class
     */
    static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }
}
