package alk.asm.loader.interfaces;

import java.io.File;
import java.util.List;

import alk.asm.loader.AsmClassLoader;

public interface ITweaker {

    void acceptOptions(List<String> args, File homeDir);

    void injectIntoClassLoader(AsmClassLoader classLoader);

    String getLaunchTarget();

    String[] getLaunchArguments();

}
