package com.samknows.measurement.environment.linux;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import com.samknows.libcore.SKLogger;

public class MpStatWrapper {
  static final String TAG = "MpStatWrapper";

	/**
	 * works only on rooted devices
	 * @param timePeriod in millis
	 * @return
	 */
	@Deprecated
	public static float getCPULoad(long timePeriod) {
		Log.d(TAG, "start cpu test for " + timePeriod / 1000 + "s");
		float idle = 100;
		try {
			//mpstat uses seconds
//			Process  p = Runtime.getRuntime().exec("mpstat " + timePeriod/1000 + " 1");
			Process  p = Runtime.getRuntime().exec("ps");
			 p.waitFor();
			 String result = IOUtils.toString(p.getInputStream());
			 String average = result.substring(result.lastIndexOf("Average"), result.length());
			 String data[] = average.split(" ");
			 String sidle = data[data.length - 1].trim();

			 idle = Float.valueOf(sidle); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG, "finished cpu test");
		
		
		return 100 - idle;
	}
}
