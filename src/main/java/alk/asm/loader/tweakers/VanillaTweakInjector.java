package alk.asm.loader.tweakers;

import java.io.File;

import alk.asm.loader.Main;
import alk.asm.loader.interfaces.IClassTransformer;

public class VanillaTweakInjector implements IClassTransformer {
	
    public VanillaTweakInjector() {
    	
    }

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] bytes) {
    	return bytes;
    }

    public static File inject() {
        return Main.sHomeDir;
    }

	@Override
	public String getTargetClass() {
		return Main.sTargetLaunchClass;
	}

}
