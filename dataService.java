package com.cisco.qdreport.dataProcess.service;

import java.io.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.cisco.qdreport.common.biz.QDReportConstants;
import com.cisco.qdreport.common.tools.CompressTool;
import com.cisco.qdreport.dataProcess.model.SAPDataProcessBean;
import com.cisco.qdreport.dataProcess.model.componentUTCoverageBean;
import com.cisco.qdreport.dataProcess.model.utCoverageBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class dataService {

    private final String direct=QDReportConstants.ROOTDICETORY;
    private  final   String branchName=QDReportConstants.BRANCHNAME;
    private final String version="1.0";


    /**
     * scan file
     */

    private   File  findXML(String path) {
        File fileXML=null;
        LinkedList<File> list = new LinkedList();
        File dir = new File(path);
        File file[] = dir.listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory())
                list.add(file[i]);
            else
                System.out.println(file[i].getAbsolutePath());
        }
        File tmp;
        while (!list.isEmpty()) {
            tmp = list.removeFirst();
            if (tmp.isDirectory()) {
                file = tmp.listFiles();
                if (file == null)
                    continue;
                for (int i = 0; i < file.length; i++) {
                    if (file[i].isDirectory())
                        list.add(file[i]);
                    else
                        System.out.println(file[i].getAbsolutePath());
                    fileXML=file[i];

                }
            } else {
                System.out.println(tmp.getAbsolutePath());
            }
        }
        return fileXML;
    }
    /**
     *
     * @param xmlFile
     * @return
     */
    private  utCoverageBean getUtCoverageBeanFromXMl(File xmlFile) {
        utCoverageBean utCoverageBean=new utCoverageBean();
        double coverPercent;
        double missed;
        double covered;
        Element root;
        Document document;
        SAXReader saxReader;
        if(xmlFile!=null){
            try {
                saxReader = new SAXReader();
                try {
                    saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                document = saxReader.read(xmlFile);
                System.out.println(document);
                root = document.getRootElement();
                System.out.println(root.getName());
                List subNodes = root.elements("counter");
                for (Iterator it = subNodes.iterator(); it.hasNext(); ) {
                    Element elm = (Element) it.next();
                    String value = elm.attributeValue("type");
                    if (!(elm.attributeValue("missed").isEmpty() && elm.attributeValue("covered").isEmpty())) {
                        if (value.equals("LINE")) {
                            missed = Integer.parseInt(elm.attributeValue("missed"));
                            covered = Integer.parseInt(elm.attributeValue("covered"));
                            coverPercent = covered /(covered+missed) ;
                            NumberFormat nf = NumberFormat.getPercentInstance();
                            nf.setMinimumFractionDigits(2);
                            nf.setRoundingMode(RoundingMode.HALF_UP);
                            String percent = nf.format(coverPercent);

                            utCoverageBean.setBranchName(branchName);
                            utCoverageBean.setCoverRate(percent);
                            System.out.println((int)missed);
                            utCoverageBean.setNoCaverageLines((int)missed);
                            utCoverageBean.setCoveredLines((int)covered);
                        }
                    }
                }
            } catch (DocumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            System.out.println("There is no xml file ");
        }
        return utCoverageBean;
    }
    private    List<utCoverageBean> getUtCoverageBeanList(utCoverageBean utCoverageBean){
        List<utCoverageBean> utCoverageBeanList=new ArrayList<utCoverageBean>();
        utCoverageBeanList.add(utCoverageBean);
        return utCoverageBeanList;
    }

    private   componentUTCoverageBean getComponentUTCoverageBean(List<utCoverageBean> utCoverageBeanList,String componentName) {
        componentUTCoverageBean componentUTCoverageBean=new componentUTCoverageBean();
        componentUTCoverageBean.setComponentName(componentName);
        componentUTCoverageBean.setUtCoverage(utCoverageBeanList);
        return componentUTCoverageBean;
    }



    private  List<componentUTCoverageBean> getComponentUTCoverageBeanList(componentUTCoverageBean componentUTCoverageBean){
        List<componentUTCoverageBean> componentUTCoverageBeanList=new ArrayList<componentUTCoverageBean>();
        componentUTCoverageBeanList.add(componentUTCoverageBean);
        return componentUTCoverageBeanList;
    }
    private  SAPDataProcessBean getSAPDataProcessBean(List<componentUTCoverageBean> componentUTCoverageBeanList,String projectName){
        SAPDataProcessBean SAPDataProcessBean=new SAPDataProcessBean();
        SAPDataProcessBean.setProjectName(projectName);
        SAPDataProcessBean.setComponentUTCoverage(componentUTCoverageBeanList);
        return SAPDataProcessBean;
    }
    public String generateJson(String projectName,String componentName){
        Pattern pattern=Pattern.compile("\\d+\\.\\d\\.(\\d+)");
        String directoryParent=direct+projectName+"_"+componentName;
        List<Integer> buildNumberList=new ArrayList<Integer>();
        File dir = new File(directoryParent);
        File targetFile[] = dir.listFiles();
        for (int i = 0; i < targetFile.length; i++) {
            if (!targetFile[i].isDirectory()){
                Matcher matcher=pattern.matcher(targetFile[i].getName());
                if(matcher.find()){
                    int BUILD_NUMBER=Integer.parseInt(matcher.group(1));
                    buildNumberList.add(BUILD_NUMBER);
                }
            }
        }
        int maxBuildNumber=Collections.max(buildNumberList);
        String tarFileName=projectName+"_"+componentName+"_UT_"+version+"."+String.valueOf(maxBuildNumber)+"_"+"Coverage.tar";
        String remoteFileName=direct+projectName+"_"+componentName+"/"+tarFileName;
        String targetFileDirectory=direct+projectName+"_"+componentName+"/"+
                projectName+"_"+componentName+"_UT_"+version+"."+String.valueOf(maxBuildNumber)+"_"+"Coverage/";
        try {

            CompressTool.unTar(remoteFileName,targetFileDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file=findXML(targetFileDirectory);
        utCoverageBean utCoverageBean=getUtCoverageBeanFromXMl(file);
        List<utCoverageBean> utCoverageBeanList=getUtCoverageBeanList(utCoverageBean);
        componentUTCoverageBean componentUTCoverageBean =getComponentUTCoverageBean(utCoverageBeanList,componentName);
        List<componentUTCoverageBean> componentUTCoverageBeanList=getComponentUTCoverageBeanList(componentUTCoverageBean);
        SAPDataProcessBean SAPDataProcessBean= getSAPDataProcessBean(componentUTCoverageBeanList,projectName);
        ObjectMapper mapper = new ObjectMapper();
        String str= null;
        try {
            str = mapper.writeValueAsString(SAPDataProcessBean);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return str;
    }


   public String generateAllJson(String projectName) {
        List<componentUTCoverageBean> componentUTCoverageBeanList = new ArrayList<componentUTCoverageBean>();
        List<String> componentNameList = new ArrayList<String>();
        componentNameList.add("dataProcess");
        componentNameList.add("dataAnalysis");
        componentNameList.add("oneviewService");
        for (String componentName : componentNameList) {
            Pattern pattern=Pattern.compile("\\d+\\.\\d\\.(\\d+)");
            String directoryParent=direct+projectName+"_"+componentName;
            List<Integer> buildNumberList=new ArrayList<Integer>();
            File dir = new File(directoryParent);
            File targetFile[] = dir.listFiles();
            for (int i = 0; i < targetFile.length; i++) {
                if (!targetFile[i].isDirectory()){
                    Matcher matcher=pattern.matcher(targetFile[i].getName());
                    if(matcher.find()){
                        int BUILD_NUMBER=Integer.parseInt(matcher.group(1));
                        buildNumberList.add(BUILD_NUMBER);
                    }
                }
            }
            int maxBuildNumber=Collections.max(buildNumberList);
            String tarFileName=projectName+"_"+componentName+"_UT_"+version+"."+String.valueOf(maxBuildNumber)+"_"+"Coverage.tar";
            String remoteFileName=direct+projectName+"_"+componentName+"/"+tarFileName;
            String targetFileDirectory=direct+projectName+"_"+componentName+"/"+
                    projectName+"_"+componentName+"_UT_"+version+"."+String.valueOf(maxBuildNumber)+"_"+"Coverage/";
            try {

                CompressTool.unTar(remoteFileName, targetFileDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = findXML(targetFileDirectory);
            utCoverageBean utCoverageBean = getUtCoverageBeanFromXMl(file);
            List<utCoverageBean> utCoverageBeanList = getUtCoverageBeanList(utCoverageBean);
            componentUTCoverageBean componentUTCoverageBean = getComponentUTCoverageBean(utCoverageBeanList, componentName);
            componentUTCoverageBeanList.add(componentUTCoverageBean);
        }
        SAPDataProcessBean SAPDataProcessBean = getSAPDataProcessBean(componentUTCoverageBeanList, projectName);
        ObjectMapper mapper = new ObjectMapper();
        String str = null;
        try {
            str = mapper.writeValueAsString(SAPDataProcessBean);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return str;
    }
}
