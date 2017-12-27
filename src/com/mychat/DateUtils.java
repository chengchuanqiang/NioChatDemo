package com.mychat;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期工具类
 * @author ccq
 *
 */
public class DateUtils {
	
	private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
	
	public static String getCurrentDate(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(PATTERN);
		return simpleDateFormat.format(date);
	}

}
