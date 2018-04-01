package com.cisco.qdreport.dataProcess.model;

import java.util.List;

public class componentUTCoverageBean {
    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public List<utCoverageBean> getUtCoverage() {
        return utCoverage;
    }

    public void setUtCoverage(List<utCoverageBean> utCoverage) {
        this.utCoverage = utCoverage;
    }

    private String componentName;
    private List<utCoverageBean> utCoverage;
}
