package org.axostudio.axonpcs.protocol.lookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class PacketResolver {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private PacketResolver() {
    }

    public static Class<?> type(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public static Optional<Class<?>> optionalType(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }

    public static MethodHandle constructor(Class<?> clazz, Class<?>... parameterTypes) throws NoSuchMethodException, IllegalAccessException {
        Constructor<?> ctor = clazz.getDeclaredConstructor(parameterTypes);
        ctor.setAccessible(true);
        return LOOKUP.unreflectConstructor(ctor);
    }

    public static Optional<MethodHandle> optionalConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            return Optional.of(LOOKUP.unreflectConstructor(ctor));
        } catch (NoSuchMethodException | IllegalAccessException | LinkageError e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a zero-argument allocator for packet classes whose only public
     * constructor requires a live NMS entity. Packet-only NPCs do not have such
     * an entity, so their serialized id fields must be populated afterwards.
     */
    public static MethodHandle allocator(Class<?> clazz) throws ReflectiveOperationException {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return MethodHandles.insertArguments(LOOKUP.unreflect(allocateInstance).bindTo(unsafe), 0, clazz);
    }

    public static MethodHandle method(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... parameterTypes)
            throws NoSuchMethodException, IllegalAccessException {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return LOOKUP.unreflect(method);
    }

    public static Optional<MethodHandle> optionalMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) return Optional.empty();
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return Optional.of(LOOKUP.unreflect(method));
        } catch (NoSuchMethodException | IllegalAccessException | LinkageError e) {
            return Optional.empty();
        }
    }

    public static MethodHandle getter(Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return LOOKUP.unreflectGetter(field);
    }

    public static Optional<MethodHandle> optionalGetter(Class<?> clazz, String... fieldNames) {
        if (clazz == null) return Optional.empty();
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return Optional.of(LOOKUP.unreflectGetter(field));
            } catch (NoSuchFieldException | IllegalAccessException | LinkageError ignored) {
            }
        }
        return Optional.empty();
    }

    public static MethodHandle setter(Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return LOOKUP.unreflectSetter(field);
    }

    public static Optional<MethodHandle> optionalSetter(Class<?> clazz, String... fieldNames) {
        if (clazz == null) return Optional.empty();
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return Optional.of(LOOKUP.unreflectSetter(field));
            } catch (NoSuchFieldException | IllegalAccessException | LinkageError ignored) {
            }
        }
        return Optional.empty();
    }

    public static Object staticFieldValue(Class<?> clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    public static Optional<Object> optionalStaticFieldValue(Class<?> clazz, String fieldName) {
        if (clazz == null) return Optional.empty();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return Optional.ofNullable(field.get(null));
        } catch (NoSuchFieldException | IllegalAccessException | LinkageError ignored) {
            return Optional.empty();
        }
    }
}
