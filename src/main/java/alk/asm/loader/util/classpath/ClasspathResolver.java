package alk.asm.loader.util.classpath;

import java.net.URL;

public interface ClasspathResolver {
    URL[] resolve(ClassLoader loader) throws Throwable;
}