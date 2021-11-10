package com.sensormetrics.server.storage;

import com.sensormetrics.server.models.HourlyTempModel;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface TemperatureStorageProvider {

    void saveTemperature(long sensorId, short temp) throws IOException;

    Map<Integer, HourlyTempModel> getHourlyTempsBySensorIDAndDate(int sensorId, String date);

    Set<String> getAllSensorsDailyTemperatures();

    void cleanOldDailyEntry(String result);
}
