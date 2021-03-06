/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica.util;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Utility functions for datetime types: date, time, timestamp.
 *
 * <p>Used by the JDBC driver.
 *
 * <p>TODO: review methods for performance. Due to allocations required, it may
 * be preferable to introduce a "formatter" with the required state.
 */
public class DateTimeUtils {
  /** The julian date of the epoch, 1970-01-01. */
  public static final int EPOCH_JULIAN = 2440588;

  private DateTimeUtils() {}

  //~ Static fields/initializers ---------------------------------------------

  /** The SimpleDateFormat string for ISO dates, "yyyy-MM-dd". */
  public static final String DATE_FORMAT_STRING = "yyyy-MM-dd";

  /** The SimpleDateFormat string for ISO times, "HH:mm:ss". */
  public static final String TIME_FORMAT_STRING = "HH:mm:ss";

  /** The SimpleDateFormat string for ISO timestamps, "yyyy-MM-dd HH:mm:ss". */
  public static final String TIMESTAMP_FORMAT_STRING =
      DATE_FORMAT_STRING + " " + TIME_FORMAT_STRING;

  /** The GMT time zone. */
  public static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");

  /** The Java default time zone. */
  public static final TimeZone DEFAULT_ZONE = TimeZone.getDefault();

  /**
   * The number of milliseconds in a second.
   */
  public static final long MILLIS_PER_SECOND = 1000L;

  /**
   * The number of milliseconds in a minute.
   */
  public static final long MILLIS_PER_MINUTE = 60000L;

  /**
   * The number of milliseconds in an hour.
   */
  public static final long MILLIS_PER_HOUR = 3600000L; // = 60 * 60 * 1000

  /**
   * The number of milliseconds in a day.
   *
   * <p>This is the modulo 'mask' used when converting
   * TIMESTAMP values to DATE and TIME values.
   */
  public static final long MILLIS_PER_DAY = 86400000; // = 24 * 60 * 60 * 1000

  /**
   * Calendar set to the epoch (1970-01-01 00:00:00 UTC). Useful for
   * initializing other values. Calendars are not immutable, so be careful not
   * to screw up this object for everyone else.
   */
  public static final Calendar ZERO_CALENDAR;

  static {
    ZERO_CALENDAR = Calendar.getInstance(DateTimeUtils.GMT_ZONE);
    ZERO_CALENDAR.setTimeInMillis(0);
  }

  /**
   * Calendar set to local time.
   */
  private static final Calendar LOCAL_CALENDAR = Calendar.getInstance();

  //~ Methods ----------------------------------------------------------------

  /**
   * Parses a string using {@link SimpleDateFormat} and a given pattern. This
   * method parses a string at the specified parse position and if successful,
   * updates the parse position to the index after the last character used.
   * The parsing is strict and requires months to be less than 12, days to be
   * less than 31, etc.
   *
   * @param s       string to be parsed
   * @param pattern {@link SimpleDateFormat}  pattern
   * @param tz      time zone in which to interpret string. Defaults to the Java
   *                default time zone
   * @param pp      position to start parsing from
   * @return a Calendar initialized with the parsed value, or null if parsing
   * failed. If returned, the Calendar is configured to the GMT time zone.
   * @pre pattern != null
   */
  private static Calendar parseDateFormat(
      String s,
      String pattern,
      TimeZone tz,
      ParsePosition pp) {
    assert pattern != null;
    SimpleDateFormat df = new SimpleDateFormat(pattern);
    if (tz == null) {
      tz = DEFAULT_ZONE;
    }
    Calendar ret = Calendar.getInstance(tz);
    df.setCalendar(ret);
    df.setLenient(false);

    java.util.Date d = df.parse(s, pp);
    if (null == d) {
      return null;
    }
    ret.setTime(d);
    ret.setTimeZone(GMT_ZONE);
    return ret;
  }

  /**
   * Parses a string using {@link SimpleDateFormat} and a given pattern. The
   * entire string must match the pattern specified.
   *
   * @param s       string to be parsed
   * @param pattern {@link SimpleDateFormat}  pattern
   * @param tz      time zone in which to interpret string. Defaults to the Java
   *                default time zone
   * @return a Calendar initialized with the parsed value, or null if parsing
   * failed. If returned, the Calendar is configured to the GMT time zone.
   */
  public static Calendar parseDateFormat(
      String s,
      String pattern,
      TimeZone tz) {
    assert pattern != null;
    ParsePosition pp = new ParsePosition(0);
    Calendar ret = parseDateFormat(s, pattern, tz, pp);
    if (pp.getIndex() != s.length()) {
      // Didn't consume entire string - not good
      return null;
    }
    return ret;
  }

  /**
   * Parses a string using {@link SimpleDateFormat} and a given pattern, and
   * if present, parses a fractional seconds component. The fractional seconds
   * component must begin with a decimal point ('.') followed by numeric
   * digits. The precision is rounded to a maximum of 3 digits of fractional
   * seconds precision (to obtain milliseconds).
   *
   * @param s       string to be parsed
   * @param pattern {@link SimpleDateFormat}  pattern
   * @param tz      time zone in which to interpret string. Defaults to the
   *                local time zone
   * @return a {@link DateTimeUtils.PrecisionTime PrecisionTime} initialized
   * with the parsed value, or null if parsing failed. The PrecisionTime
   * contains a GMT Calendar and a precision.
   */
  public static PrecisionTime parsePrecisionDateTimeLiteral(
      String s,
      String pattern,
      TimeZone tz) {
    assert pattern != null;
    ParsePosition pp = new ParsePosition(0);
    Calendar cal = parseDateFormat(s, pattern, tz, pp);
    if (cal == null) {
      return null; // Invalid date/time format
    }

    // Note: the Java SimpleDateFormat 'S' treats any number after
    // the decimal as milliseconds. That means 12:00:00.9 has 9
    // milliseconds and 12:00:00.9999 has 9999 milliseconds.
    int p = 0;
    if (pp.getIndex() < s.length()) {
      // Check to see if rest is decimal portion
      if (s.charAt(pp.getIndex()) != '.') {
        return null;
      }

      // Skip decimal sign
      pp.setIndex(pp.getIndex() + 1);

      // Parse decimal portion
      if (pp.getIndex() < s.length()) {
        String secFraction = s.substring(pp.getIndex());
        if (!secFraction.matches("\\d+")) {
          return null;
        }
        NumberFormat nf = NumberFormat.getIntegerInstance();
        Number num = nf.parse(s, pp);
        if ((num == null) || (pp.getIndex() != s.length())) {
          // Invalid decimal portion
          return null;
        }

        // Determine precision - only support prec 3 or lower
        // (milliseconds) Higher precisions are quietly rounded away
        p = Math.min(
            3,
            secFraction.length());

        // Calculate milliseconds
        int ms =
            (int) Math.round(
                num.longValue()
                * Math.pow(10, 3 - secFraction.length()));
        cal.add(Calendar.MILLISECOND, ms);
      }
    }

    assert pp.getIndex() == s.length();
    PrecisionTime ret = new PrecisionTime(cal, p);
    return ret;
  }

  /**
   * Gets the active time zone based on a Calendar argument
   */
  public static TimeZone getTimeZone(Calendar cal) {
    if (cal == null) {
      return DEFAULT_ZONE;
    }
    return cal.getTimeZone();
  }

  /**
   * Checks if the date/time format is valid
   *
   * @param pattern {@link SimpleDateFormat}  pattern
   * @throws IllegalArgumentException if the given pattern is invalid
   */
  public static void checkDateFormat(String pattern) {
    new SimpleDateFormat(pattern);
  }

  /**
   * Creates a new date formatter with Farrago specific options. Farrago
   * parsing is strict and does not allow values such as day 0, month 13, etc.
   *
   * @param format {@link SimpleDateFormat}  pattern
   */
  public static SimpleDateFormat newDateFormat(String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setLenient(false);
    return sdf;
  }

  /** Helper for CAST({timestamp} AS VARCHAR(n)). */
  public static String unixTimestampToString(long timestamp) {
    final StringBuilder buf = new StringBuilder(17);
    int date = (int) (timestamp / MILLIS_PER_DAY);
    int time = (int) (timestamp % MILLIS_PER_DAY);
    if (time < 0) {
      --date;
      time += MILLIS_PER_DAY;
    }
    unixDateToString(buf, date);
    buf.append(' ');
    unixTimeToString(buf, time);
    return buf.toString();
  }

  /** Helper for CAST({timestamp} AS VARCHAR(n)). */
  public static String unixTimeToString(int time) {
    final StringBuilder buf = new StringBuilder(8);
    unixTimeToString(buf, time);
    return buf.toString();
  }

  private static void unixTimeToString(StringBuilder buf, int time) {
    int h = time / 3600000;
    int time2 = time % 3600000;
    int m = time2 / 60000;
    int time3 = time2 % 60000;
    int s = time3 / 1000;
    int ms = time3 % 1000;
    int2(buf, h);
    buf.append(':');
    int2(buf, m);
    buf.append(':');
    int2(buf, s);
  }

  private static void int2(StringBuilder buf, int i) {
    buf.append((char) ('0' + (i / 10) % 10));
    buf.append((char) ('0' + i % 10));
  }

  private static void int4(StringBuilder buf, int i) {
    buf.append((char) ('0' + (i / 1000) % 10));
    buf.append((char) ('0' + (i / 100) % 10));
    buf.append((char) ('0' + (i / 10) % 10));
    buf.append((char) ('0' + i % 10));
  }

  /** Helper for CAST({date} AS VARCHAR(n)). */
  public static String unixDateToString(int date) {
    final StringBuilder buf = new StringBuilder(10);
    unixDateToString(buf, date);
    return buf.toString();
  }

  private static void unixDateToString(StringBuilder buf, int date) {
    julianToString(buf, date + EPOCH_JULIAN);
  }

  private static void julianToString(StringBuilder buf, int julian) {
    // this shifts the epoch back to astronomical year -4800 instead of the
    // start of the Christian era in year AD 1 of the proleptic Gregorian
    // calendar.
    int j = julian + 32044;
    int g = j / 146097;
    int dg = j % 146097;
    int c = (dg / 36524 + 1) * 3 / 4;
    int dc = dg - c * 36524;
    int b = dc / 1461;
    int db = dc % 1461;
    int a = (db / 365 + 1) * 3 / 4;
    int da = db - a * 365;

    // integer number of full years elapsed since March 1, 4801 BC
    int y = g * 400 + c * 100 + b * 4 + a;
    // integer number of full months elapsed since the last March 1
    int m = (da * 5 + 308) / 153 - 2;
    // number of days elapsed since day 1 of the month
    int d = da - (m + 4) * 153 / 5 + 122;
    int year = y - 4800 + (m + 2) / 12;
    int month = (m + 2) % 12 + 1;
    int day = d + 1;
    int4(buf, year);
    buf.append('-');
    int2(buf, month);
    buf.append('-');
    int2(buf, day);
  }

  public static String intervalYearMonthToString(int v, TimeUnitRange range) {
    final StringBuilder buf = new StringBuilder();
    if (v >= 0) {
      buf.append('+');
    } else {
      buf.append('-');
      v = -v;
    }
    final int y;
    final int m;
    switch (range) {
    case YEAR:
      v = roundUp(v, 12);
      y = v / 12;
      buf.append(y);
      break;
    case YEAR_TO_MONTH:
      y = v / 12;
      buf.append(y);
      buf.append('-');
      m = v % 12;
      number(buf, m, 2);
      break;
    case MONTH:
      m = v;
      buf.append(m);
      break;
    default:
      throw new AssertionError(range);
    }
    return buf.toString();
  }

  public static StringBuilder number(StringBuilder buf, int v, int n) {
    for (int k = digitCount(v); k < n; k++) {
      buf.append('0');
    }
    return buf.append(v);
  }

  public static int digitCount(int v) {
    for (int n = 1;; n++) {
      v /= 10;
      if (v == 0) {
        return n;
      }
    }
  }

  private static int roundUp(int dividend, int divisor) {
    int remainder = dividend % divisor;
    dividend -= remainder;
    if (remainder * 2 > divisor) {
      dividend += divisor;
    }
    return dividend;
  }

  /** Cheap, unsafe, long power. power(2, 3) returns 8. */
  public static long powerX(long a, long b) {
    long x = 1;
    while (b > 0) {
      x *= a;
      --b;
    }
    return x;
  }

  public static String intervalDayTimeToString(long v, TimeUnitRange range,
      int scale) {
    final StringBuilder buf = new StringBuilder();
    if (v >= 0) {
      buf.append('+');
    } else {
      buf.append('-');
      v = -v;
    }
    final long ms;
    final long s;
    final long m;
    final long h;
    final long d;
    switch (range) {
    case DAY_TO_SECOND:
      v = roundUp(v, powerX(10, 3 - scale));
      ms = v % 1000;
      v /= 1000;
      s = v % 60;
      v /= 60;
      m = v % 60;
      v /= 60;
      h = v % 24;
      v /= 24;
      d = v;
      buf.append((int) d);
      buf.append(' ');
      number(buf, (int) h, 2);
      buf.append(':');
      number(buf, (int) m, 2);
      buf.append(':');
      number(buf, (int) s, 2);
      fraction(buf, scale, ms);
      break;
    case DAY_TO_MINUTE:
      v = roundUp(v, 1000 * 60);
      v /= 1000;
      v /= 60;
      m = v % 60;
      v /= 60;
      h = v % 24;
      v /= 24;
      d = v;
      buf.append((int) d);
      buf.append(' ');
      number(buf, (int) h, 2);
      buf.append(':');
      number(buf, (int) m, 2);
      break;
    case DAY_TO_HOUR:
      v = roundUp(v, 1000 * 60 * 60);
      v /= 1000;
      v /= 60;
      v /= 60;
      h = v % 24;
      v /= 24;
      d = v;
      buf.append((int) d);
      buf.append(' ');
      number(buf, (int) h, 2);
      break;
    case DAY:
      v = roundUp(v, 1000 * 60 * 60 * 24);
      d = v / (1000 * 60 * 60 * 24);
      buf.append((int) d);
      break;
    case HOUR:
      v = roundUp(v, 1000 * 60 * 60);
      v /= 1000;
      v /= 60;
      v /= 60;
      h = v;
      buf.append((int) h);
      break;
    case HOUR_TO_MINUTE:
      v = roundUp(v, 1000 * 60);
      v /= 1000;
      v /= 60;
      m = v % 60;
      v /= 60;
      h = v;
      buf.append((int) h);
      buf.append(':');
      number(buf, (int) m, 2);
      break;
    case HOUR_TO_SECOND:
      v = roundUp(v, powerX(10, 3 - scale));
      ms = v % 1000;
      v /= 1000;
      s = v % 60;
      v /= 60;
      m = v % 60;
      v /= 60;
      h = v;
      buf.append((int) h);
      buf.append(':');
      number(buf, (int) m, 2);
      buf.append(':');
      number(buf, (int) s, 2);
      fraction(buf, scale, ms);
      break;
    case MINUTE_TO_SECOND:
      v = roundUp(v, powerX(10, 3 - scale));
      ms = v % 1000;
      v /= 1000;
      s = v % 60;
      v /= 60;
      m = v;
      buf.append((int) m);
      buf.append(':');
      number(buf, (int) s, 2);
      fraction(buf, scale, ms);
      break;
    case MINUTE:
      v = roundUp(v, 1000 * 60);
      v /= 1000;
      v /= 60;
      m = v;
      buf.append((int) m);
      break;
    case SECOND:
      v = roundUp(v, powerX(10, 3 - scale));
      ms = v % 1000;
      v /= 1000;
      s = v;
      buf.append((int) s);
      fraction(buf, scale, ms);
      break;
    default:
      throw new AssertionError(range);
    }
    return buf.toString();
  }

  /**
   * Rounds a dividend to the nearest divisor.
   * For example roundUp(31, 10) yields 30; roundUp(37, 10) yields 40.
   * @param dividend Number to be divided
   * @param divisor Number to divide by
   * @return Rounded dividend
   */
  private static long roundUp(long dividend, long divisor) {
    long remainder = dividend % divisor;
    dividend -= remainder;
    if (remainder * 2 > divisor) {
      dividend += divisor;
    }
    return dividend;
  }

  private static void fraction(StringBuilder buf, int scale, long ms) {
    if (scale > 0) {
      buf.append('.');
      long v1 = scale == 3 ? ms
          : scale == 2 ? ms / 10
          : scale == 1 ? ms / 100
            : 0;
      number(buf, (int) v1, scale);
    }
  }

  public static int dateStringToUnixDate(String s) {
    int hyphen1 = s.indexOf('-');
    int y;
    int m;
    int d;
    if (hyphen1 < 0) {
      y = Integer.parseInt(s.trim());
      m = 1;
      d = 1;
    } else {
      y = Integer.parseInt(s.substring(0, hyphen1).trim());
      final int hyphen2 = s.indexOf('-', hyphen1 + 1);
      if (hyphen2 < 0) {
        m = Integer.parseInt(s.substring(hyphen1 + 1).trim());
        d = 1;
      } else {
        m = Integer.parseInt(s.substring(hyphen1 + 1, hyphen2).trim());
        d = Integer.parseInt(s.substring(hyphen2 + 1).trim());
      }
    }
    return ymdToUnixDate(y, m, d);
  }

  public static int timeStringToUnixDate(String v) {
    return timeStringToUnixDate(v, 0);
  }

  public static int timeStringToUnixDate(String v, int start) {
    final int colon1 = v.indexOf(':', start);
    int hour;
    int minute;
    int second;
    int milli;
    if (colon1 < 0) {
      hour = Integer.parseInt(v.trim());
      minute = 1;
      second = 1;
      milli = 0;
    } else {
      hour = Integer.parseInt(v.substring(start, colon1).trim());
      final int colon2 = v.indexOf(':', colon1 + 1);
      if (colon2 < 0) {
        minute = Integer.parseInt(v.substring(colon1 + 1).trim());
        second = 1;
        milli = 0;
      } else {
        minute = Integer.parseInt(v.substring(colon1 + 1, colon2).trim());
        int dot = v.indexOf('.', colon2);
        if (dot < 0) {
          second = Integer.parseInt(v.substring(colon2 + 1).trim());
          milli = 0;
        } else {
          second = Integer.parseInt(v.substring(colon2 + 1, dot).trim());
          milli = Integer.parseInt(v.substring(dot + 1).trim());
        }
      }
    }
    return hour * (int) MILLIS_PER_HOUR
        + minute * (int) MILLIS_PER_MINUTE
        + second * (int) MILLIS_PER_SECOND
        + milli;
  }

  public static long timestampStringToUnixDate(String s) {
    final long d;
    final long t;
    s = s.trim();
    int space = s.indexOf(' ');
    if (space >= 0) {
      d = dateStringToUnixDate(s.substring(0, space));
      t = timeStringToUnixDate(s, space + 1);
    } else {
      d = dateStringToUnixDate(s);
      t = 0;
    }
    return d * MILLIS_PER_DAY + t;
  }

  public static long unixDateExtract(TimeUnitRange range, long date) {
    return julianExtract(range, (int) date + EPOCH_JULIAN);
  }

  private static int julianExtract(TimeUnitRange range, int julian) {
    // this shifts the epoch back to astronomical year -4800 instead of the
    // start of the Christian era in year AD 1 of the proleptic Gregorian
    // calendar.
    int j = julian + 32044;
    int g = j / 146097;
    int dg = j % 146097;
    int c = (dg / 36524 + 1) * 3 / 4;
    int dc = dg - c * 36524;
    int b = dc / 1461;
    int db = dc % 1461;
    int a = (db / 365 + 1) * 3 / 4;
    int da = db - a * 365;

    // integer number of full years elapsed since March 1, 4801 BC
    int y = g * 400 + c * 100 + b * 4 + a;
    // integer number of full months elapsed since the last March 1
    int m = (da * 5 + 308) / 153 - 2;
    // number of days elapsed since day 1 of the month
    int d = da - (m + 4) * 153 / 5 + 122;
    int year = y - 4800 + (m + 2) / 12;
    int month = (m + 2) % 12 + 1;
    int day = d + 1;
    switch (range) {
    case YEAR:
      return year;
    case MONTH:
      return month;
    case DAY:
      return day;
    default:
      throw new AssertionError(range);
    }
  }

  public static long unixTimestampFloor(TimeUnitRange range, long timestamp) {
    int date = (int) (timestamp / MILLIS_PER_DAY);
    final int f = julianDateFloor(range, date + EPOCH_JULIAN, true);
    return (long) f * MILLIS_PER_DAY;
  }

  public static long unixDateFloor(TimeUnitRange range, long date) {
    return julianDateFloor(range, (int) date + EPOCH_JULIAN, true);
  }

  public static long unixTimestampCeil(TimeUnitRange range, long timestamp) {
    int date = (int) (timestamp / MILLIS_PER_DAY);
    final int f = julianDateFloor(range, date + EPOCH_JULIAN, false);
    return (long) f * MILLIS_PER_DAY;
  }

  public static long unixDateCeil(TimeUnitRange range, long date) {
    return julianDateFloor(range, (int) date + EPOCH_JULIAN, true);
  }

  private static int julianDateFloor(TimeUnitRange range, int julian,
      boolean floor) {
    // this shifts the epoch back to astronomical year -4800 instead of the
    // start of the Christian era in year AD 1 of the proleptic Gregorian
    // calendar.
    int j = julian + 32044;
    int g = j / 146097;
    int dg = j % 146097;
    int c = (dg / 36524 + 1) * 3 / 4;
    int dc = dg - c * 36524;
    int b = dc / 1461;
    int db = dc % 1461;
    int a = (db / 365 + 1) * 3 / 4;
    int da = db - a * 365;

    // integer number of full years elapsed since March 1, 4801 BC
    int y = g * 400 + c * 100 + b * 4 + a;
    // integer number of full months elapsed since the last March 1
    int m = (da * 5 + 308) / 153 - 2;
    // number of days elapsed since day 1 of the month
    int d = da - (m + 4) * 153 / 5 + 122;
    int year = y - 4800 + (m + 2) / 12;
    int month = (m + 2) % 12 + 1;
    int day = d + 1;
    switch (range) {
    case YEAR:
      if (!floor && (month > 1 || day > 1)) {
        ++year;
      }
      return ymdToUnixDate(year, 1, 1);
    case MONTH:
      if (!floor && day > 1) {
        ++month;
      }
      return ymdToUnixDate(year, month, 1);
    default:
      throw new AssertionError(range);
    }
  }

  public static int ymdToUnixDate(int year, int month, int day) {
    final int julian = ymdToJulian(year, month, day);
    return julian - EPOCH_JULIAN;
  }

  public static int ymdToJulian(int year, int month, int day) {
    int a = (14 - month) / 12;
    int y = year + 4800 - a;
    int m = month + 12 * a - 3;
    int j = day + (153 * m + 2) / 5
        + 365 * y
        + y / 4
        - y / 100
        + y / 400
        - 32045;
    if (j < 2299161) {
      j = day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
    }
    return j;
  }

  public static long unixTimestamp(int year, int month, int day, int hour,
      int minute, int second) {
    final int date = ymdToUnixDate(year, month, day);
    return (long) date * MILLIS_PER_DAY
        + (long) hour * MILLIS_PER_HOUR
        + (long) minute * MILLIS_PER_MINUTE
        + (long) second * MILLIS_PER_SECOND;
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * Helper class for {@link DateTimeUtils#parsePrecisionDateTimeLiteral}
   */
  public static class PrecisionTime {
    private final Calendar cal;
    private final int precision;

    public PrecisionTime(Calendar cal, int precision) {
      this.cal = cal;
      this.precision = precision;
    }

    public Calendar getCalendar() {
      return cal;
    }

    public int getPrecision() {
      return precision;
    }
  }
}

// End DateTimeUtils.java
