package com.responcity.dataplayer.dataplayer.model;

import java.util.Date;

public class ConvertedPoint {
    float temp;
    float wind;
    float prec;
    int precT;
    Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public float getPrec() {
        return prec;
    }

    public void setPrec(float prec) {
        this.prec = prec;
    }

    public int getPrecT() {
        return precT;
    }

    public void setPrecT(int precT) {
        this.precT = precT;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getWind() {
        return wind;
    }

    public void setWind(float wind) {
        this.wind = wind;
    }
}
