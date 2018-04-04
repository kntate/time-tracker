package com.uline.utility;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TimeTracker {

  public static void main(String... args) throws IOException {

    WorkYear year = new FileParser().parseYear();
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(); 
    int weekNum = today.get(woy);
    WorkWeek week = year.getWeek(weekNum);
    
    if (args[0].equals("start")) {
      start(today, week, args[1]);
    } else if (args[0].equals("stop")) {
      stop(today, week, args[1]);
    } else if (args[0].equals("yesterday")) {
      yesterday(today, week, args[1], args[2]);
    }
    
    
    FileUtils.writeLines(new File("/tmp/data1.out"), year.print());
  }
  
  private static void start(LocalDate day, WorkWeek week, String time) throws NumberFormatException, IOException {
    
    week.addDay(day, new WorkDay(day, FileParser.parseTime(time), null));
  }
  
  private static void stop(LocalDate day, WorkWeek week, String time) throws NumberFormatException, IOException {
    WorkDay workDay = week.getDay(day);
    workDay.endDay(FileParser.parseTime(time));
  }
  
  private static void yesterday(LocalDate today, WorkWeek week, String inTime, String outTime) throws NumberFormatException, IOException {
    LocalDate yesterday = today.minusDays(1);
    WorkDay workDay = new WorkDay(yesterday, FileParser.parseTime(inTime),FileParser.parseTime(outTime));
    week.addDay(yesterday, workDay);
  }

  private static class FileParser {
    public WorkYear parseYear() throws IOException {
      List<String> lines = FileUtils.readLines(new File("/tmp/data.out"));
      WorkYear year = new WorkYear();
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
        
        if (StringUtils.startsWithAny(line, "Date", "---", "Week", "   ") || StringUtils.isBlank(line)) {
          continue;
        }
        String[] split = StringUtils.split(line, "|");
        String dateStr = split[0].trim();
        String inTimeStr = split[1].trim();
                
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
        LocalDate parsedDate = LocalDate.parse(dateStr, formatter);
        TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(); 
        weekNum = parsedDate.get(woy);
        
        TimeInstance inTime = parseTime(inTimeStr);
        
        TimeInstance outTime = null;
        if (split.length >2 ){
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

  private static class WorkYear{
    Map<Integer, WorkWeek> weeks = new LinkedHashMap<Integer, WorkWeek>();
    
    public void addWeek(Integer weekNum, WorkWeek week) {
      weeks.put(weekNum, week);
    }
    
    public WorkWeek getWeek(Integer weekNum) {
      WorkWeek week = weeks.get(weekNum);
      if (week==null) {
        week = new WorkWeek(weekNum);
        weeks.put(weekNum, week);
        
      }
      
      return week;
    }
    
    public List<String> print() throws IOException{
      List<String> lines = new ArrayList<String>();
      
      for (Integer weekNum : weeks.keySet()) {
        lines.add("");
        lines.addAll(weeks.get(weekNum).print());
      }
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
    
    public String print() throws IOException {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
      String dateText = date.format(formatter);
      if (outTime == null) {
        return dateText + " |   " + inTime.print() ;
      } else {
        return dateText + " |   " + inTime.print() + "    |   "
            + outTime.print() + "      | " + getHours();
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
    private static final String SEPARATOR = "---------------------------------------------------";
    private static final String HEADER = "Date       |   Time in  |   Time out   | Hours";
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

    public double getHours() {
      double hours = 0;
      for (WorkDay day : days.values()) {
        hours += day.getHours();
      }
      return hours;
    }

    public List<String> print() throws IOException {
      List<String> lines = new ArrayList<String>();
      lines.add("Work Week " + weekNum);
      lines.add(SEPARATOR);
      for (WorkDay day : days.values()) {
        lines.add(day.print());
      }
      lines.add(SEPARATOR);
      StringBuilder totalBuilder = new StringBuilder();
      for (int i = 0; i<40; i++) {
        totalBuilder.append(" ");
      }
      totalBuilder.append("total: " + getHours());
      lines.add(totalBuilder.toString());
      return lines;
    }
  }

  private static class TimeInstance {
     double quarterCount;
     
     int hour, minute;


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
      return "" + hour + ":" + minute;
    }
  }

}
