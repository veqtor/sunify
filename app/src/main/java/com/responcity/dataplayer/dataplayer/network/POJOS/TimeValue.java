package com.responcity.dataplayer.dataplayer.network.POJOS;

import java.util.Date;

public class TimeValue {

    Date validTime;
    Parameter[] parameters;

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }

    public Date getValidTime() {
        return validTime;
    }

    public void setValidTime(Date validTime) {
        this.validTime = validTime;
    }
}
