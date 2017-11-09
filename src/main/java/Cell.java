import model.TerrainType;
import model.Vehicle;
import model.WeatherType;

import java.util.HashMap;

public class Cell {

    private HashMap<Long, Vehicle> vehicleMap;
    private TerrainType terrainType;
    private WeatherType weatherType;
    int x;
    int y;

    Cell (int x, int y) {
        vehicleMap = new HashMap<>();
        this.x = x;
        this.y = y;
    }

    public void addVehicleMap(Long id, Vehicle vehicle) {
        this.vehicleMap.put(id,vehicle);
    }

    public boolean isVehicleById(Long id) {
        return vehicleMap.containsKey(id);
    }

    public void deleteVehicleById(Long id) {
        vehicleMap.remove(id);
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleMap.get(id);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public HashMap<Long, Vehicle> getVehicleMap() {
        return this.vehicleMap;
    }

    //TODO Добавить getter и setter для Terrain и Weather

}
