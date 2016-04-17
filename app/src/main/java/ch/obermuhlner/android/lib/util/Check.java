package ch.obermuhlner.android.lib.util;

public class Check {

	private static final boolean ENABLED = true;
	
	public static void isTrue(boolean value) {
		if (ENABLED) {
			isTrue(value, null);
		}
	}
	
	public static void isTrue(boolean value, String message) {
		if (ENABLED) {
			if (!value) {
				throw new IllegalArgumentException(message);
			}
		}
	}
	
	public static void isNotNull(Object value) {
		if (ENABLED) {
			isNotNull(value, null);
		}
	}
	
	public static void isNotNull(Object value, String message) {
		if (ENABLED) {
			if (value == null) {
				throw new NullPointerException(message);
			}
		}
	}
}
