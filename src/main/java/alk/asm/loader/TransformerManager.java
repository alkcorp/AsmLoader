package alk.asm.loader;

import static alk.asm.instrumentation.ListLoadedClassesAgent.getClassLoader;
import static alk.asm.instrumentation.ListLoadedClassesAgent.instrumentation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import alk.asm.instrumentation.ListLoadedClassesAgent;
import alk.asm.loader.interfaces.IAsmPlugin;
import alk.asm.loader.interfaces.ISetupClass;
import alk.asm.loader.interfaces.ITweaker;
import alk.asm.loader.tweakers.TransformerInjectionAndSortingTweaker;
import alk.asm.loader.util.Log;
import alk.asm.loader.util.ReflectionUtils;


public class TransformerManager {

	private static List<String> sLoadedTransformers = Lists.newArrayList();
	private static List<TransformerWrapper> sLoadedPlugins;

	private static class TransformerWrapper implements ITweaker {

		public final String mName;
		public final IAsmPlugin mTransformerInstance;
		public final String mLocation;
		public final int mSortIndex;

		public TransformerWrapper(String aName, IAsmPlugin aTransformerInstance, int aSortIndex) {
			super();
			this.mName = aName;
			this.mTransformerInstance = aTransformerInstance;
			this.mLocation = aTransformerInstance.getClass().getCanonicalName();
			this.mSortIndex = aSortIndex;
		}

		@Override
		public String toString() {
			return String.format("%s {%s}", this.mName);
		}

		@Override
		public void acceptOptions(List<String> aArgs, File aHomeDir) {
			// NO OP
		}

		@Override
		public void injectIntoClassLoader(AsmClassLoader aClassLoader) {
			Log.fine("Injecting transformer %s {%s} class transformers", mName, mTransformerInstance.getClass().getName());
			if (mTransformerInstance.getASMTransformerClass() != null) 
				for (String aTransformer : mTransformerInstance.getASMTransformerClass()) {
				Log.finer("Registering transformer %s", aTransformer);
				aClassLoader.registerTransformer(aTransformer);
			}
			else {
				Log.finer("Transformer %s has no results for getASMTransformerClass()", mTransformerInstance.getName());
			}
			Log.fine("Injection complete");

			Log.fine("Running transformer plugin for %s {%s}", mName, mTransformerInstance.getClass().getName());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("mcLocation", Main.sHomeDir);
			data.put("coremodList", sLoadedPlugins);
			Log.fine("Running transformer %s", mName);
			data.put("coremodLocation", mLocation);
			String setupClass = mTransformerInstance.getSetupClass();
			if (setupClass != null)
			{
				try
				{
					ISetupClass call = (ISetupClass) Class.forName(setupClass, true, aClassLoader).newInstance();
					Map<String, Object> callData = new HashMap<String, Object>();
					callData.put("mcLocation", Main.sHomeDir);
					callData.put("classLoader", aClassLoader);
					callData.put("coremodLocation", mLocation);
					call.injectData(callData);
					call.call();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			Log.fine("Transformer class %s run successfully", mTransformerInstance.getClass().getSimpleName());

		}

		@Override
		public String getLaunchTarget()
		{
			return "";
		}

		@Override
		public String[] getLaunchArguments()
		{
			return new String[0];
		}

	}

	public static void handleLaunch(File aHomeDir, AsmClassLoader aClassLoader) {

		if (!injectTransformer("alk.asm.loader.tweakers.TransformerInjectionAndSortingTweaker")) {
			Log.severe("Unable to load transformer: alk.asm.loader.tweakers.TransformerInjectionAndSortingTweaker");
			System.exit(1);
		}
		sLoadedPlugins = new ArrayList<TransformerWrapper>();

		Log.fine("All fundamental transformers are successfully located");
		// Now that we have the root plugins loaded - lets see what else might
		// be around
		String aCommandLineTransformers = System.getProperty("alk.transformers.load", "");
		for (String aTransformerClassName : aCommandLineTransformers.split(","))
		{
			if (aTransformerClassName.isEmpty())
			{
				continue;
			}
			Log.info("Found a command line transformer : %s", aTransformerClassName);
			//loadTransformer(aClassLoader, aTransformerClassName);
		}
		discoverTransformers(aClassLoader);

	}
	
	private static Class sAsmPluginInterace;

	private static void discoverTransformers(AsmClassLoader aClassLoader) {
		Log.fine("Discovering transformers");
		sAsmPluginInterace = ReflectionUtils.getClass("alk.asm.loader.interfaces.IAsmPlugin", true);
		Set<Class> aTransformers = ReflectionUtils.dynamicallyFindClassesInPackageImplementing("alk.asm.loader.transformers", sAsmPluginInterace);
		Log.log("Found "+aTransformers.size()+" valid transformer classes.");
		for (Class aTransformer : aTransformers) {
			Log.finer("Adding %s to the list of known transformers, it will not be examined again", aTransformer.getName());
			Log.log("ClassLoader: "+aTransformer.getClassLoader().hashCode());		
			sLoadedTransformers.add(aTransformer.getName());                
			loadTransformer(aClassLoader, aTransformer);
		}
	}

	public static List<String> getLoadedTransformers()
	{
		return sLoadedTransformers;
	}

	private static TransformerWrapper loadTransformer(AsmClassLoader aClassLoader, Class aTransformerClass)
	{
		
		String aTransformerClassName = aTransformerClass.getCanonicalName();
		String aTransformerName = aTransformerClassName.substring(aTransformerClassName.lastIndexOf('.') + 1);
		try	{
			Log.fine("Instantiating transformer class %s", aTransformerName);
			aClassLoader.addTransformerExclusion(aTransformerClassName);
			//Class<?> aTransformerClass = ReflectionUtils.getClass(aTransformerClassName);
			

			Object instance = aTransformerClass.newInstance();		
			if (instance != null) {
				Log.log("Instance is not null");

				if (instance instanceof IAsmPlugin) {
					Log.log("Instance is IAsmPlugin");
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode());		
					Log.log("ClassLoader: "+instance.getClass().getClassLoader() + " | " + IAsmPlugin.class.getClassLoader());	
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode() + " " + (instance.getClass().getClassLoader() == IAsmPlugin.class.getClassLoader()));						
				}

				else {
					Log.log("Instance is not IAsmPlugin - "+instance.getClass().getCanonicalName());
					Log.log("ClassLoader: "+instance.getClass().getClassLoader() + " | " + IAsmPlugin.class.getClassLoader());	
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode() + " | " + IAsmPlugin.class.getClassLoader().hashCode() + " | " +(instance.getClass().getClassLoader() == IAsmPlugin.class.getClassLoader()));		
				}
				
				if (sAsmPluginInterace.isInstance(instance)) {
					Log.log("Instance is IAsmPlugin [Reflection]");
					Log.log("ClassLoader: "+instance.getClass().getClassLoader() + " | " + IAsmPlugin.class.getClassLoader());	
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode() + " " +(instance.getClass().getClassLoader() == IAsmPlugin.class.getClassLoader()));		
				}
				else {
					Log.log("Instance is not IAsmPlugin [Reflection] - "+instance.getClass().getCanonicalName());
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode());	
					Log.log("ClassLoader: "+instance.getClass().getClassLoader() + " | " + IAsmPlugin.class.getClassLoader());	
					Log.log("ClassLoader: "+instance.getClass().getClassLoader().hashCode() + " " +(instance.getClass().getClassLoader() == IAsmPlugin.class.getClassLoader()));		
					Class[] interfaces = instance.getClass().getInterfaces();
					for (Class i : interfaces) {
					    if (i.toString().equals(IAsmPlugin.class.toString())) {
							Log.log("Implements "+i.getCanonicalName()+"");
							break;
					    }
					}
				}
			}
			else {
				Log.log("Instance is null");				
			}
			Log.log("============");
	        //printClassesLoadedBy("BOOTSTRAP");
			Log.log("============");
	        printClassesLoadedBy("SYSTEM");
			Log.log("============");
	        //printClassesLoadedBy("EXTENSION");
			Log.log("============");
			/*for (Class clazz : instrumentation.getAllLoadedClasses()) {
				if (clazz != null) {
					ClassLoader aLoader = clazz.getClassLoader();	        		
					Log.log("Instrument: "+clazz.getCanonicalName()+" | "+(aLoader != null ? aLoader.hashCode() : "Null Class Loader"));
				}
			}*/
			Log.log("============");
			
			Constructor aConstructor = ReflectionUtils.getConstructor(aTransformerClass);
			IAsmPlugin aAsmPlugin = ReflectionUtils.createNewInstanceFromConstructor(aConstructor, new Object[] {});			
			int aSortIndex = aAsmPlugin.getSortingIndex();
			aTransformerName = aAsmPlugin.getName();
			if (aAsmPlugin.getTransformerExclusions() != null && aAsmPlugin.getTransformerExclusions().length > 0) {
				for (String aExclusion : aAsmPlugin.getTransformerExclusions()) {
					aClassLoader.addTransformerExclusion(aExclusion);					
				}
			}
			TransformerWrapper aWrapper = new TransformerWrapper(aTransformerName, aAsmPlugin, aSortIndex);
			sLoadedPlugins.add(aWrapper);
			Log.fine("Enqueued transformer %s", aTransformerName);
			return aWrapper;
		}
		catch (ClassCastException cce) {
			Log.log(Level.ERROR, cce, "Transformer %s: The plugin %s is not an implementor of IAsmPlugin", aTransformerName, aTransformerClassName);
		}
		catch (InstantiationException ie) {
			Log.log(Level.ERROR, ie, "Transformer %s: The plugin class %s was not instantiable", aTransformerName, aTransformerClassName);
		}
		catch (IllegalAccessException iae) {
			Log.log(Level.ERROR, iae, "Transformer %s: The plugin class %s was not accessible", aTransformerName, aTransformerClassName);
		}
		return null;
	}

	public static void injectCoreModTweaks(TransformerInjectionAndSortingTweaker aInjectionAndSortingTweaker)
	{
		@SuppressWarnings("unchecked")
		List<ITweaker> aTweakers = (List<ITweaker>) Main.sBlackboard.get("Tweaks");
		// Add the sorting tweaker first- it'll appear twice in the list
		aTweakers.add(0, aInjectionAndSortingTweaker);
		for (TransformerWrapper aWrapper : sLoadedPlugins)
		{
			aTweakers.add(aWrapper);
		}
	}

	private static Map<String,Integer> sTweakSorting = Maps.newHashMap();

	public static void sortTweakList()
	{
		@SuppressWarnings("unchecked")
		List<ITweaker> aTweaks = (List<ITweaker>) Main.sBlackboard.get("Tweaks");
		// Basically a copy of Collections.sort pre 8u20, optimized as we know we're an array list.
		// Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
		ITweaker[] aToSort = aTweaks.toArray(new ITweaker[aTweaks.size()]);
		Arrays.sort(aToSort, new Comparator<ITweaker>() {
			@Override
			public int compare(ITweaker o1, ITweaker o2)
			{
				Integer first = null;
				Integer second = null;
				if (o1 instanceof TransformerInjectionAndSortingTweaker)
				{
					first = Integer.MIN_VALUE;
				}
				if (o2 instanceof TransformerInjectionAndSortingTweaker)
				{
					second = Integer.MIN_VALUE;
				}

				if (o1 instanceof TransformerWrapper)
				{
					first = ((TransformerWrapper) o1).mSortIndex;
				}
				else if (first == null)
				{
					first = sTweakSorting.get(o1.getClass().getName());
				}
				if (o2 instanceof TransformerWrapper)
				{
					second = ((TransformerWrapper) o2).mSortIndex;
				}
				else if (second == null)
				{
					second = sTweakSorting.get(o2.getClass().getName());
				}
				if (first == null)
				{
					first = 0;
				}
				if (second == null)
				{
					second = 0;
				}

				return Ints.saturatedCast((long)first - (long)second);
			}
		});
		// Basically a copy of Collections.sort, optimized as we know we're an array list.
		// Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
		for (int j = 0; j < aToSort.length; j++) {
			aTweaks.set(j, aToSort[j]);
		}
	}

	public static boolean injectTransformer(String aTransformerClassName) {
		Log.log("Injecting Transformer into Blackboard: "+aTransformerClassName);
		@SuppressWarnings("unchecked")
		List<String> tweakClasses = (List<String>) Main.sBlackboard.get("TweakClasses");
		return tweakClasses.add(aTransformerClassName);
	}
	
	private static void printClassesLoadedBy(String classLoaderType) {
        System.out.println(classLoaderType + " ClassLoader : ");
        Class<?>[] classes = ListLoadedClassesAgent.listLoadedClasses(classLoaderType);        
        for (Class clazz : classes) {
        	if (clazz != null) {
        		String aClassName = clazz.getCanonicalName();
        		if (aClassName != null && aClassName.length() > 0) {
        			if (
        					clazz.getCanonicalName().startsWith("java.") ||
        					clazz.getCanonicalName().startsWith("javax.") ||
        					clazz.getCanonicalName().startsWith("sun.") ||
        					clazz.getCanonicalName().startsWith("com.sun.") ||
        					clazz.getCanonicalName().startsWith("jdk.")
        					 || clazz.getCanonicalName().startsWith("javax.")
        					 || clazz.getCanonicalName().startsWith("org.apache")
        					 || clazz.getCanonicalName().startsWith("com.google")
        					 || clazz.getCanonicalName().startsWith("org.eclipse")) {
        				continue;
        			}
        			else {
                		ClassLoader aLoader = clazz.getClassLoader();        		
                		Log.log(classLoaderType+": "+aClassName+" | "+(aLoader != null ? aLoader.hashCode() : "Bootstrap Class Loader"));        				
        			}
        		}
        		
        	}
        }
    }
	
}