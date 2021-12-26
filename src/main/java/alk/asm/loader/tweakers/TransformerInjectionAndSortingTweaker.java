package alk.asm.loader.tweakers;

import java.io.File;
import java.util.List;

import alk.asm.loader.AsmClassLoader;
import alk.asm.loader.Main;
import alk.asm.loader.TransformerManager;
import alk.asm.loader.interfaces.ITweaker;


/**
 * This class is to manage the injection of coremods as tweakers into the tweak framework.
 * It has to inject the coremod tweaks during construction, because that is the only time
 * the tweak list is writeable.
 *
 */
public class TransformerInjectionAndSortingTweaker implements ITweaker {

	private boolean run;
	public TransformerInjectionAndSortingTweaker()
	{
		TransformerManager.injectCoreModTweaks(this);
		run = false;
	}

	@Override
	public void acceptOptions(List<String> args, File gameDir)
	{
		if (!run) {
			// We sort the tweak list here so that it obeys the tweakordering
			TransformerManager.sortTweakList();
			@SuppressWarnings("unchecked")
			List<String> newTweaks = (List<String>) Main.sBlackboard.get("TweakClasses");
			//newTweaks.add("alk.asm.loader.transformers.LoggingTransformer");
		}
		run = true;
	}

	@Override
	public void injectIntoClassLoader(AsmClassLoader classLoader)
	{
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