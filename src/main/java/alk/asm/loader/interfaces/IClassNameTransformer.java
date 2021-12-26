package alk.asm.loader.interfaces;

public interface IClassNameTransformer {

    String unmapClassName(String name);

    String remapClassName(String name);

}