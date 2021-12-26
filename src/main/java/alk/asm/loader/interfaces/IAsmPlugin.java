package alk.asm.loader.interfaces;

public interface IAsmPlugin {
	
	String getName();
	
    /**
     * Return a list of classes that implements the IClassTransformer interface
     * @return a list of classes that implements the IClassTransformer interface
     */
    String[] getASMTransformerClass();
    
    String[] getTransformerExclusions();
    
    int getSortingIndex();

    String getSetupClass();

}