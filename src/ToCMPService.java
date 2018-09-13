package com.cisco.qdreport.qualitydashboard.service;

import com.cisco.qdreport.common.biz.QDReportConstants;
import com.cisco.qdreport.common.tools.CompressTool;
import com.cisco.qdreport.qualitydashboard.dao.jobDao;
import com.cisco.qdreport.qualitydashboard.dao.reportDao;
import com.cisco.qdreport.qualitydashboard.model.Job;
import com.cisco.qdreport.qualitydashboard.model.report;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.cisco.qdreport.qualitydashboard.model.dataProcessPipeLine.repoBean;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
/**
 * @author  Ryan Miao
 */

@Service
public class ToCMPService {
    private final static Logger logger = LoggerFactory.getLogger(ToCMPService.class);
    public  static final String CMPTOKEN_Lab = System.getenv("CMPToken");
    public  static final String SparkToken = System.getenv("SparkToken");
    public  static final String CMPTOKEN_prod = System.getenv("CMPToken_prod");
    public static final String Domain_Prod="https://oneportal.webex.com/";
    public static final String Domain_Lab="https://oneportal.qa.webex.com/";
    public static final String component="oneportal/api/v1/job";
    @Autowired
    jobDao jobDao;
    @Autowired
    reportDao reportDao;

    public String update(String jobName,String buildNumber, String version, String packageName,String environment,String jobOwner,String repoName,String branch){
        String[]jobnamearr=jobName.split(",");
        CloseableHttpResponse response=null;
        StringBuilder SB=new StringBuilder();
        for (int i=0;i<jobnamearr.length;i++){
             response=update2( jobnamearr[i],  buildNumber,  version,  packageName,environment,jobOwner,repoName,branch);
            try {
                SB.append(EntityUtils.toString(response.getEntity()));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return SB.toString();
    }

    private CloseableHttpResponse  update2(String jobName, String buildNumber, String version, String packageName,String environment,String jobOwner,String repoName,String branch){
        logger.info("start to update...");
        String Domain;
        String cmpToken;
        if(environment.equals("Prod")||environment.equals("BTS")){
            Domain=Domain_Prod;
            cmpToken=CMPTOKEN_prod;
        }else{
            Domain=Domain_Lab;
            cmpToken=CMPTOKEN_Lab;
        }
        int id=getIdbyJobName(cmpToken,Domain,jobName);
        String rs=null;
        String tarName=null;
        String jarName;
        String Messages;
        CloseableHttpResponse SaveResponse=null;
        if(id==-1){
            Messages= "**"+jobName+"**"+" job is not exsited on "+"**"+environment+"**"+" environment.";
            rs=Messages;
        }else{
            if(branch.equals("master")){
                tarName = packageName + "-" + version + "-" + buildNumber + ".tar";
            }else{
                tarName = packageName + "-"+branch+"-"+ version + "-" + buildNumber + ".tar";
            }

             jarName=packageName + "-" + version + "-" + "SNAPSHOT.jar";
            logger.info("tarName is :"+tarName);
            String downloadTarUrl;
//https://engci-private-sjc.cisco.com/jenkins/pda/job/SAP%20Pipeline/job/pda-process-spark23/job/master/1/artifact/lakeview-kpi/target/pda-lakeview-kpi-1.0-1.tar
            if(packageName.equals("pda-jmt-batch")){
                downloadTarUrl=QDReportConstants.DOWNLOADURL+branch+"/"+buildNumber+ "/artifact/" +"jmtMiniBatch/target/";
             }else if (packageName.equals("pda-jmt-dashboard")){
                downloadTarUrl=QDReportConstants.DOWNLOADURL+branch+"/"+buildNumber+ "/artifact/" +"jmt/target/";
             }else if(packageName.equals("pda-lakeview-kpi")){
                downloadTarUrl="https://engci-private-sjc.cisco.com/jenkins/pda/job/SAP%20Pipeline/job/pda-process-spark23/job/"+branch+"/"+buildNumber+"/artifact/lakeview-kpi/target/";
             }else{
                String[] projectName = packageName.split("-");
                downloadTarUrl=QDReportConstants.DOWNLOADURL+branch+"/" + buildNumber + "/artifact/" +
                        projectName[projectName.length - 1] + "/target/";
             }
             downLoadAndGetFileUrl(QDReportConstants.FILEPATH, downloadTarUrl, tarName);
             File file=new File(QDReportConstants.FILEPATH + jarName);
             try {
                SaveResponse=update3(cmpToken,Domain, jobName,String.valueOf(id),file,jarName);
                rs=EntityUtils.toString(SaveResponse.getEntity());
                logger.info("the return message after update the jar is :"+rs);
             }catch (Exception e){
                e.printStackTrace();
             }
             String buildLink="https://engci-private-sjc.cisco.com/jenkins/pda/view/SAP/job/SAP%20Pipeline/job/"+repoName+"/job/"+branch+"/"+buildNumber+"/console";
             if(SaveResponse.getStatusLine().getStatusCode()==200) {
                 insertToJob(jobName,jobOwner,tarName,environment);
                 insertToRepo(repoName,packageName,jobName,environment,tarName,buildLink);
                Messages = "**" + tarName + "**" + " is uploaded successfully for " + "**"+jobName+ "**"+ " job on "+
                        "**"+environment+"**"+" environment.";
                logger.info(jobName+"is updated successfully on "+environment+" environment!");
            } else {
                Messages = "**" + tarName + "**" + " failed to upload on,please check it.  [check](https://engci-private-sjc.cisco.com/jenkins/pda/view/SAP/job/jobfordeploy/)";
                logger.info(jobName+"is updated unsuccessfully on "+environment+" environment!");
            }
       }
       logger.info("Messages is:" + Messages.toString());
       sendMessageToSpark(Messages);
        return SaveResponse;
    }

    private CloseableHttpResponse update3(String cmpToken,String Domain,String jobName,String id,File file,String jarName){
        String jobJson="{\"sourcepackage\":\""+jarName+"\""+"}";
        logger.info("jobjson is :"+jobJson+",jar's location :"+file.getAbsolutePath());
        FileBody fileBody = new FileBody(file,ContentType.MULTIPART_FORM_DATA);
        StringBody stringBody=new StringBody(jobJson,ContentType.TEXT_PLAIN);
        HttpEntity requestEntity = MultipartEntityBuilder.create()
                .addPart("file", fileBody)
                .addPart("job",stringBody)
                .build();
        String url=Domain+component+"/"+id;
        CloseableHttpResponse SaveResponse= dopost(url,requestEntity,cmpToken);
        return  SaveResponse;
    }

    public String GenrateConfAndsaveJob(String jobName, String buildNumber, String version, String packageName,
                          String oracleUser, String oraclePass, String teodbUser, String teodbPass,
                          String systoolUser, String systoolPass,String environment) {
        logger.info("start to create job ...");
        String Domain;
        String cmpToken;
        if(environment.equals("Prod")||environment.equals("Bts")){
            Domain=Domain_Prod;
            cmpToken=CMPTOKEN_prod;
        }else{
            Domain=Domain_Lab;
            cmpToken=CMPTOKEN_Lab;
        }
        File configfile = new File(QDReportConstants.FILEPATH + "config.conf");
        try {
            FileWriter fileWriter = new FileWriter(configfile);
            fileWriter.write("pcia {\n" +
                    "  oracle {\n" +
                    "    username = " + oracleUser + "\n" +
                    "    password = " + oraclePass + "\n" +
                    "  }\n" +
                    "  teodb {\n" +
                    "    username =" + teodbUser + "\n" +
                    "    password =" + teodbPass + "\n" +
                    "  }\n" +
                    "  systool {\n" +
                    "    username =" + systoolUser + "\n" +
                    "    password =" + systoolPass + "\n" +
                    "  }\n" +
                    "} ");
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saveandSubmitJob(cmpToken,Domain,jobName, buildNumber, version, packageName, configfile);
    }

    public String saveandSubmitJob(String cmpToken,String Domain,String jobName, String buildNumber, String version, String PackageName, File configfile) {
        int id;
        String createurl=Domain+component;
        if ((id = getIdbyJobName(cmpToken,Domain,jobName)) != -1) {
            String killrs=kill(createurl+"/"+id,cmpToken);
            logger.info("kill api response :"+killrs);
            String deleters=delete(createurl+"/"+id,cmpToken);
            logger.info("delete api response :"+deleters);
        }
        String rs = null;
        StringBuilder SB = new StringBuilder();
        String str;
        InputStream in;
        String tarName = PackageName + "-" + version + "-" + buildNumber + ".tar";
        logger.info("tar name :"+tarName);
        JSONObject jsonObject2;
        try {
                in=new FileInputStream(QDReportConstants.JobName_Argu_ABSOLUTE_PATH+"job.json");
//            in = ClassLoader.getSystemResourceAsStream("json/job.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((str = reader.readLine()) != null) {
                SB.append(str);
            }
            jsonObject2 = JSONObject.fromObject(SB.toString()).getJSONObject(jobName);
            String jobJson = jsonObject2.toString();
            String[] projectName = PackageName.split("-");
            downLoadAndGetFileUrl(QDReportConstants.FILEPATH, QDReportConstants.DOWNLOADURL+ buildNumber + "/artifact/" +
                    projectName[projectName.length - 1] + "/target/", tarName);
            File file=new File(QDReportConstants.FILEPATH + PackageName + "-" + version + "-" + "SNAPSHOT.jar");

            FileBody fileBody = new FileBody(file,ContentType.MULTIPART_FORM_DATA);
            FileBody configfileBody = new FileBody(configfile,ContentType.MULTIPART_FORM_DATA);
            StringBody stringBody=new StringBody(jobJson,ContentType.TEXT_PLAIN);

            HttpEntity requestEntity = MultipartEntityBuilder.create()
                    .addPart("file", fileBody)
                    .addPart("job",stringBody)
                    .addPart("configfile",configfileBody)
                    .build();

           CloseableHttpResponse SaveResponse= dopost(createurl,requestEntity,cmpToken);
            rs=EntityUtils.toString(SaveResponse.getEntity());
            logger.info("create API response:"+ rs);
            String submitid=getId(rs);
            String submitUrl=Domain+component+"/"+submitid+"/submit";
            CloseableHttpResponse SaveandsubmitResponse=submit(submitUrl,cmpToken);
            String Messages;
            if (SaveResponse.getStatusLine().getStatusCode()==200&&SaveandsubmitResponse.getStatusLine().getStatusCode()==200) {
                Messages = "**" + jobName + "**" + " is submitted successfully on the BTS environment and ready to approve.  [approve](https://oneportal.qa.webex.com/oneportal/?entry-point=clops#/job/jobManage).";
                logger.info("save and submit job successfully!!!");
            } else {
                Messages = "**" + jobName + "**" + " failed to deploy,please check your job.  [check](https://engci-private-sjc.cisco.com/jenkins/pda/view/SAP/job/jobfordeploy/)";
            }
            logger.info("Messages to send to spark"+Messages);
            sendMessageToSpark(Messages);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    public String monitor(String id) {
        String rs = null;
        String mess=null;
        try {
            String MonitorURL = Domain_Prod + id + "/status";
            rs = doGet(MonitorURL,CMPTOKEN_prod);
            JSONObject response = JSONObject.fromObject(rs);
            if (response.getString("data").equals("No data found.")) {
                mess = "Job is still waitting to approve";
            } else {
                mess = "Status:" + response.getJSONObject("data").get("status") +
                        "StartTime:" + response.getJSONObject("data").get("starttimestr") +
                        "Lastmodifier:" + response.getJSONObject("data").get("lastmodifier");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mess;
    }

    private HttpResponse sendMessageToSpark(String message) {
        String payload = String.format("{\"roomId\": \"%s\", \"markdown\": \"%s\"}",
                QDReportConstants.PCIA20Room,
                message);
        String payload2= String.format("{\"roomId\": \"%s\", \"markdown\": \"%s\"}",
                QDReportConstants.commonPDAsparkroom,
                message);
        HttpEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        HttpEntity entity2 = new StringEntity(payload2, ContentType.APPLICATION_JSON);
        HttpResponse response = dopost(QDReportConstants.sparkURl, entity,SparkToken);
        HttpResponse response2 = dopost(QDReportConstants.sparkURl, entity2,SparkToken);
        return response;
    }

    public String list(String Domain,String cmpToken) {
        String rs = null;
        try {
            rs = doGet(Domain+component+"/list",cmpToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    private CloseableHttpResponse submit(String submitUrl,String cmpToken) {
        CloseableHttpResponse rs = null;
        HttpEntity httpEntity=MultipartEntityBuilder.create().build();
        try {
            rs = dopost(submitUrl,httpEntity,cmpToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    private String kill(String killURL,String cmpToken) {
        String rs = null;
        try {
            rs = doGet(killURL,cmpToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    private String delete(String deleteUrl,String cmpToken) {
        String rs = null;
        try {
            rs = doDelete(deleteUrl,cmpToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    private  String getId(String result) {
        JSONObject jsonObject = JSONObject.fromObject(result);
        String id = null;
        try {
            id = jsonObject.getJSONObject("data").getString("id").toString();
        } catch (Exception e) {
            logger.info("failed to deploy");
        }
        return id;
    }

        private int getIdbyJobName(String cmpToken,String Domain,String jobName) {
        String rs = list(Domain,cmpToken);
        JSONArray jsonArray = JSONObject.fromObject(rs).getJSONArray("data");
        int id = -1;
        for (int i = 0; i < jsonArray.size(); i++) {
            if (jsonArray.getJSONObject(i).getString("jobname").equals(jobName)) {
                id = Integer.parseInt(jsonArray.getJSONObject(i).getString("id"));
            }
        }
        return id;
    }

    private String downLoadAndGetFileUrl(String localfilePath, String Url, String filename) {
        FileOutputStream fileOut = null;
        try {
            URL url = new URL(Url + filename);
            logger.info("dawnload_url :"+Url + filename);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5 * 1000);
            InputStream inStream = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(inStream);
            fileOut = new FileOutputStream(localfilePath + filename);
            BufferedOutputStream bos = new BufferedOutputStream(fileOut);

            byte[] buf = new byte[4096];
            int length = bis.read(buf);
            while (length != -1) {
                bos.write(buf, 0, length);
                length = bis.read(buf);
            }
            CompressTool.unTar(QDReportConstants.FILEPATH + filename, QDReportConstants.FILEPATH);
            bos.close();
            bis.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localfilePath;
    }

    public String doGet(String url,String cmpToken) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String result = "";
        try {
            httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", cmpToken);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private String doDelete(String url,String cmpToken) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String result=null ;
        try {
            httpClient = HttpClients.createDefault();
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.setHeader("Authorization", cmpToken);
            httpDelete.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpDelete.setHeader("X-Requested-With", "XMLHttpRequest");
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)
                    .setConnectionRequestTimeout(35000)
                    .setSocketTimeout(60000)
                    .build();
            httpDelete.setConfig(requestConfig);
            response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();

            result = EntityUtils.toString(entity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    private   CloseableHttpResponse dopost(String url,HttpEntity requestEntity,String token){
        HttpPost post = new HttpPost(url);
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(900000000).setConnectionRequestTimeout(900000000)
                .setSocketTimeout(900000000).build();
        post.setConfig(requestConfig);
        post.setHeader("Authorization", token);
        post.setEntity(requestEntity);
        CloseableHttpResponse response=null;
        try {
            response = httpClient.execute(post);
        }catch (Exception e){
           e.printStackTrace();
        }
        return response;
    }
    /**job table
     *
     * @param jobname
     * @param jobOwner
     * @param lastestPackVersion
     * @param environment
     */

    public void insertToJob(String jobname,String jobOwner,String lastestPackVersion,String environment){
        Job job=new Job();
        job.setJobName(jobname);
        job.setDeployTime(new java.sql.Timestamp(new java.util.Date().getTime()));
        job.setOwner(jobOwner);
        job.setLastestPackVersion(lastestPackVersion);
        job.setEnvironment(environment);
        jobDao.insertByJobName(job);
    }
    public  List<Job> selectByOwner(String owner){
        List<Job> list=jobDao.selectByOwner(owner);
        return list;
    }

    public List<Job> selectByJobname(String jobName){
        return jobDao.selectByJobname(jobName);
    }

    public List<Job> selectByEnvir(String envir){
        return  jobDao.selectByEnvir(envir);
    }

    public void insertToRepo(String repoName,String projectName,String jobName,String environment,String lastestTar,String  buildLink){
        report report=new report();
        report.setRepoName(repoName);
        report.setDeployTime(new java.sql.Timestamp(new java.util.Date().getTime()));
        report.setJobName(jobName);
        report.setLastestTar(lastestTar);
        report.setEnvironment(environment);
        report.setComponentName(projectName);
        report.setBuildLink(buildLink);
        reportDao.insertByRepoName(report);
    }
    public List<repoBean> selectLastestFromReport(){
        return reportDao.selectLastestFromReport();
    }

}
