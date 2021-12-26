package alk.asm.loader.util;

public class StringUtils {

	public static <V> String getDataStringFromArray(V[] parameterTypes) {
		if (parameterTypes == null || parameterTypes.length == 0) {
			return "empty/null";
		}
		else {
			String aData = "";
			for (V y : parameterTypes) {
				if (y != null) {
					aData += ", "+y.toString();
				}
			}
			return aData;
		}		
	}
	
}
