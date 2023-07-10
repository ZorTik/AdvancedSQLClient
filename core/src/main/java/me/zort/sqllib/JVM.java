package me.zort.sqllib;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public abstract class JVM {

  public abstract Object invokeDefault(Class<?> declaringClass, Object instance, Method method, Object[] args) throws Throwable;

  public static JVM getJVM() {
    int ver = jvmVer();
    if (ver < 8)
      throw new UnsupportedOperationException("Unsupported JVM version: " + ver);
    else if (ver == 8)
      return new JVM8();
    else
      return new JVM9();
  }

  private static int jvmVer() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }


  static final class JVM8 extends JVM {
    @Override
    public Object invokeDefault(Class<?> declaringClass, Object instance, Method method, Object[] args) throws Throwable {
      Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
      constructor.setAccessible(true);
      return constructor.newInstance(declaringClass)
              .in(declaringClass)
              .unreflectSpecial(method, declaringClass)
              .bindTo(instance)
              .invokeWithArguments(args);
    }
  }

  static final class JVM9 extends JVM {
    @Override
    public Object invokeDefault(Class<?> declaringClass, Object instance, Method method, Object[] args) throws Throwable {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      return lookup.findSpecial(declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), declaringClass)
              .bindTo(instance)
              .invokeWithArguments(args);
    }
  }

}
