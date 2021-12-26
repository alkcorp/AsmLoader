package alk.asm.loader;

public class MainThread extends Thread {

    public final String[] mArgs;

    public MainThread(String[] args, AsmClassLoader sClassLoader) {        
        mArgs = args;
        setContextClassLoader(sClassLoader);
        setName("Launch-Thread");
        start();
    }
	
	@Override
	public void run() {
        new Launch(mArgs);
	}
	
}
