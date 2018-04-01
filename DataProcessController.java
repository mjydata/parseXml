package com.cisco.qdreport.dataProcess.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.cisco.qdreport.dataProcess.service.dataService;
@RestController
public class DataProcessController {
    /**
     *
     *  http://pda-report.cisco.com:8443/utCoverage/SAP/dataAnalysis
     */

    @Autowired
    dataService dataService;
    @RequestMapping(value="/utCoverage/{projectName}/{componentName}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public String generateReport(@PathVariable("projectName" )String projectName,
                                 @PathVariable("componentName")String componentName){
        return dataService.generateJson(projectName, componentName);
    }
    @RequestMapping("/utCoverage/{projectName}")
    public String generateAllReport(@PathVariable("projectName")String projectName){
        return dataService.generateAllJson(projectName);
    }
}
