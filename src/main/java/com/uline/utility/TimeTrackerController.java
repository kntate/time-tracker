package com.uline.utility;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.uline.utility.TimeTracker.FileParser;
import com.uline.utility.TimeTracker.WorkYear;

@RestController
@RequestMapping("/api")
public class TimeTrackerController {


  @GetMapping("/print")
  @ResponseBody
  public String print() throws IOException {
    WorkYear year = getYear();

    year.printToFile();
    return getPrint(year);
  }

  @GetMapping("/yesterday")
  @ResponseBody
  public String yesterday(@RequestParam(name = "inTime", required = false) String inTime,
      @RequestParam(name = "outTime", required = false) String outTime) throws IOException {

    WorkYear year = getYear();

    TimeTracker.yesterday(year.getToday(), year.getCurrentWorkWeek(), inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }

  @GetMapping("/today")
  @ResponseBody
  public String today(@RequestParam(name = "inTime", required = false) String inTime,
      @RequestParam(name = "outTime", required = false) String outTime) throws IOException {

    WorkYear year = getYear();

    TimeTracker.today(year.getToday(), year.getCurrentWorkWeek(), inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }

  @GetMapping("/input")
  @ResponseBody
  public String input(@RequestParam(name = "inTime", required = false) String inTime,
      @RequestParam(name = "outTime", required = false) String outTime,
      @RequestParam(name = "date", required = false) String date) throws IOException {
    WorkYear year = getYear();

    TimeTracker.input(date + "/" + year.getToday().getYear(), year, inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }

  @GetMapping("/start")
  @ResponseBody
  public String start(@RequestParam(name = "inTime", required = false) String inTime,
      @RequestParam(name = "dryRun", required = false) boolean dryRun) throws IOException {

    WorkYear year = getYear();

    TimeTracker.start(year.getToday(), year.getCurrentWorkWeek(), inTime);
    if (!dryRun) {
      year.printToFile();
    }
    return getPrint(year);
  }

  @GetMapping("/end")
  @ResponseBody
  public String end(@RequestParam(name = "outTime", required = false) String outTime,
      @RequestParam(name = "dryRun", required = false) boolean dryRun) throws IOException {
    if (dryRun) {
      return testEnd(outTime);
    }


    WorkYear year = getYear();

    TimeTracker.end(year.getToday(), year.getCurrentWorkWeek(), outTime);
    year.printToFile();
    return getPrint(year);
  }

  @GetMapping("/testEnd")
  @ResponseBody
  public String testEnd(@RequestParam(name = "outTime", required = false) String outTime) throws IOException {

    WorkYear year = getYear();

    TimeTracker.end(year.getToday(), year.getCurrentWorkWeek(), outTime);

    return getPrint(year);
  }

  @GetMapping("/rte")
  @ResponseBody
  public String removeTodayEnd(@RequestParam(name = "outTime", required = true) String outTime,
      @RequestParam(name = "dryRun", required = false) boolean dryRun) throws IOException {

    WorkYear year = getYear();

    TimeTracker.removeTodayEnd(year.getToday(), year.getCurrentWorkWeek());
    if (!dryRun) {
      year.printToFile();
    }
    return getPrint(year);
  }

  @GetMapping("/pto")
  @ResponseBody
  public String pto(@RequestParam(name = "date", required = false) String date,
      @RequestParam(name = "hours", required = false) Integer hours,
      @RequestParam(name = "dryRun", required = false) boolean dryRun) throws IOException {

    WorkYear year = getYear();
	if (hours == null){
		hours = 8;
	}

    TimeTracker.addPto(date + "/" + year.getToday().getYear(), year, hours);
    if (!dryRun) {
      year.printToFile();
    }
    return getPrint(year);
  }
  
  @GetMapping("/holiday")
  @ResponseBody
  public String holiday(@RequestParam(name = "date", required = false) String date,
      @RequestParam(name = "dryRun", required = false) boolean dryRun) throws IOException {

    WorkYear year = getYear();

    TimeTracker.addHoliday(date + "/" + year.getToday().getYear(), year);
    if (!dryRun) {
      year.printToFile();
    }
    return getPrint(year);
  }

  @GetMapping("/remove")
  @ResponseBody
  public String remove(@RequestParam(name = "date", required = false) String date) throws IOException {

    WorkYear year = getYear();

    TimeTracker.removeDay(date + "/" + year.getToday().getYear(), year);
    year.printToFile();
    return getPrint(year);
  }

  private WorkYear getYear() throws IOException {
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    return new FileParser().parseYear(weekNum, today);
  }

  private String getPrint(WorkYear year) {
    StringBuilder strBuild = new StringBuilder();
    for (String string : year.print()) {
      strBuild.append(string).append(System.getProperty("line.separator"));
    }
    return strBuild.toString();
  }

}
