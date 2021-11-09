package com.sensormetrics.server.storage.filesystem;

import com.sensormetrics.server.models.HourlyTempModel;
import com.sensormetrics.server.storage.StorageProvider;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class FileSystemStore implements StorageProvider {

    private final String SLASH = System.getProperty("os.name").toLowerCase().contains("win") ? "\\" : "/";
    private final String FS_STORE_PATH = System.getProperty("user.home") + SLASH + "sensordata";
    private final String TEMPERATURES_PATH = FS_STORE_PATH + SLASH + "temperature";

    @Override
    public void saveTemp(long sensorId, short temp) throws IOException {
        FileUtils.write(
                new File(TEMPERATURES_PATH + SLASH + sensorId + SLASH +
                        java.time.LocalDate.now() + SLASH + new DateTime().hourOfDay().get() + SLASH + temp),
                null, Charset.defaultCharset());
        System.out.println("Created new file for sensor: '" + sensorId + "' with temperature: '" + temp + "'");
    }

    /**
     * Returns a map comprised of <sensorId, <hours> --> list<temperature>>
     */
    @Override
    public Map<Integer, HourlyTempModel> getHourlyTempsBySensorIDAndDate(int sensorId, String date) {
        File sensorTempDirectory = new File(TEMPERATURES_PATH + SLASH + sensorId + SLASH + date + SLASH);
        if (sensorTempDirectory.exists()) {
            List<String> tempByHours = FileUtils.listFiles(
                            sensorTempDirectory,
                            null, true)
                    .stream()
                    .map(File::toString)
                    .map(this::getLastHourTempFromPath)
                    .collect(Collectors.toList());
            return convertPathListToSensorKeyMap(sensorId, tempByHours);
        } else {
            return null;
        }
    }

    @Override
    public void cleanOldDailyEntry(String date) {
        File file = new File(TEMPERATURES_PATH + SLASH + "*" + SLASH + date + SLASH);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Cleaned old date directory for all sensors: " + file);
            }
        }
    }

    private Map<Integer, HourlyTempModel> convertPathListToSensorKeyMap(int sensorId, List<String> list) {
        HashMap<Integer, HourlyTempModel> SensorIdHourlyTempMap = new HashMap<>();
        list.forEach(item -> {
            short hour = Short.parseShort(item.substring(1, nthLastIndexOfSlash(1, item)));
            short temp = Short.parseShort(item.substring(item.lastIndexOf(SLASH) + 1));
            if (!SensorIdHourlyTempMap.containsKey(sensorId)) {
                addNewSensorEntry(sensorId, SensorIdHourlyTempMap, hour, temp);
            } else {
                addToExistingSensorEntry(sensorId, SensorIdHourlyTempMap, hour, temp);
            }
        });
        return SensorIdHourlyTempMap;
    }

    private void addToExistingSensorEntry(int sensorId, HashMap<Integer, HourlyTempModel> SensorIdHourlyTempMap, short hour, short temp) {
        HourlyTempModel exitingSensorModel = SensorIdHourlyTempMap.get(sensorId);
        if (!exitingSensorModel.getHourToTemp().containsKey(hour)) {
            ArrayList<Short> tempList = new ArrayList<>();
            tempList.add(temp);
            exitingSensorModel.getHourToTemp().put(hour, tempList);
        } else {
            List<Short> tempList = exitingSensorModel.getHourToTemp().get(hour);
            tempList.add(temp);
        }
    }

    private void addNewSensorEntry(int sensorId, HashMap<Integer, HourlyTempModel> SensorIdHourlyTempMap, short hour, short temp) {
        HourlyTempModel newSensorModel = new HourlyTempModel();
        ArrayList<Short> tempList = new ArrayList<>();
        tempList.add(temp);
        newSensorModel.getHourToTemp().put(hour, tempList);
        SensorIdHourlyTempMap.put(sensorId, newSensorModel);
    }

    private String getLastHourTempFromPath(String value) {
        return value.substring(nthLastIndexOfSlash(2, value));
    }

    private int nthLastIndexOfSlash(int nth, String value) {
        if (nth == 0) {
            return value.length();
        }
        return nthLastIndexOfSlash(--nth, value.substring(0, value.lastIndexOf(SLASH)));
    }
}
