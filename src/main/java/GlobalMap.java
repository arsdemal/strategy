import java.util.ArrayList;

class GlobalMap {
    private ArrayList<ArrayList<Cell>> mapGame;

    public void setMapGame(ArrayList<ArrayList<Cell>> mapGame) {
        this.mapGame = mapGame;
    }

    public Cell getCell(int i, int j) {
        return mapGame.get(i).get(j);
    }

    public Cell getCellbyVehicleId(Long id) {
        for (ArrayList<Cell> cells : mapGame) {
            for (Cell oneCell: cells) {
                if (oneCell.isVehicleById(id)) {
                    return oneCell;
                }
            }
        }
        return null;
    }
}