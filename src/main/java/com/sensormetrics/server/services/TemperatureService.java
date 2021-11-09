package com.sensormetrics.server.services;

import com.sensormetrics.server.models.HourlyTempModel;
import com.sensormetrics.server.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TemperatureService {

    private final StorageProvider storageProvider;
    // < Date, <SensorId --> <Hour>,<Temp>> >
    private final Map<String, Map<Integer, HourlyTempModel>> dailySensorTempCache = new HashMap<>();
    private final int DAYS_TO_TRACK = 7;
    private List<String> dates;
    public final int SENSORS_ID_RANGE = 100;

    @Autowired
    public TemperatureService(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @PostConstruct
    public void init() {
        initWeeklySensorTempCache();
        scheduleCacheRefresh();
    }

    private void scheduleCacheRefresh() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this::initWeeklySensorTempCache,
                        0,
                        1, TimeUnit.HOURS);
    }

    private void initWeeklySensorTempCache() {
        this.dates = getDatesForTheLastWeek();
        this.dates.forEach(date -> {
            for (int i = 1; i <= SENSORS_ID_RANGE; i++) {
                Map<Integer, HourlyTempModel> dateToSensorIdMap =
                        storageProvider.getHourlyTempsBySensorIDAndDate(i, date);
                if (dateToSensorIdMap != null) {
                    Map<Integer, HourlyTempModel> existingEntry = dailySensorTempCache.put(date, dateToSensorIdMap);
                    if (existingEntry != null) {
                        dateToSensorIdMap.putAll(existingEntry);
                    }
                }
            }
        });
        cleanOldDailyEntriesIfNeeded();
    }

    private void cleanOldDailyEntriesIfNeeded() {
        Set<String> datesToClean = dates.stream()
                .filter(date -> !getDatesForTheLastWeek().contains(date))
                .collect(Collectors.toSet());
        if (!datesToClean.isEmpty()) {
            datesToClean.forEach(date -> {
                dailySensorTempCache.remove(date);
                storageProvider.cleanOldDailyEntry(date);
            });
        }
    }

    public void addTemp(long sensorId, short temp) throws IOException {
        storageProvider.saveTemp(sensorId, temp);
    }

    public short getDailyMaxTempByDateAndById(int sensorId, String date) {
        HourlyTempModel sensorData = getHourlyTempModel(sensorId, date);
        if (sensorData == null) {
            return Short.MIN_VALUE;
        }
        return getMaxTemp(sensorData);
    }

    private short getMaxTemp(HourlyTempModel sensorData) {
        short maxTemp = Short.MIN_VALUE;
        Optional<Short> max = Optional.empty();
        for (Map.Entry<Short, List<Short>> hour : sensorData.getHourToTemp().entrySet()) {
            if (!hour.getValue().isEmpty()) {
                max = hour.getValue().stream().max(Short::compare);
            }
            if (max.isPresent() && max.get() > maxTemp) {
                maxTemp = max.get();
            }
        }
        return maxTemp;
    }

    public short getMinTempByDailyDateAndById(int sensorId, String date) {
        HourlyTempModel sensorData = getHourlyTempModel(sensorId, date);
        if (sensorData == null) {
            return Short.MAX_VALUE;
        }
        return getMinTemp(sensorData);
    }

    private short getMinTemp(HourlyTempModel sensorData) {
        short minTemp = Short.MAX_VALUE;
        Optional<Short> min = Optional.empty();
        for (Map.Entry<Short, List<Short>> hour : sensorData.getHourToTemp().entrySet()) {
            if (!hour.getValue().isEmpty()) {
                min = hour.getValue().stream().min(Short::compare);
            }
            if (min.isPresent() && min.get() < minTemp) {
                minTemp = min.get();
            }
        }
        return minTemp;
    }

    public float getDailyAverageByDateAndById(int sensorId, String date) {
        HourlyTempModel sensorData = getHourlyTempModel(sensorId, date);
        if (sensorData == null) {
            return Short.MIN_VALUE;
        }
        return getDailyAverage(sensorData);
    }

    private float getDailyAverage(HourlyTempModel sensorData) {
        ArrayList<Float> hourlyAverage = new ArrayList<>();
        for (Map.Entry<Short, List<Short>> hour : sensorData.getHourToTemp().entrySet()) {
            if (!hour.getValue().isEmpty()) {
                int hourSum = hour.getValue().stream()
                        .reduce((short) 0, this::addShorts);
                int numOfTemps = hour.getValue().size();
                hourlyAverage.add(hourSum / (float) numOfTemps);
            }
        }
        return calculateAverageByElementsSize(hourlyAverage);
    }

    private float calculateAverageByElementsSize(ArrayList<Float> hourlyAverage) {
        return hourlyAverage.stream()
                .reduce((float) 0, Float::sum) / hourlyAverage.size();
    }

    public short getMaxTempLastWeekForSensor(int sensorId) {
        return this.dates.stream()
                .map(date -> getDailyMaxTempByDateAndById(sensorId, date))
                .max(Short::compare)
                .orElse(Short.MIN_VALUE);
    }

    public short getMinTempLastWeekForSensor(int sensorId) {
        return this.dates.stream()
                .map(date -> getMinTempByDailyDateAndById(sensorId, date))
                .min(Short::compare)
                .orElse(Short.MAX_VALUE);
    }

    public float getAverageTempLastWeekForSensor(int sensorId) {
        ArrayList<Float> weeklyAveragesList = this.dates.stream()
                .map(date -> getHourlyTempModel(sensorId, date))
                .filter(Objects::nonNull)
                .map(this::getDailyAverage)
                .collect(Collectors.toCollection(ArrayList::new));
        return calculateAverageByElementsSize(weeklyAveragesList);
    }

    public short getMaxTempLastWeekOfAllSensors() {
        ArrayList<Short> dailyMaxList = new ArrayList<>();
        this.dates.forEach(date -> {
            for (int i = 1; i <= SENSORS_ID_RANGE; i++) {
                Map<Integer, HourlyTempModel> dateToSensorIdMap = dailySensorTempCache.get(date);
                if (dateToSensorIdMap != null) {
                    HourlyTempModel hourlyTempModel = dateToSensorIdMap.get(i);
                    if (hourlyTempModel != null) {
                        dailyMaxList.add(getMaxTemp(hourlyTempModel));
                    }
                }
            }
        });
        return dailyMaxList.stream()
                .max(Short::compare).orElse(Short.MIN_VALUE);
    }

    public short getMinTempWeeklyOfAllSensors() {
        ArrayList<Short> dailyMinList = new ArrayList<>();
        this.dates.forEach(date -> {
            for (int i = 1; i <= SENSORS_ID_RANGE; i++) {
                Map<Integer, HourlyTempModel> dateToSensorIdMap = dailySensorTempCache.get(date);
                if (dateToSensorIdMap != null) {
                    HourlyTempModel hourlyTempModel = dateToSensorIdMap.get(i);
                    if (hourlyTempModel != null) {
                        dailyMinList.add(getMinTemp(hourlyTempModel));
                    }
                }
            }
        });
        return dailyMinList.stream()
                .min(Short::compare).orElse(Short.MAX_VALUE);
    }

    public float getAverageTempLastWeekOfAllSensors() {
        ArrayList<Float> dailyAverageList = new ArrayList<>();
        this.dates.forEach(date -> {
            for (int i = 1; i <= SENSORS_ID_RANGE; i++) {
                Map<Integer, HourlyTempModel> dateToSensorIdMap = dailySensorTempCache.get(date);
                if (dateToSensorIdMap != null) {
                    HourlyTempModel hourlyTempModel = dateToSensorIdMap.get(i);
                    if (hourlyTempModel != null) {
                        dailyAverageList.add(getDailyAverage(hourlyTempModel));
                    }
                }
            }
        });
        return calculateAverageByElementsSize(dailyAverageList);
    }

    private HourlyTempModel getHourlyTempModel(int sensorId, String date) {
        Map<Integer, HourlyTempModel> sensorByDateData = dailySensorTempCache.get(date);
        if (sensorByDateData == null) {
            return null;
        }
        return sensorByDateData.get(sensorId);
    }

    private List<String> getDatesForTheLastWeek() {
        ArrayList<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        dates.add(today.toString());
        for (int i = 1; i <= DAYS_TO_TRACK; i++) {
            LocalDate date = today.minus(i, ChronoUnit.DAYS);
            dates.add(date.toString());
        }
        return dates;
    }

    private short addShorts(short a, short b) {
        return (short) (a + b);
    }
}
