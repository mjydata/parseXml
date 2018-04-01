package com.cisco.qdreport.dataProcess.model;


public class utCoverageBean {

    private String branchName;

    private String coverRate;

    private int noCaverageLines;
    private int coveredLines;


    public double getCoveredLines() {
        return coveredLines;
    }

    public void setCoveredLines(int coveredLines) {
        this.coveredLines = coveredLines;
    }



    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCoverRate() {
        return coverRate;
    }

    public void setCoverRate(String coverRate) {
        this.coverRate = coverRate;
    }

    public double getNoCaverageLines() {
        return noCaverageLines;
    }

    public void setNoCaverageLines(int noCaverageLines) {
        this.noCaverageLines = noCaverageLines;
    }


}
