# Sensor Metrics Server

* Currently supports 100 sensors - their IDs should by ranging from 1 to 100 (including)

## Steps to Setup

**1. Clone the application**

```bash
https://github.com/andreikom/SensorMetricsServer.git
```

2. Build locally using 
```bash
mvn clean install
```

3. Start in IDE with
```bash
StartSensorMetricsServer
```

## Quick curl commands

* Replace with values instead any '{{ }}' brackets (remove the curly brackets)

1. Send sensor temp

```bash
curl -XPOST http://localhost:8080/temperature/{{sensorId}}/{{temp}}
```

2. Get daily temperature average of a sensor by its ID and a wanted date 
```bash
GET http://localhost:8080/temperature/daily_avg/{{sensorId}}/{{date}}
```

3. Get daily max temperature of a sensor by its ID and a wanted date
```bash
GET http://localhost:8080/temperature/daily_max/{{sensorId}}/{{date}}
```

4. Get daily min temperature of a sensor by its ID and a wanted date
```bash
GET http://localhost:8080/temperature/daily_min/{{sensorId}}/{{date}}
```

5. Get weekly average temperature of all sensors combined
```bash
GET http://localhost:8080/temperature/weekly_avg
```

6. Get weekly average temperature of a particular sensor
```bash
GET http://localhost:8080/temperature/weekly_avg/{{sensorId}}
```

7. Get weekly max temperature of all sensors combined (one sensor sent the highest value)
```bash
GET http://localhost:8080/temperature/weekly_max
```

8. Get weekly max temperature of a particular sensor
```bash
GET http://localhost:8080/temperature/weekly_max/{{sensorId}}
```

9. Get weekly min temperature of all sensors combined (one sensor sent the lowest value)
```bash
GET http://localhost:8080/temperature/weekly_min
```

10. Get weekly min temperature of a particular sensor
```bash
GET http://localhost:8080/temperature/weekly_min/{{sensorId}}
```