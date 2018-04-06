package com.uline.utility;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
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

import javax.activity.InvalidActivityException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TimeTracker {
  
  private static final String DATA_FILE = "/data/time/" + LocalDate.now().getYear() + "/data.txt" ;
   
  public static WorkYear getYear() throws IOException {
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum, today);
    return year;
  }

  public static String formatDouble(Double num) {
    DecimalFormat df = new DecimalFormat("#.00");
    String numStr = df.format(num);
    if (num < 10) {
      numStr = "0" + numStr;
    }
    return numStr;
  }
  
  public static String formatDouble4Digit(Double num) {
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

  public static void start(LocalDate day, WorkWeek week, String time) throws NumberFormatException, IOException {

    week.addDay(day, new WorkDay(day, FileParser.parseTime(time), null));
  }

  public static void end(LocalDate day, WorkWeek week, String time) throws IOException {
    WorkDay workDay = week.getDay(day);
    if (workDay == null) {
      throw new InvalidActivityException("Error, cannot end a day that hasn't been started!");
    }
    workDay.endDay(FileParser.parseTime(time));
  }

  public static void yesterday(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    LocalDate yesterday = today.minusDays(1);
    WorkDay workDay = new WorkDay(yesterday, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(yesterday, workDay);
  }

  public static void today(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    WorkDay workDay = new WorkDay(today, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(today, workDay);
  }

  public static void input(String dateStr, WorkYear year, String inTime, String outTime)
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
  
  public static void removeDay(String dateStr, WorkYear year) throws IOException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    week.days.remove(parsedDate.getDayOfWeek().getValue());

  }
  
  public static void addPto(String dateStr, WorkYear year, Integer hours) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    WorkDay workDay = new WorkDay(parsedDate, new WorkInterval(hours.doubleValue()));
    week.addDay(parsedDate, workDay);

    year.addWeek(weekNum, week);
  }

  public static class FileParser {
    public WorkYear parseYear(Integer currentWeekNum, LocalDate today) throws IOException {
      FileUtils.copyFile(new File(DATA_FILE), new File(DATA_FILE + ".bak"));
      List<String> lines = FileUtils.readLines(new File(DATA_FILE), Charset.defaultCharset());
      WorkYear year = new WorkYear(currentWeekNum, today);
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

  static class WorkYear {
    Map<Integer, WorkWeek> weeks = new TreeMap<Integer, WorkWeek>(Collections.reverseOrder());
    Integer currentWeekNum;
    LocalDate today;

    public WorkYear(Integer currentWeek, LocalDate today) {
      this.currentWeekNum = currentWeek;
      this.today = today;
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
    
    public WorkWeek getCurrentWorkWeek() {
      return getWeek(getCurrentWeekNum());
    }
    
    public Integer getCurrentWeekNum() {
      return currentWeekNum;
    }

    public LocalDate getToday() {
      return today;
    }

    public void printToFile() throws IOException {
      List<String> lines = print();
      FileUtils.writeLines(new File(DATA_FILE), lines);
    }

    public List<String> print() {
      List<String> tmpLines = new ArrayList<String>();
      
      String separator = StringUtils.repeat("-", 39);

      Double totalHours = 0d;
      Double totalHoursExcludingCurrent = 0d;
      Integer numWeeks = 0;
      for (WorkWeek week : weeks.values()) {
        tmpLines.add("");
        tmpLines.addAll(week.print());
        totalHours += week.getHours();
        if (!week.getWeekNum().equals(getCurrentWeekNum()) || week.hasCompletedDay(DayOfWeek.FRIDAY)) {
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
  
  private static class WorkInterval{
    TimeInstance inTime;
    TimeInstance outTime;
    boolean isPto;
    Double hours = 0d;
    boolean isOpen;
        
    public WorkInterval(TimeInstance inTime, TimeInstance outTime) {
      super();
      this.inTime = inTime;
      this.outTime = outTime;
      isPto = false;
      
      if (outTime == null) {
        isOpen = true;
      } else {
        isOpen = false;
        hours = (outTime.quarterCount - inTime.quarterCount) / 4;
      }
    }
    
    public WorkInterval(Double hours) {
      this.hours = hours;
      isPto = true;
      isOpen = false;
    }
    
    public void close(TimeInstance outTime) {
      this.outTime = outTime;
      isOpen = false;
      hours = (outTime.quarterCount - inTime.quarterCount) / 4;
    }
    
    public Double getHours() {
      return hours;
    }
        
  }

  private static class WorkDay {

    LocalDate date;
    List<WorkInterval> workIntervals = new ArrayList<WorkInterval>();

    public WorkDay(LocalDate date, TimeInstance inTime, TimeInstance outTime) {
      workIntervals.add(new WorkInterval(inTime, outTime));
      this.date = date;
    }
    
    public WorkDay(LocalDate date, WorkInterval ptoTime) {
      workIntervals.add(ptoTime);
      this.date = date;
    }
    
    public void addIntervals(List<WorkInterval> intervals) {
      workIntervals.addAll(intervals);
    }

    public List<WorkInterval> getWorkIntervals() {
      return workIntervals;
    }
    
    public boolean isFinished() {
      if (workIntervals.isEmpty()) {
        return false;
      }
      for (WorkInterval workInterval : workIntervals) {
        if (workInterval.isOpen) {
          return false;
        }
      }
      
      return true;
    }

    public void endDay(TimeInstance outTime) {
      WorkInterval workInterval = workIntervals.get(workIntervals.size() - 1);
      workInterval.close(outTime);
    }

    public List<String> print() {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
      String dateText = date.format(formatter);
      dateText += StringUtils.repeat(" ", 22 - dateText.length());
      
      List<String> lines = new ArrayList<String>();
      for (WorkInterval workInterval : workIntervals) {
        String line;
        if (workInterval.isOpen) {
          line =  dateText + " |   " + workInterval.inTime.print();
        } else if (workInterval.isPto){
          line = dateText + " |    PTO     |    PTO      | "
              + formatDouble(workInterval.getHours()) + "  |";
        }   else {
          line = dateText + " |   " + workInterval.inTime.print() + "    |   " + workInterval.outTime.print() + "     | "
              + formatDouble(workInterval.getHours()) + "  |";
        }
        lines.add(line);
        dateText = StringUtils.repeat(" ", dateText.length());
      }
      
      return lines;
    }
    
    public double getHours() {
      double hours = 0;
      for (WorkInterval workInterval : workIntervals) {
        hours += workInterval.getHours();
      }
      return hours;
    }

  }

  public static class WorkWeek {
    Map<Integer, WorkDay> days = new TreeMap<Integer, WorkDay>();
    private static final String SEPARATOR = "------------------------------------------------------------";
    Integer weekNum;

    public WorkWeek(Integer weekNum) {
      this.weekNum = weekNum;
    }

    public void addDay(LocalDate date, WorkDay day) {
      WorkDay workDay = getDay(date);
      if (workDay == null) {
        days.put(date.getDayOfWeek().getValue(), day);
      } else {
        workDay.addIntervals(day.getWorkIntervals());
      }
    }

    public boolean hasCompletedDay(DayOfWeek dayOfWeek) {
      WorkDay day = days.get(dayOfWeek.getValue());
      if (day == null) {
        return false;
      }
      return day.isFinished();
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
        lines.addAll(day.print());
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
