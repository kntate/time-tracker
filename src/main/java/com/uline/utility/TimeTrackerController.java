package com.uline.utility;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.uline.utility.TimeTracker.FileParser;
import com.uline.utility.TimeTracker.WorkWeek;
import com.uline.utility.TimeTracker.WorkYear;

@RestController
@RequestMapping("/api/auth")
public class TimeTrackerController {
  
  @PostMapping(
      path = "/login",
      produces = {"application/json"}
    )
  @CrossOrigin
  public AuthResponse login(@RequestHeader("authorization") String authInfo, final HttpServletResponse response) throws IOException, InterruptedException {

       
    System.out.println("Here in login with info: " + authInfo);
    if (StringUtils.contains(authInfo, " ")) {
      authInfo = StringUtils.substringAfter(authInfo, " ");
    }
    System.out.println("Here in login with info: " + authInfo);
    byte[] decodedByte = Base64.getDecoder().decode(authInfo);
    String decodedString = new String(decodedByte);
    String user = StringUtils.substringBefore(decodedString, ":");
    String pw = StringUtils.substringAfter(decodedString, ":");
    
    if (StringUtils.equalsIgnoreCase(user, "asdf")) {
      return new AuthResponse();
    }
    
    System.out.println("user: " + user);
    System.out.println("pw: " + pw);
    AuthResponse authResponse = new AuthResponse();
    authResponse.setAs400Id("as400Id");
    authResponse.setBranchCode("brch" + user);
    authResponse.setFullName(user);
    
    if (StringUtils.equalsIgnoreCase(user, "Kevin")) {
      Role role = new Role();
      role.setAuthority("super");
      authResponse.getRoles().add(role);
    } else {
      Role role = new Role();
      role.setAuthority("user");
      authResponse.getRoles().add(role);
    }
    
    Role role1 = new Role();
    role1.setAuthority("quotes");
    //authResponse.getRoles().add(role1);
//    System.out.println("about to sleep");
//    Thread.sleep(5000);
//    System.out.println("done sleeping");
//    
    response.addHeader("token", "1232");
    
    return authResponse;
  }
  
  private static class Role {
    String authority;

    public String getAuthority() {
      return authority;
    }

    public void setAuthority(String authority) {
      this.authority = authority;
    }
    
  }
  
  private static class AuthResponse{
    private String userId;
    private String branchCode;
    private String department;
    private String as400Id;
    private List<Role> roles = new ArrayList<Role>();
    private String country;
    private String email;
    private String extension;
    private String fullName;
    private String warehouse;
    private String personNumber;
    //expiration: Date;
    public String getUserId() {
      return userId;
    }
    public void setUserId(String userId) {
      this.userId = userId;
    }
    public String getBranchCode() {
      return branchCode;
    }
    public void setBranchCode(String branchCode) {
      this.branchCode = branchCode;
    }
    public String getDepartment() {
      return department;
    }
    public void setDepartment(String department) {
      this.department = department;
    }
    public String getAs400Id() {
      return as400Id;
    }
    public void setAs400Id(String as400Id) {
      this.as400Id = as400Id;
    }
    public String getCountry() {
      return country;
    }
    public void setCountry(String country) {
      this.country = country;
    }
    public String getEmail() {
      return email;
    }
    public void setEmail(String email) {
      this.email = email;
    }
    public String getExtension() {
      return extension;
    }
    public void setExtension(String extension) {
      this.extension = extension;
    }
    public String getFullName() {
      return fullName;
    }
    public void setFullName(String fullName) {
      this.fullName = fullName;
    }
    public String getWarehouse() {
      return warehouse;
    }
    public void setWarehouse(String warehouse) {
      this.warehouse = warehouse;
    }
    public String getPersonNumber() {
      return personNumber;
    }
    public void setPersonNumber(String personNumber) {
      this.personNumber = personNumber;
    }
    public List<Role> getRoles() {
      return roles;
    }
    public void setRoles(List<Role> roles) {
      this.roles = roles;
    }
        
  }
  
  

  @GetMapping("/print")
  @ResponseBody
  public String print() throws IOException {
    WorkYear year = getYear();
    
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/yesterday")
  @ResponseBody
  public String yesterday(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.yesterday(year.getToday(), year.getCurrentWorkWeek(), inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/today")
  @ResponseBody
  public String today(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.today(year.getToday(), year.getCurrentWorkWeek(), inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/input")
  @ResponseBody
  public String input(@RequestParam(name="inTime", required=false) String inTime, @RequestParam(name="outTime", required=false) String outTime,@RequestParam(name="date", required=false) String date) throws IOException {
    WorkYear year = getYear();
    
    TimeTracker.input(date + "/" + year.getToday().getYear(), year, inTime, outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/start")
  @ResponseBody
  public String start(@RequestParam(name="inTime", required=false) String inTime) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.start(year.getToday(), year.getCurrentWorkWeek(), inTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/end")
  @ResponseBody
  public String end(@RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.end(year.getToday(), year.getCurrentWorkWeek(), outTime);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/rte")
  @ResponseBody
  public String removeTodayEnd(@RequestParam(name="outTime", required=false) String outTime) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.removeTodayEnd(year.getToday(), year.getCurrentWorkWeek());
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/pto")
  @ResponseBody
  public String pto(@RequestParam(name="date", required=false) String date, @RequestParam(name="hours", required=false) Integer hours) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.addPto(date + "/" + year.getToday().getYear(), year, hours);
    year.printToFile();
    return getPrint(year);
  }
  
  @GetMapping("/remove")
  @ResponseBody
  public String remove(@RequestParam(name="date", required=false) String date) throws IOException {
 
    WorkYear year = getYear();
    
    TimeTracker.removeDay(date + "/" + year.getToday().getYear(), year);
    year.printToFile();
    return getPrint(year);
  }
  
  private WorkYear getYear() throws IOException {
    LocalDate today = LocalDate.now();
    TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
    int weekNum = today.get(woy);
    WorkYear year = new FileParser().parseYear(weekNum, today);
    return year;
  }
  
  private String getPrint(WorkYear year) {
    StringBuilder strBuild = new StringBuilder();
    for (String string : year.print()) {
      strBuild.append(string).append(System.getProperty("line.separator"));
    }
    return strBuild.toString();
  }

}
