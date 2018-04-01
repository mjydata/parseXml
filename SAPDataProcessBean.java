package com.cisco.qdreport.dataProcess.model;

import java.util.List;

public class SAPDataProcessBean {

    private String projectName;

    private List<componentUTCoverageBean> componentUTCoverage;

    public List<componentUTCoverageBean> getComponentUTCoverage() {
        return componentUTCoverage;
    }

    public void setComponentUTCoverage(List<componentUTCoverageBean> componentUTCoverage) {
        this.componentUTCoverage = componentUTCoverage;
    }



    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


}
