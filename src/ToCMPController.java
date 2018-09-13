package com.cisco.qdreport.qualitydashboard.controller;

import com.cisco.qdreport.qualitydashboard.model.Job;
import com.cisco.qdreport.qualitydashboard.model.dataProcessPipeLine.repoBean;
import com.cisco.qdreport.qualitydashboard.service.ToCMPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.cisco.qdreport.qualitydashboard.model.report;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

@RestController
public class ToCMPController {
    private final static Logger logger = LoggerFactory.getLogger(ToCMPController.class);
    @Autowired
    ToCMPService toCMPService;
    @RequestMapping(value="/job/{id}/status",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public String kill(@PathVariable("id" )String id,
                     HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "*");
        return toCMPService.monitor(id);
    }

    //create api is invaild now
    @RequestMapping(value="/job",method = RequestMethod.POST)
    public String create(@RequestParam(value ="jobName",required = true)String jobName,
                         @RequestParam(value ="buildNumber",required = true)String buildNumber,
                         @RequestParam(value ="version",required = true) String  version,
                         @RequestParam(value ="packageName",required = true) String  packageName,
                         @RequestParam(value ="oracleUser",required = true) String  oracleUser,
                         @RequestParam(value ="oraclePass",required = true) String  oraclePass,
                         @RequestParam(value ="teodbUser",required = true) String  teodbUser,
                         @RequestParam(value ="teodbPass",required = true) String  teodbPass,
                         @RequestParam(value ="systoolUser",required = true) String  systoolUser,
                         @RequestParam(value ="systoolPass",required = true) String  systoolPass,
                         @RequestParam(value ="environment",required = true) String  environment
                       ){
        logger.info(jobName,buildNumber,version,packageName);
        return toCMPService.GenrateConfAndsaveJob(jobName,buildNumber,version,packageName,
                oracleUser,oraclePass,teodbUser,teodbPass,systoolUser,systoolPass,environment);
    }

    @RequestMapping(value="/job/update",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(@RequestParam(value ="jobName",required = true)String jobName,
                                        @RequestParam(value ="buildNumber",required = true)String buildNumber,
                                        @RequestParam(value ="version",required = true)String version,
                                        @RequestParam(value ="packageName",required = true)String packageName,
                                        @RequestParam(value ="environment",required = true)String environment,
                                        @RequestParam(value ="jobOwner",required = true)String jobOwner,
                                        @RequestParam(value ="branch",required = true)String branch,
                                        @RequestParam(value ="repoName",required = true)String repoName
    ){
        return toCMPService.update(jobName,buildNumber,version,packageName,environment,jobOwner,repoName,branch);
    }
    @RequestMapping(value="/pipelineMetrics/insertToJob",method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public void insertTojob(@RequestParam(value ="jobName",required = true)String jobName,
                         @RequestParam(value ="environment",required = true)String environment,
                         @RequestParam(value ="jobOwner",required = true)String jobOwner,
                         @RequestParam(value ="lastestPackVersion",required = true)String lastestPackVersion

    ){
         toCMPService.insertToJob(jobName,jobOwner,lastestPackVersion,environment);
    }

    /**
     * requset url:http://10.224.183.16/pipelineMetrics/getJob?owner=bindu
     * @param owner
     * @param jobName
     * @param envir
     * @param response
     * @return
     */
    @RequestMapping(value="/pipelineMetrics/getJob",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Job> selectByOwner(
            @RequestParam(value ="owner",required = false)String owner,
            @RequestParam(value ="jobName",required = false)String jobName,
                    @RequestParam(value ="envir",required = false)String envir,
                       HttpServletResponse response){

        response.setHeader("Access-Control-Allow-Origin", "*");
        if (owner!=null &&jobName ==null&&envir ==null){
            return toCMPService.selectByOwner(owner);
        }
        else  if (jobName!=null &&owner ==null&&envir ==null){
            return toCMPService.selectByJobname(jobName);
        }else  if (envir!=null &&owner ==null&&jobName ==null){
            return toCMPService.selectByEnvir(envir);
        }else {
            return  null;
        }
    }
    /**
     * request url: http://10.224.183.16/pipelineMetrics/getAll
     * @param response
     * @return
     */
    @RequestMapping(value="/pipelineMetrics/getAll",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List<repoBean> selectLastestFromReport(HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "*");
       return toCMPService.selectLastestFromReport();
    }

}

