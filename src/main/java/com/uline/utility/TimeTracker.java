package com.uline.utility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TimeTracker {

  public static void main(String... args) throws IOException {

    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    WorkWeek currentWeek = year.getWeek(weekNum);

    if (args.length > 0) {
      if (args[0].equals("start")) {
        start(today, currentWeek, args[1]);
      } else if (args[0].equals("stop")) {
        stop(today, currentWeek, args[1]);
      } else if (args[0].equals("yesterday")) {
        yesterday(today, currentWeek, args[1], args[2]);
      } else if (args[0].equals("today")) {
        today(today, currentWeek, args[1], args[2]);
      } else if (args[0].equals("input")) {
        input(args[1] + "/" + today.getYear(), year, args[2], args[3]);
      }
    }

    List<String> lines = year.print();
    for (String line : lines) {
      System.out.println(line);
    }
    FileUtils.writeLines(new File("/tmp/data-" + today.getYear() + ".out"), lines);
  }

  private static String formatDouble(Double num) {
    DecimalFormat df = new DecimalFormat("#.00");
    String numStr = df.format(num);
    if (num < 10) {
      numStr = "0" + numStr;
    }
    return numStr;
  }
  
  private static String formatDouble4Digit(Double num) {
    DecimalFormat df = new DecimalFormat("#.00");
    String numStr = df.format(num);
    if (num < 10) {
      numStr = " " + numStr;
    } 
    if (num < 100) {
      numStr = " " + numStr;
    } 
    if (num < 1000) {
      numStr = " " + numStr;
    }
    return numStr;
  }

  private static void start(LocalDate day, WorkWeek week, String time) throws NumberFormatException, IOException {

    week.addDay(day, new WorkDay(day, FileParser.parseTime(time), null));
  }

  private static void stop(LocalDate day, WorkWeek week, String time) throws IOException {
    WorkDay workDay = week.getDay(day);
    workDay.endDay(FileParser.parseTime(time));
  }

  private static void yesterday(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    LocalDate yesterday = today.minusDays(1);
    WorkDay workDay = new WorkDay(yesterday, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(yesterday, workDay);
  }

  private static void today(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    WorkDay workDay = new WorkDay(today, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(today, workDay);
  }

  private static void input(String dateStr, WorkYear year, String inTime, String outTime)
      throws NumberFormatException, IOException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    WorkDay workDay = new WorkDay(parsedDate, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(parsedDate, workDay);

    year.addWeek(weekNum, week);
  }

  private static class FileParser {
    public WorkYear parseYear(Integer currentWeekNum) throws IOException {
      List<String> lines = FileUtils.readLines(new File("/tmp/data.out"), Charset.defaultCharset());
      WorkYear year = new WorkYear(currentWeekNum);
      WorkWeek week = null;
      Integer weekNum = 0;
      for (String line : lines) {
        if (StringUtils.startsWith(line, "Work Week")) {
          String[] split = StringUtils.split(line, " ");
          weekNum = Integer.parseInt(split[2]);
          week = new WorkWeek(weekNum);
          year.addWeek(weekNum, week);
          continue;
        }

        if (StringUtils.startsWithAny(line, "Date", "---", "Week", "   ", "Average", "Year", " ") || StringUtils.isBlank(line)) {
          continue;
        }
        String[] split = StringUtils.split(line, "|");
        String dateStr = split[0].trim();
        String inTimeStr = split[1].trim();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
        LocalDate parsedDate = LocalDate.parse(dateStr, formatter);

        TimeInstance inTime = parseTime(inTimeStr);

        TimeInstance outTime = null;
        if (split.length > 2) {
          String outTimeStr = split[2].trim();
          outTime = parseTime(outTimeStr);
        }
        WorkDay day = new WorkDay(parsedDate, inTime, outTime);
        week.addDay(parsedDate, day);
      }


      return year;
    }

    public static TimeInstance parseTime(String timeStr) throws NumberFormatException, IOException {
      String[] splitStr = StringUtils.split(timeStr, ":");
      return new TimeInstance(Integer.parseInt(splitStr[0]), Integer.parseInt(splitStr[1]));
    }
  }

  private static class WorkYear {
    Map<Integer, WorkWeek> weeks = new TreeMap<Integer, WorkWeek>(Collections.reverseOrder());
    Integer currentWeek;

    public WorkYear(Integer currentWeek) {
      this.currentWeek = currentWeek;
    }

    public void addWeek(Integer weekNum, WorkWeek week) {
      weeks.put(weekNum, week);
    }
    
    public WorkWeek getWeek(Integer weekNum) {
      WorkWeek week = weeks.get(weekNum);
      if (week == null) {
        week = new WorkWeek(weekNum);
        weeks.put(weekNum, week);

      }

      return week;
    }

    public List<String> print() {
      List<String> tmpLines = new ArrayList<String>();
      
      String separator = StringUtils.repeat("-", 39);

      Double totalHours = 0d;
      Double totalHoursExcludingCurrent = 0d;
      Integer numWeeks = 0;
      for (Integer weekNum : weeks.keySet()) {
        tmpLines.add("");
        WorkWeek week = weeks.get(weekNum);
        tmpLines.addAll(week.print());
        totalHours += week.getHours();
        if (!week.getWeekNum().equals(currentWeek)) {
          totalHoursExcludingCurrent += week.getHours();
          numWeeks++;
        }
      }

      List<String> lines = new ArrayList<String>();
      lines.add(separator);
      lines.add("Year Total Hours         |  " + formatDouble4Digit(totalHours) + "   |");
      lines.add("Average Hours Per Week   |  " + formatDouble4Digit(totalHoursExcludingCurrent / numWeeks) + "   |");
      lines.add("Year Total Full Weeks    |     " + numWeeks + "      |");
      lines.add(separator);
      lines.add("");
      lines.addAll(tmpLines);
      return lines;
    }
  }

  private static class WorkDay {
    TimeInstance inTime;
    TimeInstance outTime;
    LocalDate date;

    public WorkDay(LocalDate date, TimeInstance inTime, TimeInstance outTime) {
      super();
      this.inTime = inTime;
      this.outTime = outTime;
      this.date = date;
    }

    public void endDay(TimeInstance outTime) {
      this.outTime = outTime;
    }

    public String print() {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
      String dateText = date.format(formatter);
      int pad = 22 - dateText.length();
      for (int i = 0; i < pad; i++) {
        dateText += " ";
      }
      if (outTime == null) {
        return dateText + " |   " + inTime.print();
      } else {
        return dateText + " |   " + inTime.print() + "    |   " + outTime.print() + "     | " + formatDouble(getHours())
            + "  |";
      }
    }

    public double getHours() {
      if (outTime == null) {
        return 0;
      }

      return (outTime.quarterCount - inTime.quarterCount) / 4;
    }
  }

  private static class WorkWeek {
    Map<Integer, WorkDay> days = new TreeMap<Integer, WorkDay>();
    private static final String SEPARATOR = "------------------------------------------------------------";
    Integer weekNum;

    public WorkWeek(Integer weekNum) {
      this.weekNum = weekNum;
    }

    public void addDay(LocalDate date, WorkDay day) {
      days.put(date.getDayOfWeek().getValue(), day);
    }

    public WorkDay getDay(LocalDate date) {
      return days.get(date.getDayOfWeek().getValue());
    }

    public Integer getWeekNum() {
      return weekNum;
    }

    public double getHours() {
      double hours = 0;
      for (WorkDay day : days.values()) {
        hours += day.getHours();
      }
      return hours;
    }

    public List<String> print() {
      List<String> lines = new ArrayList<String>();
      lines.add("");
      lines.add("Work Week " + weekNum);
      lines.add(SEPARATOR);
      for (WorkDay day : days.values()) {
        lines.add(day.print());
      }
      lines.add(SEPARATOR);
      StringBuilder totalBuilder = new StringBuilder("Week Total");
      for (int i = 0; i < 40; i++) {
        totalBuilder.append(" ");
      }
      totalBuilder.append("| " + formatDouble(getHours()) + "  |");
      lines.add(totalBuilder.toString());
      lines.add(SEPARATOR);
      return lines;
    }
  }

  private static class TimeInstance {
    double quarterCount;

    Integer hour;
    Integer minute;


    public TimeInstance(int hour, int minute) throws IOException {
      this.hour = hour;
      this.minute = minute;
      double additionalQuarters = 0;
      if (minute < 8) {
        additionalQuarters = 0;
      } else if (minute > 7 && minute < 23) {
        additionalQuarters = 1;
      } else if (minute > 22 && minute < 38) {
        additionalQuarters = 2;
      } else if (minute > 37 && minute < 53) {
        additionalQuarters = 3;
      } else {
        additionalQuarters = 4;
      }

      quarterCount = hour * 4 + additionalQuarters;

    }

    public String print() {

      return String.format("%s:%s", formatInt(hour), formatInt(minute));
    }

    private String formatInt(Integer num) {
      String numStr = num.toString();
      if (num < 10) {
        numStr = "0" + numStr;
      }
      return numStr;
    }
  }

}
