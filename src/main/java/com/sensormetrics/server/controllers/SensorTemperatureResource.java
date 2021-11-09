package com.sensormetrics.server.controllers;

import com.sensormetrics.server.services.TemperatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class SensorTemperatureResource {

    private final TemperatureService temperatureService;

    @Autowired
    public SensorTemperatureResource(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }

    @PostMapping("/temperature/{sensorId}/{temp}")
    public ResponseEntity<String> addSensorTemp(@PathVariable int sensorId, @PathVariable Short temp) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        try {
            temperatureService.addTemp(sensorId, temp);
        } catch (IOException e) {
            String msg = "Error occurred while attempting to add sensor temperature:";
            System.out.println(msg);
            e.printStackTrace();
            return new ResponseEntity<>(msg + ", please check server logs", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/temperature/daily_max/{sensorId}/{date}")
    public ResponseEntity<?> getDailyMaxForSensor(@PathVariable int sensorId, @PathVariable String date) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getDailyMaxTempByDateAndById(sensorId, date), HttpStatus.OK);
    }

    @GetMapping("/temperature/daily_min/{sensorId}/{date}")
    public ResponseEntity<?> getDailyMinForSensor(@PathVariable int sensorId, @PathVariable String date) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getMinTempByDailyDateAndById(sensorId, date), HttpStatus.OK);
    }

    @GetMapping("/temperature/daily_avg/{sensorId}/{date}")
    public ResponseEntity<?> getDailyAverageForSensor(@PathVariable int sensorId, @PathVariable String date) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getDailyAverageByDateAndById(sensorId, date), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_max/{sensorId}")
    public ResponseEntity<?> getWeeklyMaxForSensor(@PathVariable int sensorId) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getMaxTempLastWeekForSensor(sensorId), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_min/{sensorId}")
    public ResponseEntity<?> getWeeklyMinForSensor(@PathVariable int sensorId) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getMinTempLastWeekForSensor(sensorId), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_avg/{sensorId}")
    public ResponseEntity<?> getWeeklyAvgForSensor(@PathVariable int sensorId) {
        if (isSensorIdOutsideRange(sensorId)) {
            return new ResponseEntity<>("Sensor ID is not within allowed range", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(temperatureService.getAverageTempLastWeekForSensor(sensorId), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_max")
    public ResponseEntity<Short> getWeeklyMax() {
        return new ResponseEntity<>(temperatureService.getMaxTempLastWeekOfAllSensors(), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_min")
    public ResponseEntity<Short> getWeeklyMin() {
        return new ResponseEntity<>(temperatureService.getMinTempWeeklyOfAllSensors(), HttpStatus.OK);
    }

    @GetMapping("/temperature/weekly_avg")
    public ResponseEntity<Float> getWeeklyAvg() {
        return new ResponseEntity<>(temperatureService.getAverageTempLastWeekOfAllSensors(), HttpStatus.OK);
    }

    private boolean isSensorIdOutsideRange(@PathVariable int sensorId) {
        return sensorId < 1 || sensorId > temperatureService.SENSORS_ID_RANGE;
    }
}
