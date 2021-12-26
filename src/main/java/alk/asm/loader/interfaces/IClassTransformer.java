package alk.asm.loader.interfaces;

public interface IClassTransformer {

    byte[] transform(String name, String transformedName, byte[] basicClass);
    
    String getTargetClass();

}