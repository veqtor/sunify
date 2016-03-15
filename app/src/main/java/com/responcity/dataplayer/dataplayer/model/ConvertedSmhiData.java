package com.responcity.dataplayer.dataplayer.model;

import java.util.List;

public class ConvertedSmhiData {
    public List<ConvertedPoint> getConvertedPoints() {
        return convertedPoints;
    }

    private float[] temps;
    private float[] winds;
    private float[] precs;
    private float[] prects;

    public void setConvertedPoints(List<ConvertedPoint> convertedPoints) {
        this.convertedPoints = convertedPoints;
    }

    List<ConvertedPoint> convertedPoints;

    public ConvertedSmhiData() {
    }

    public float[] getTempsArray() {
        if(temps == null) {
            createArrays();
        }
        return temps;
    }
    public float[] getWindsArray() {
        if(winds == null) {
            createArrays();
        }
        return winds;
    }
    public float[] getPrecsArray() {
        if(precs == null) {
            createArrays();
        }
        return precs;
    }

    public float[] getPrectsArray() {
        if(prects == null) {
            createArrays();
        }
        return prects;
    }

    private void createArrays() {
        temps = new float[convertedPoints.size()];
        winds = new float[convertedPoints.size()];
        precs = new float[convertedPoints.size()];
        prects = new float[convertedPoints.size()];
        for(int i = 0; i < convertedPoints.size(); i++) {
            ConvertedPoint convertedPoint = convertedPoints.get(i);
            temps[i] = convertedPoint.getTemp();
            winds[i] = convertedPoint.getWind();
            precs[i] = convertedPoint.getPrec();
            prects[i] = convertedPoint.getPrecT();
        }
    }

}
