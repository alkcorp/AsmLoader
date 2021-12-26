package alk.asm.loader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.google.common.base.Throwables;

import alk.asm.loader.interfaces.ITweaker;
import alk.asm.loader.util.Log;


public class Launch extends Thread {

    public final String[] mArgs;

    public Launch(String[] args) {        
        try {
            //System.setSecurityManager(new AsmSecurityManager());
        }
        catch (SecurityException se) {
            throw new RuntimeException("FML was unable to install the security manager. The game will not start", se);
        }
        mArgs = args;
    }
    


    private void launch(String[] args) {
    	        
        final List<String> tweakClassNames = new ArrayList<String>();
        final List<String> argumentList = new ArrayList<String>();
        for (String arg : args) {
        	argumentList.add(arg);
        }
        // This list of names will be interacted with through tweakers. They can append to this list
        // any 'discovered' tweakers from their preferred mod loading mechanism
        // By making this object discoverable and accessible it's possible to perform
        // things like cascading of tweakers
        Main.sBlackboard.put("TweakClasses", tweakClassNames);
        setupHome();

        // This argument list will be constructed from all tweakers. It is visible here so
        // all tweakers can figure out if a particular argument is present, and add it if not
        Main.sBlackboard.put("ArgumentList", argumentList);

        // This is to prevent duplicates - in case a tweaker decides to add itself or something
        final Set<String> allTweakerNames = new HashSet<String>();
        // The 'definitive' list of tweakers
        final List<ITweaker> allTweakers = new ArrayList<ITweaker>();
        try {
            final List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
            // The list of tweak instances - may be useful for interoperability
            Main.sBlackboard.put("Tweaks", tweakers);
            // The primary tweaker (the first one specified on the command line) will actually
            // be responsible for providing the 'main' mName and generally gets called first
            ITweaker primaryTweaker = null;
            // This loop will terminate, unless there is some sort of pathological tweaker
            // that reinserts itself with a new identity every pass
            // It is here to allow tweakers to "push" new tweak classes onto the 'stack' of
            // tweakers to evaluate allowing for cascaded discovery and injection of tweakers
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    // Safety check - don't reprocess something we've already visited
                    if (allTweakerNames.contains(tweakName)) {
                    	Log.log(Level.WARN, "Tweak class mName %s has already been visited -- skipping", tweakName);
                        // remove the tweaker from the stack otherwise it will create an infinite loop
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    Log.log(Level.INFO, "Loading tweak class mName %s", tweakName);

                    // Ensure we allow the tweak class to load with the parent classloader
                    Main.sClassLoader.addClassLoaderExclusion(tweakName.substring(0,tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, Main.sClassLoader).newInstance();
                    tweakers.add(tweaker);

                    // Remove the tweaker from the list of tweaker names we've processed this pass
                    it.remove();
                    // If we haven't visited a tweaker yet, the first will become the 'primary' tweaker
                    if (primaryTweaker == null) {
                    	Log.log(Level.INFO, "Using primary tweak class mName %s", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                // Now, iterate all the tweakers we just instantiated
                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    Log.log(Level.INFO, "Calling tweak class %s", tweaker.getClass().getName());
                    tweaker.acceptOptions(argumentList, Main.sHomeDir);
                    tweaker.injectIntoClassLoader(Main.sClassLoader);
                    allTweakers.add(tweaker);
                    // again, remove from the list once we've processed it, so we don't get duplicates
                    it.remove();
                }
                // continue around the loop until there's no tweak classes
            } while (!tweakClassNames.isEmpty());

            // Once we're done, we then ask all the tweakers for their arguments and add them all to the
            // master argument list
            for (final ITweaker tweaker : allTweakers) {
                argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }

            // Finally we turn to the primary tweaker, and let it tell us where to go to launch
            final String launchTarget = Main.sTargetLaunchClass;
            final Class<?> clazz = Class.forName(launchTarget, false, Main.sClassLoader);
            final Method mainMethod = clazz.getMethod("main", new Class[]{String[].class});

            Log.info("Launching wrapped %s", launchTarget);
            mainMethod.invoke(null, (Object) argumentList.toArray(new String[argumentList.size()]));
        } 
        catch (Exception e) {
        	Log.log(Level.ERROR, e, "Unable to launch");
            System.exit(1);
        }      	
    }
    
    private void setupHome() {
        
        Log.info("Java is %s, version %s, running on %s:%s:%s, installed at %s", System.getProperty("java.vm.name"), System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"), System.getProperty("java.home"));
        Log.fine("Java classpath at launch is %s", System.getProperty("java.class.path"));
        Log.fine("Java library path at launch is %s", System.getProperty("java.library.path"));

        try
        {
            TransformerManager.handleLaunch(Main.sHomeDir, Main.sClassLoader);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Log.log(Level.ERROR, t, "An error occurred trying to configure the minecraft home at %s for Forge Mod Loader", Main.sHomeDir.getAbsolutePath());
            throw Throwables.propagate(t);
        }
    }

	@Override
	public void run() {
        launch(mArgs);
	}

}
