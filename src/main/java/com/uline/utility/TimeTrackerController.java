package com.uline.utility;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.uline.utility.TimeTracker.FileParser;
import com.uline.utility.TimeTracker.WorkWeek;
import com.uline.utility.TimeTracker.WorkYear;

@Controller
public class TimeTrackerController {

  @GetMapping("/print")
  @ResponseBody
  public String print() throws IOException {
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/yesterday")
  @ResponseBody
  public String yesterday(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    WorkWeek currentWeek = year.getWeek(weekNum);
    
    TimeTracker.yesterday(today, currentWeek, inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/today")
  @ResponseBody
  public String today(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    WorkWeek currentWeek = year.getWeek(weekNum);
    
    TimeTracker.today(today, currentWeek, inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/input")
  @ResponseBody
  public String input(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime,@RequestParam(name="date", required=false) String date) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    
    TimeTracker.input(date + "/" + today.getYear(), year, inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/start")
  @ResponseBody
  public String start(@RequestParam(name="inTime", required=false) String inTime) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    WorkWeek currentWeek = year.getWeek(weekNum);
    
    TimeTracker.start(today, currentWeek, inTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/end")
  @ResponseBody
  public String end(@RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    WorkWeek currentWeek = year.getWeek(weekNum);
    
    TimeTracker.end(today, currentWeek, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/pto")
  @ResponseBody
  public String pto(@RequestParam(name="date", required=false) String date, @RequestParam(name="hours", required=false) Integer hours) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    
    TimeTracker.addPto(date + "/" + today.getYear(), year, hours);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/remove")
  @ResponseBody
  public String remove(@RequestParam(name="date", required=false) String date) throws IOException {
 
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum);
    
    TimeTracker.removeDay(date + "/" + today.getYear(), year);
    year.printToFile();
    return getPrint(year);
  }
  
  private String getPrint(WorkYear year) {
    StringBuilder strBuild = new StringBuilder();
    for (String string : year.print()) {
      strBuild.append(string).append(System.getProperty("line.separator"));
    }
    return strBuild.toString();
  }

}
