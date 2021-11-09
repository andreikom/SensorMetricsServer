package com.sensormetrics.server.storage;

import com.sensormetrics.server.models.HourlyTempModel;

import java.io.IOException;
import java.util.Map;

public interface StorageProvider {

    void saveTemp(long sensorId, short temp) throws IOException;

    Map<Integer, HourlyTempModel> getHourlyTempsBySensorIDAndDate(int sensorId, String date);

    void cleanOldDailyEntry(String result);
}
