package alk.asm.loader.tweakers;

import java.io.File;
import java.util.List;

import alk.asm.loader.AsmClassLoader;
import alk.asm.loader.Main;
import alk.asm.loader.interfaces.ITweaker;

public class VanillaTweaker implements ITweaker {
	
    private List<String> mArgs;

    @Override
    public void acceptOptions(List<String> aArgs, File aHomeDir) {
        this.mArgs = aArgs;
    }

    @Override
    public void injectIntoClassLoader(AsmClassLoader classLoader) {
        classLoader.registerTransformer(VanillaTweakInjector.class.getCanonicalName());
    }

    @Override
    public String getLaunchTarget() {
        return Main.sTargetLaunchClass;
    }

    @Override
    public String[] getLaunchArguments() {
        return mArgs.toArray(new String[mArgs.size()]);
    }
}
