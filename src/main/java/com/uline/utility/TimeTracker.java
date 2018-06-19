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

  private static final String DATA_FILE = "/data/time/" + LocalDate.now().getYear() + "/data.txt";
  private static final String DATE_FORMAT = "MM/dd/yyyy";
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

  public static WorkYear getYear() throws IOException {
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    return new FileParser().parseYear(weekNum, today);
  }

  public static String formatDouble(Double num) {
    DecimalFormat df = new DecimalFormat("#.00");
    String numStr = df.format(num);
    if (num < 10) {
      numStr = "0" + numStr;
    }
    if (num < 1) {
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

  public static void removeTodayEnd(LocalDate day, WorkWeek week) throws IOException {
    WorkDay workDay = week.getDay(day);
    if (workDay == null) {
      throw new InvalidActivityException("Error, cannot end a day that hasn't been started!");
    }
    workDay.removeLastClockout();

  }

  public static void yesterday(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    LocalDate yesterday = today.minusDays(1);
    
    if (StringUtils.isBlank(outTime)) {
      throw new IllegalArgumentException("Error, outTime is required!");
    }
    
    if (StringUtils.isBlank(inTime)) {
      WorkDay yesterdayWorkDay = week.getDay(yesterday);
      yesterdayWorkDay.endDay(FileParser.parseTime(outTime));
    } else {
      WorkDay workDay = new WorkDay(yesterday, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
      week.addDay(yesterday, workDay);
    }
    
  }

  public static void today(LocalDate today, WorkWeek week, String inTime, String outTime) throws IOException {
    WorkDay workDay = new WorkDay(today, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(today, workDay);
  }

  public static void input(String dateStr, WorkYear year, String inTime, String outTime) throws IOException {

    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    WorkDay workDay = new WorkDay(parsedDate, FileParser.parseTime(inTime), FileParser.parseTime(outTime));
    week.addDay(parsedDate, workDay);

    year.addWeek(weekNum, week);
  }

  public static void removeDay(String dateStr, WorkYear year) throws IOException {
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    week.days.remove(parsedDate.getDayOfWeek().getValue());

  }

  public static void addPto(String dateStr, WorkYear year, Integer hours) {
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    WorkDay workDay = new WorkDay(parsedDate, new WorkInterval(hours.doubleValue()));
    week.addDay(parsedDate, workDay);

    year.addWeek(weekNum, week);
  }

  public static void addHoliday(String dateStr, WorkYear year) {
    LocalDate parsedDate = LocalDate.parse(dateStr.trim(), formatter);
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    Integer weekNum = parsedDate.get(woy);
    WorkWeek week = year.getWeek(weekNum);

    WorkDay workDay = new WorkDay(parsedDate, new WorkInterval());
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

        if (StringUtils.startsWithAny(line, "Date", "---", "Week", "   ", "Average", "Year", " ", "Expected", "Ahead",
            "Behind", "Remaining", "Hours") || StringUtils.isBlank(line)) {
          continue;
        }
        String[] split = StringUtils.split(line, "|");
        String dateStr = split[0].trim();
        String inTimeStr = split[1].trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE MM/dd/yyyy");
        LocalDate parsedDate = LocalDate.parse(dateStr, formatter);


        if (inTimeStr.equals("HOLIDAY")) {
          WorkDay workDay = new WorkDay(parsedDate, new WorkInterval());
          week.addDay(parsedDate, workDay);
        } else if (inTimeStr.equals("PTO")) {
          WorkDay workDay = new WorkDay(parsedDate, new WorkInterval(new Double(split[3])));
          week.addDay(parsedDate, workDay);
        } else {

          TimeInstance inTime = parseTime(inTimeStr);

          TimeInstance outTime = null;
          if (split.length > 2) {
            String outTimeStr = split[2].trim();
            outTime = parseTime(outTimeStr);
          }
          WorkDay day = new WorkDay(parsedDate, inTime, outTime);
          if (week != null) {
            week.addDay(parsedDate, day);
          }
        }
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
      Double currentWeekHours = 0d;
      Integer numWeeks = 0;
      boolean isCurWeekOpen = true;
      for (WorkWeek week : weeks.values()) {
        boolean isOpenCurWeek = true;
        if (!week.getWeekNum().equals(getCurrentWeekNum()) || week.hasCompletedDay(DayOfWeek.FRIDAY)) {
          totalHoursExcludingCurrent += week.getHours();
          numWeeks++;
          isOpenCurWeek = false;
        }
        
        if (week.getWeekNum().equals(getCurrentWeekNum()) && week.hasCompletedDay(DayOfWeek.FRIDAY)){
          isCurWeekOpen = false;
          currentWeekHours =  week.getHours();
        }
        
        if (week.getWeekNum().equals(getCurrentWeekNum())){
          currentWeekHours =  week.getHours();
        }
        
        tmpLines.add("");
        tmpLines.addAll(week.print(isOpenCurWeek));
        totalHours += week.getHours();
      }


      List<String> headerLines = new ArrayList<String>();
      headerLines.add(separator);
      headerLines.add("Year Total Hours         |  " + formatDouble4Digit(totalHours) + "   |");
      if (!isCurWeekOpen) {
        headerLines.add("Hours over 45 for Year   |  " + formatDouble4Digit(totalHours - 45 * numWeeks) + "   |");
      } else {
        headerLines.add("Hours over 45 for Year   |  " + formatDouble4Digit((totalHours - currentWeekHours) - 45 * (numWeeks)) + "   |");
      }
      headerLines
          .add("Average Hours Per Week   |  " + formatDouble4Digit(totalHoursExcludingCurrent / numWeeks) + "   |");
      if (numWeeks < 10) {
        headerLines.add("Year Total Full Weeks    |     " + numWeeks + "      |");
      } else {
        headerLines.add("Year Total Full Weeks    |    " + numWeeks + "      |");
      }
      headerLines.add(separator);
      headerLines.add("");

      tmpLines.add(" ");
      tmpLines.add(" ");
      tmpLines.add(" ");
      tmpLines.addAll(headerLines);
      WorkWeek currentWeek = weeks.get(getCurrentWeekNum());
      if (currentWeek != null) {
        tmpLines.addAll(currentWeek.print(true));
      }


      List<String> lines = new ArrayList<String>();
      lines.addAll(headerLines);
      lines.addAll(tmpLines);


      return lines;
    }
  }

  private static class WorkInterval {
    TimeInstance inTime;
    TimeInstance outTime;
    boolean isPto;
    Double hours = 0d;
    boolean isOpen;
    boolean isHoliday;


    public WorkInterval(TimeInstance inTime, TimeInstance outTime) {
      super();
      this.inTime = inTime;
      this.outTime = outTime;
      isPto = false;
      isHoliday = false;

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

    public WorkInterval() {
      this.hours = 8d;
      isPto = false;
      isOpen = false;
      isHoliday = true;
    }

    public void open() {
      this.outTime = null;
      this.isOpen = true;
      this.hours = 0d;
    }

    public void close(TimeInstance outTime) {
      this.outTime = outTime;
      isOpen = false;
      hours = (outTime.quarterCount - inTime.quarterCount) / 4;
    }

    public Double getHours() {
      return hours;
    }

    public boolean isHoliday() {
      return isHoliday;
    }

  }

  private static class WorkDay {

    LocalDate date;
    List<WorkInterval> workIntervals = new ArrayList<WorkInterval>();

    public WorkDay(LocalDate date, TimeInstance inTime, TimeInstance outTime) {
      workIntervals.add(new WorkInterval(inTime, outTime));
      this.date = date;
    }

    public void removeLastClockout() {
      int size = workIntervals.size();
      if (workIntervals.isEmpty()) {
        throw new RuntimeException("No day found for today!");
      }
      WorkInterval lastInterval = workIntervals.get(size - 1);
      lastInterval.open();
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
          line = dateText + " |   " + workInterval.inTime.print();
        } else if (workInterval.isPto) {
          line = dateText + " |    PTO     |    PTO      | " + formatDouble(workInterval.getHours()) + "  |";
        } else if (workInterval.isHoliday()) {
          line = dateText + " |  HOLIDAY   |  HOLIDAY    | " + formatDouble(workInterval.getHours()) + "  |";
        } else {
          line = dateText + " |   " + workInterval.inTime.print() + "    |   " + workInterval.outTime.print()
              + "     | " + formatDouble(workInterval.getHours()) + "  |";
        }
        lines.add(line);
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

    public List<String> print(boolean isOpenCurWeek) {
      List<String> lines = new ArrayList<String>();
      lines.add("");
      if (isOpenCurWeek) {
        lines.add("Work Week " + weekNum + " - Current Week");
      } else {
        lines.add("Work Week " + weekNum);
      }
      lines.add(SEPARATOR);
      double numCompletedDays = 0;
      for (WorkDay day : days.values()) {
        lines.addAll(day.print());
        if (day.isFinished()) {
          numCompletedDays++;
        }
      }
      lines.add(SEPARATOR);
      StringBuilder totalBuilder = fill("Week Total", 40);
      totalBuilder.append("| " + formatDouble(getHours()) + "  |");
      lines.add(totalBuilder.toString());
      lines.add(SEPARATOR);
      if (isOpenCurWeek && getHours() != 0d && getHours() < 46d) {
        StringBuilder expected = fill("Expected Total", 36);
        double expectedHours = numCompletedDays * 9.25;
        expected.append("| " + formatDouble(expectedHours) + "  |");
        lines.add(expected.toString());
        lines.add(SEPARATOR);

        StringBuilder remaining = fill("Remaining", 41);
        remaining.append("| " + formatDouble(46d - getHours()) + "  |");
        lines.add(remaining.toString());
        lines.add(SEPARATOR);

        boolean isAhead = expectedHours < getHours();
        StringBuilder ahead = fill(isAhead ? "Ahead " : "Behind", 44);
        if (isAhead) {
          ahead.append("| " + formatDouble(getHours() - expectedHours) + "  |");
        } else {
          ahead.append("| " + formatDouble(expectedHours - getHours()) + "  |");
        }

        lines.add(ahead.toString());
        lines.add(SEPARATOR);
      }
      return lines;
    }

    private StringBuilder fill(String label, int size) {
      StringBuilder totalBuilder = new StringBuilder(label);
      for (int i = 0; i < size; i++) {
        totalBuilder.append(" ");
      }

      return totalBuilder;
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

    public double getQuarterCount() {
      return quarterCount;
    }

    public void setQuarterCount(double quarterCount) {
      this.quarterCount = quarterCount;
    }

    public Integer getHour() {
      return hour;
    }

    public void setHour(Integer hour) {
      this.hour = hour;
    }

    public Integer getMinute() {
      return minute;
    }

    public void setMinute(Integer minute) {
      this.minute = minute;
    }
  }

}
