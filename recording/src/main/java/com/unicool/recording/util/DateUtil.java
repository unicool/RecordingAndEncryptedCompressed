package com.unicool.recording.util;

import android.text.TextUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class DateUtil {
    /**
     * 年月日时分秒毫秒(无下划线) yyyyMMddHHmmssSSS
     */
    public static final String LONG_DATETIME = "yyyyMMddHHmmssSSS";

    /**
     * 完整时间 yyyy-MM-dd HH:mm:ss
     */
    public static final String SIMPLE_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 完整时间,不含分隔符 yyyyMMddHHmmss
     */
    public static final String SIMPLE_DATE_NO_SEPARATOR = "yyyyMMddHHmmss";

    /**
     * 年月日 yyyy-MM-dd
     */
    public static final String SHORT_DATE = "yyyy-MM-dd";

    /**
     * 年月日(无下划线) yyyyMMdd
     */
    public static final String SHORT_DATE_NO_SEPARATOR = "yyyyMMdd";

    /**
     * 时 分 HH:mm
     */
    public static final String SIMPLE_HOUR_MINUTE = "HH:mm";
    // 一天的秒数
    public static final int SecondOfOneDay = 86400;

    /**
     * 返回系统当前时间(精确到毫秒)
     *
     * @return 以yyyyMMddHHmmssSSS为格式的当前系统时间
     */
    public static String getLongDateTime() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat(LONG_DATETIME, Locale.getDefault());
        return df.format(date);
    }

    /**
     * 获取系统当前日期，格式：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getSimpleDateTime() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat(SIMPLE_DATETIME, Locale.getDefault());
        return df.format(date);
    }

    /**
     * 获取系统当前时分，格式：HH:mm
     *
     * @return
     */
    public static String getSimpleHourMinute() {
        DateFormat df = new SimpleDateFormat(SIMPLE_HOUR_MINUTE, Locale.getDefault());
        return df.format(getSimpleDateTime());
    }

    /**
     * 将时间的秒数去掉
     *
     * @param time
     * @return
     */
    public static String getDateTimeNoSecond(String time) {
        // 有秒数时才去掉秒数
        if (time.split(":").length == 3) {
            return time.substring(0, time.lastIndexOf(":"));
        } else {
            return time;
        }
    }

    /**
     * 获取系统当前日期，格式：yyyyMMddHHmmss
     *
     * @return
     */
    public static String getSimpleDateTimeNoSeparator() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat(SIMPLE_DATE_NO_SEPARATOR,
                Locale.getDefault());
        return df.format(date);
    }

    /**
     * 获取系统当前年月日，格式：yyyyMMdd
     *
     * @return
     */
    public static String getDate() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat(SHORT_DATE_NO_SEPARATOR,
                Locale.getDefault());
        return df.format(date);
    }

    /**
     * 获取年月日，格式：yyyy-MM-dd
     *
     * @param date
     * @return
     */
    public static String getDate(Date date) {
        DateFormat df = new SimpleDateFormat(SHORT_DATE, Locale.getDefault());
        return df.format(date);
    }

    public static String dateToString(Date date, DateFormat dateFormat) {
        return dateFormat.format(date);
    }

    public static String dateToString(Date date, String format) {
        DateFormat df = new SimpleDateFormat(format, Locale.getDefault());
        return df.format(date);
    }

    /***
     * 将字符串日期转换成Date对象
     *
     * @param time yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static Date stringToDate(String time) {
        Date date = null;
        DateFormat df = new SimpleDateFormat(SIMPLE_DATETIME,
                Locale.getDefault());
        try {
            date = df.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 计算两个时间的差值，单位为秒
     *
     * @param time1
     * @param time2
     * @return 相差的秒数，time1>time2，返回正数，否则返回负数；如果转换错误，则返回Long.MIN_VALUE
     */
    public static long calcTime(String time1, String time2) {
        long result = Long.MIN_VALUE;
        if (TextUtils.isEmpty(time1) || TextUtils.isEmpty(time2)) {
            return result;
        }

        SimpleDateFormat df = new SimpleDateFormat(SIMPLE_DATETIME, Locale.getDefault());
        try {
            result = (df.parse(time1).getTime() - df.parse(time2).getTime()) / 1000;
            return result;
        } catch (ParseException e) {
            e.printStackTrace();
            return result;
        }
    }

    /**
     * 判断两个时间是否超过两天，非48小时
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isMoreThanTwoDays(String date1, String date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(stringToDate(date1));

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(stringToDate(date2));

        if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) {
            return Math.abs(cal1.get(Calendar.DAY_OF_YEAR) - cal2.get(Calendar.DAY_OF_YEAR)) >= 2;
        } else if (cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR) > 0) {
            return cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR) > 1 ||
                    Math.abs(cal1.get(Calendar.DAY_OF_YEAR) - cal2.get(Calendar.DAY_OF_YEAR) + cal2.getActualMaximum(Calendar.DAY_OF_YEAR)) >= 2;
        } else {
            return cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR) > 1 ||
                    Math.abs(cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR) + cal1.getActualMaximum(Calendar.DAY_OF_YEAR)) >= 2;
        }
    }

    /**
     * 判断两个时间是否在同一天
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameDate(String date1, String date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(stringToDate(date1));

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(stringToDate(date2));

        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * 将秒数转换成简单日期格式
     *
     * @param second 1970年以来的毫秒数
     * @return
     */
    public static String secondToDateTime(String second) {
        if (TextUtils.isEmpty(second)) {
            return "";
        }

        Date date = new Date(Long.parseLong(second));
        SimpleDateFormat sf = new SimpleDateFormat(SIMPLE_DATETIME,
                Locale.getDefault());
        return sf.format(date);
    }

    /**
     * 将秒数转换成短日期格式（不含小时、分、秒部分）
     *
     * @param second 1970年以来的毫秒数
     * @return
     */
    public static String secondToShortDate(String second) {
        if (TextUtils.isEmpty(second)) {
            return "";
        }
        Date date = new Date(Long.parseLong(second));
        SimpleDateFormat sf = new SimpleDateFormat(SHORT_DATE,
                Locale.getDefault());
        return sf.format(date);
    }

    /**
     * @param date
     * @return
     */
    public static String csharpDateToJavaDate(String date) {
        if (TextUtils.isEmpty(date)) {
            return "";
        }
        String second = Pattern.compile("[^0-9]").matcher(date).replaceAll("");
        if (TextUtils.isEmpty(second)) {
            return "";
        }
        return secondToDateTime(second);
    }

    /**
     * 设定夜间模式开始时间点或结束时间点
     *
     * @param hour
     * @param minute
     * @param isAddOneDay 是否需要加1天
     * @return
     */
    public static String setTimePoint(int hour, int minute, boolean isAddOneDay) {
        Calendar calendar = Calendar.getInstance();
        if (isAddOneDay) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return DateUtil.dateToString(calendar.getTime(), DateUtil.SIMPLE_DATETIME);
    }
}
