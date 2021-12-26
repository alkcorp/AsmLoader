package alk.asm.loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import alk.asm.loader.util.classpath.Classpath;

public class Main {
	
	public static Map<String,Object> sBlackboard;
	public static AsmClassLoader sClassLoader;
	public static File sHomeDir;
	public static final String sTargetLaunchClass = "alk.test.HelloWorld";

    public static void main(String[] args) {
    	sClassLoader = new AsmClassLoader(Classpath.getClasspath());
        sBlackboard = new HashMap<String, Object>(); 
        sHomeDir = getHomeDirectory();
        Thread aLaunch = new Launch(args);
        aLaunch.setContextClassLoader(sClassLoader);
        aLaunch.setName("Launch-Thread");
        aLaunch.start();
    }

    private static File getHomeDirectory() {
    	return new File("");
    }
	
}
