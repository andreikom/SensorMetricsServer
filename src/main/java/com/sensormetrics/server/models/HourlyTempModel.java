package com.sensormetrics.server.models;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HourlyTempModel {

     @Getter
     private Map<Short, List<Short>> hourToTemp = new HashMap<>();

}
