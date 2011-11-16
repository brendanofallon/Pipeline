package util;

public class ElapsedTimeFormatter {

	public static String getElapsedTime(long start, long end) {
		long elapsedMillis = end - start;
		if (elapsedMillis < 1000) {
			return elapsedMillis + " ms";
		}
		int seconds = (int)Math.round( elapsedMillis / 1000.0 );
		int minutes = (int)Math.round( elapsedMillis / (1000.0 * 60.0) );
		int hours = (int)Math.round( elapsedMillis / (1000.0 * 60.0 * 60.0));
		
		//I think this is right?
		seconds = seconds % 60;
		minutes  = minutes % 60;
		
		return hours + ":" + minutes + ":" + seconds;
	}
}
