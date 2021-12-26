package alk.asm.loader.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
	
    public static Log log = new Log();
    private Logger myLog;

    private static boolean configured;

    private static void configureLogging() {
        log.myLog = LogManager.getLogger("Alk-Asm-Launcher");
        configured = true;
    }

    public static void retarget(Logger to) {
        log.myLog = to;
    }
    public static void log(String logChannel, Level level, String format, Object... data) {
        makeLog(logChannel);
        LogManager.getLogger(logChannel).log(level, String.format(format, data));
    }
    
    public static void log(Level level, String aString) {
        if (!configured) {
            configureLogging();
        }
        log.myLog.log(level, aString);
    }

    public static void log(Level level, String format, Object... data) {
        if (!configured) {
            configureLogging();
        }
        logWithSystem(level, String.format(format, data));
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data) {
        makeLog(logChannel);
        LogManager.getLogger(logChannel).log(level, String.format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        if (!configured) {
            configureLogging();
        }
        log.myLog.log(level, String.format(format, data), ex);
    }
	
	public static void log(String aText) {
		logWithSystem(Level.INFO, aText);
	}

    public static void severe(String aString) {
        log(Level.ERROR, aString);
    }
    
    public static void severe(String format, Object... data) {
        log(Level.ERROR, format, data);
    }

    public static void warning(String format, Object... data) {
        log(Level.WARN, format, data);
    }
    
    public static void warning(String aString) {
        log(Level.WARN, aString);
    }

    public static void info(String format, Object... data) {
        log(Level.INFO, format, data);
    }

    public static void fine(String format, Object... data) {
        log(Level.DEBUG, format, data);
    }

    public static void finer(String format, Object... data) {
        log(Level.TRACE, format, data);
    }

    public static void finest(String format, Object... data) {
        log(Level.TRACE, format, data);
    }

    public static void makeLog(String logChannel) {
    	LogManager.getLogger(logChannel);
    }

	public static void reflection(String aText) {
		logWithSystem(Level.INFO, "[Reflection] "+aText);
	}
	
	private static void logWithSystem(Level aLevel, String aString) {
		System.out.println(aLevel.name()+": "+aString);
	}

}
