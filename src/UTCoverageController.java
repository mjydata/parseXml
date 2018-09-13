package com.cisco.qdreport.qualitydashboard.controller.utCoverage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.cisco.qdreport.qualitydashboard.service.utCoverage.dataService;

import javax.servlet.http.HttpServletResponse;

@RestController
public class UTCoverageController {
    /**
     *
     *  10.224.183.16/utCoverage/SAP/dataAnalysis
     */

    @Autowired
    dataService dataService;
    @RequestMapping(value="/utCoverage/{projectName}/{componentName}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public String generateReport(@PathVariable("projectName" )String projectName,
                                 @PathVariable("componentName")String componentName, HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "*");
        return dataService.generateJson(projectName, componentName);
    }
    @RequestMapping("/utCoverage/{projectName}")
    public String generateAllReport(@PathVariable("projectName")String projectName, HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "*");
        return dataService.generateAllJson(projectName);
    }
}