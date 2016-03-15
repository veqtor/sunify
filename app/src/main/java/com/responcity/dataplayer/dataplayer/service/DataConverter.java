package com.responcity.dataplayer.dataplayer.service;

import com.responcity.dataplayer.dataplayer.model.ConvertedPoint;
import com.responcity.dataplayer.dataplayer.model.ConvertedSmhiData;
import com.responcity.dataplayer.dataplayer.network.POJOS.Parameter;
import com.responcity.dataplayer.dataplayer.network.POJOS.SmhiPoint;
import com.responcity.dataplayer.dataplayer.network.POJOS.TimeValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataConverter {
    public static ConvertedSmhiData convertSmhiPointSeries(SmhiPoint input) {
        ConvertedSmhiData convertedSmhiData = new ConvertedSmhiData();
        List<ConvertedPoint> convertedPoints = new ArrayList<>(48);
        Date now = new Date();
        boolean startedConverting = false;
        TimeValue[] timeValues = input.getTimeSeries();
        for(int i = 0; i < timeValues.length; i++) {
            if(!startedConverting) {
                if(timeValues[i].getValidTime().before(now)) {
                    startedConverting = true;
                }
            }
            if(startedConverting) {
                ConvertedPoint cPoint = new ConvertedPoint();
                cPoint.setTime(timeValues[i].getValidTime());
                Parameter[] parameters = timeValues[i].getParameters();
                for(Parameter p : parameters) {
                    if(p.getName().contentEquals("t")) {
                        cPoint.setTemp(p.getValues()[0]);
                    }
                    else if(p.getName().contentEquals("ws")) {
                        cPoint.setWind(p.getValues()[0]);
                    }
                    else if(p.getName().contentEquals("pit")) {
                        cPoint.setPrec(p.getValues()[0]);
                    }
                    else if(p.getName().contentEquals("pcat")) {
                        cPoint.setPrecT((int)p.getValues()[0]);
                    }
                }
                convertedPoints.add(cPoint);
                if(convertedPoints.size() >= 48) {
                    break;
                }
            }
        }
        convertedSmhiData.setConvertedPoints(convertedPoints);
        return convertedSmhiData;
    }
}
