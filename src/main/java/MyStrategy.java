import model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;




public final class MyStrategy implements Strategy {



    private Random random;

    private TerrainType[][] terrainTypeByCellXY;
    private WeatherType[][] weatherTypeByCellXY;

    private Player me;
    private World world;
    private Game game;
    private Move move;
    private GlobalMap globalMap = new GlobalMap();

    private final Map<Long, Vehicle> vehicleById = new HashMap<>();
    private final Map<Long, Integer> updateTickByVehicleId = new HashMap<>();
    private final Queue<Consumer<Move>> delayedMoves = new ArrayDeque<>();


    public int getNumberRow(double y) {
        return (int)((y/world.getHeight())*game.getTerrainWeatherMapRowCount());
    }

    public int getNumberColumn(double x) {
        return (int)((x/world.getWidth())*game.getTerrainWeatherMapColumnCount());
    }




    /**
     * Основной метод стратегии, осуществляющий управление армией. Вызывается каждый тик.
     *
     * @param me    Информация о вашем игроке.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Player me, World world, Game game, Move move) {
        initializeStrategy(world, game);
        initializeTick(me, world, game, move);

        if (me.getRemainingActionCooldownTicks() > 0) {
            return;
        }

        if (executeDelayedMove()) {
            return;
        }

        move();

        executeDelayedMove();
    }

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(World world, Game game) {
        if (random == null) {
            random = new Random(game.getRandomSeed());

            ArrayList<ArrayList<Cell>> cells = new ArrayList<>();

            for (int i = 0; i < game.getTerrainWeatherMapRowCount(); i++) {

                ArrayList<Cell> cellsColumn = new ArrayList<>();

                for (int j = 0; j < game.getTerrainWeatherMapColumnCount(); j++) {
                    cellsColumn.add(new Cell(i,j));
                    //System.out.print(" " + j);
                }

                //System.out.println("");

                cells.add(cellsColumn);
            }

                   // [];



            //System.out.println("cells = " + cells.size());

            globalMap.setMapGame(cells);

            System.out.println("Wight world =" + world.getWidth());
            System.out.println("Height world = " + world.getHeight());

            terrainTypeByCellXY = world.getTerrainByCellXY();
            weatherTypeByCellXY = world.getWeatherByCellXY();
        }
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним, а также актуализируем сведения о каждой
     * технике и времени последнего изменения её состояния.
     */
    private void initializeTick(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        this.game = game;
        this.move = move;

        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicleById.put(vehicle.getId(), vehicle);
            updateTickByVehicleId.put(vehicle.getId(), world.getTickIndex());

            globalMap.getCell(getNumberRow(vehicle.getY()),
                    getNumberColumn(vehicle.getX())).addVehicleMap(vehicle.getId(), vehicle);

            System.out.println("NumberColumn = " + getNumberColumn(vehicle.getX()));
        }

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
            long vehicleId = vehicleUpdate.getId();

            if (vehicleUpdate.getDurability() == 0) {
                vehicleById.remove(vehicleId);
                updateTickByVehicleId.remove(vehicleId);
            } else {
                vehicleById.put(vehicleId, new Vehicle(vehicleById.get(vehicleId), vehicleUpdate));
                updateTickByVehicleId.put(vehicleId, world.getTickIndex());
            }
        }
    }

    /**
     * Достаём отложенное действие из очереди и выполняем его.
     *
     * @return Возвращает {@code true}, если и только если отложенное действие было найдено и выполнено.
     */
    private boolean executeDelayedMove() {
        Consumer<Move> delayedMove = delayedMoves.poll();
        if (delayedMove == null) {
            return false;
        }

        delayedMove.accept(move);
        return true;
    }

    /**
     * Основная логика нашей стратегии.
     */
    int count = 0;
    private double prevTargetX = 0;
    private double prevTargetY = 0;
    double targetX = 0;
    double targetY = 0;
    VehicleType nextTarget = VehicleType.HELICOPTER;
    int battleFormation = 0;
    int step = 0;

    private VehicleType getNearestVehicleType () {

        double minS = Math.pow(world.getWidth(),2) + Math.pow(world.getHeight(),2);
        VehicleType type = null;

        for(VehicleType vehicleType: VehicleType.values()) {
            double x = streamVehicles(
                    Ownership.ALLY, vehicleType
            ).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);

            double y = streamVehicles(
                    Ownership.ALLY, vehicleType
            ).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

            double s = Math.pow(x-world.getWidth()/2,2) + Math.pow(y - world.getHeight()/2,2);

            if ( s < minS) {
                type = vehicleType;
                minS = s;
            }
        }

        return type;
    }

    int timeCount = 0;
    int timeBegin = 0;
    int timePlan = 0;

    //TODO Написать функцию создания боевого построения
    private void createBattleFormation(VehicleType vehicleType) {

        //if( world.getTickIndex() % 60 == 0) {

            //System.out.println("OK");

            // Определить координаты группы

            double x = streamVehicles(
                    Ownership.ALLY, vehicleType
            ).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);

            //System.out.println("x = " + x);

            double y = streamVehicles(
                    Ownership.ALLY, vehicleType
            ).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

            // Выделить часть группы

            if (step == 0) {
                delayedMoves.add(move -> {
                    move.setAction(ActionType.CLEAR_AND_SELECT);
                    move.setRight(x);
                    move.setBottom(world.getHeight());
                    move.setVehicleType(vehicleType);
                });
            }


            double minX = streamVehicles(
                    Ownership.ALLY, vehicleType).mapToDouble(Vehicle::getX).min().orElse(0);
            //System.out.println("minX = " + minX);
            double maxX = streamVehicles(
                    Ownership.ALLY, vehicleType).mapToDouble(Vehicle::getX).max().orElse(0);
            //System.out.println("maxX = " + maxX);
            double minY = streamVehicles(
                    Ownership.ALLY, vehicleType).mapToDouble(Vehicle::getY).min().orElse(0);
            //System.out.println("minY = " + minY);
            double maxY = streamVehicles(
                    Ownership.ALLY, vehicleType).mapToDouble(Vehicle::getY).max().orElse(0);
            //System.out.println("maxY = " + maxY);

            if (step == 0) {

                timePlan = (int)(maxY - minY + 3)*3;

                delayedMoves.add(move -> {
                    move.setAction(ActionType.MOVE);
                    move.setX(0);
                    move.setY(maxY - minY + 3);
                    move.setVehicleType(vehicleType);
                });
                step++;
                timeBegin = timeCount;


            } else if (step == 1) {

                if (world.getTickIndex() % 60 == 0) {

                    delayedMoves.add(move -> {
                        move.setAction(ActionType.MOVE);
                        move.setX((maxX - minX)/2 + 3);
                        move.setVehicleType(vehicleType);
                    });

                    step++;

                }

            } else if (step == 2) {
                if (timeCount > timeBegin + timePlan) {

                    timeBegin = timeCount;

                    delayedMoves.add(move-> {
                        move.setAction(ActionType.CLEAR_AND_SELECT);
                        move.setRight(world.getWidth());
                        move.setBottom(world.getHeight());
                        move.setVehicleType(vehicleType);
                    });


                    delayedMoves.add(move -> {
                       move.setAction(ActionType.ROTATE);
                       move.setX(x);
                       move.setY(y);
                       move.setAngle(StrictMath.PI/4);
                    });

                    step++;

                }

            } else if (step == 3) {



                if (timeCount > timeBegin + 30) {
                    delayedMoves.add(move -> {
                        move.setAction(ActionType.MOVE);
                        move.setX(30);
                        move.setY(30);
                        move.setVehicleType(vehicleType);
                    });
                    battleFormation = 1;
                }

            }

        //}

    }

    private void refreshGlobalMap () {

        for (VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {

            long vehicleId = vehicleUpdate.getId();

            Cell oldCell = globalMap.getCellbyVehicleId(vehicleId);
            Cell newCell = globalMap.getCell(getNumberRow(vehicleUpdate.getY()),
                    getNumberColumn(vehicleUpdate.getX()));

            //System.out.println("new_X = " + newCell.getX() + " old_X = " + oldCell.getX());



            if (vehicleUpdate.getDurability() == 0) {

                //System.out.println("Status: death Id =" + vehicleId + " Type = " + oldCell.getVehicleById(vehicleId).getType());

                oldCell.deleteVehicleById(vehicleId);



            } else {

                if (newCell.getX() != oldCell.getX() || newCell.getY() != oldCell.getY()) {

                    //System.out.println("Status: move Id =" + vehicleId + " Type = " + oldCell.getVehicleById(vehicleId).getType());

                    newCell.addVehicleMap(vehicleId,oldCell.getVehicleById(vehicleId));
                    oldCell.deleteVehicleById(vehicleId);

                }
            }
        }

    }

    int init = 0;
    VehicleType nearestVehicleType;

    private void move() {
        // Каждые 300 тиков ...

        refreshGlobalMap();


        /*DEBUG Проверка, что юниты удаляются из списков
        for (int i = 2; i <= 2; i++) {
            for (int j = 0; j <= 0; j++) {
                System.out.print(" SizeMap [" + i + "][" + j + "]= " + globalMap.getCell(i,j).getVehicleMap().size());
            }
            System.out.println("");
        }*/

        timeCount++;

        //TODO Сбор информации о текущем положении

        if( init == 0) {
            nearestVehicleType = getNearestVehicleType();
            System.out.println("INIT: nearestVehicleType = " + nearestVehicleType);
            init = 1;
        }

        //System.out.println("battleFormation = " + battleFormation);

        if (battleFormation == 0) {
                createBattleFormation(nearestVehicleType);
        } else {

        if (world.getTickIndex() % 60 == 0) {
            // ... для каждого типа техники ...

            System.out.println("Step: " + count);
            count++;

            //TODO Разбить войска на группы

            double x = streamVehicles(
                    Ownership.ALLY, VehicleType.FIGHTER
            ).mapToDouble(Vehicle::getX).average().orElse(Double.NaN);

            double y = streamVehicles(
                    Ownership.ALLY, VehicleType.FIGHTER
            ).mapToDouble(Vehicle::getY).average().orElse(Double.NaN);

            //TODO Предсказать движение противника


            if (streamVehicles(Ownership.ENEMY, nextTarget).count() != 0) {
                targetX = streamVehicles(
                        Ownership.ENEMY, nextTarget
                ).mapToDouble(Vehicle::getX).average().orElseGet(
                        () -> streamVehicles(
                                Ownership.ENEMY
                        ).mapToDouble(Vehicle::getX).average().orElse(world.getWidth() / 2.0D)
                );


                targetY = streamVehicles(
                        Ownership.ENEMY, nextTarget
                ).mapToDouble(Vehicle::getY).average().orElseGet(
                        () -> streamVehicles(
                                Ownership.ENEMY
                        ).mapToDouble(Vehicle::getY).average().orElse(world.getHeight() / 2.0D)
                );
            } else {
                if (nextTarget != VehicleType.FIGHTER)
                    nextTarget = VehicleType.FIGHTER;
            }

            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                delayedMoves.add(move -> {
                    move.setAction(ActionType.CLEAR_AND_SELECT);
                    move.setRight(world.getWidth());
                    move.setBottom(world.getHeight());
                    move.setVehicleType(VehicleType.FIGHTER);
                });



                if (prevTargetY != 0 || prevTargetX != 0) {

                    delayedMoves.add(move -> {
                        move.setAction(ActionType.MOVE);
                        move.setX(2 * targetX - prevTargetX - x);
                        move.setY(2 * targetY - prevTargetY - y);
                    });

                }
            }

            /* DEBUG
            System.out.println("prevTargetX = " + prevTargetX + " currTargetX =" + targetX);
            System.out.println("prevTargetY = " + prevTargetY + " currTargetY =" + targetY);
            */

            prevTargetX = targetX;
            prevTargetY = targetY;


            //Что лучше? Выделить группы с разными видами войск или отправить группы однотипных войск на уничтожение
            //вражеской группы с которой эффективно справится

            //Сперва уничтожить авиацию послать бтр, самолеты и вертолеты на уничтожение авиации
            //С помощью истрибителей уничтожить вертолеты

            //Параллельно перестроение танков и ремонтников
            //Во время сражения при значительных повреждениях отправить первый ряд назад в 3-й или 2-й для ремонта

            //Самых хилых отправлять на ремонт

            //Создать специальную ремонтную группу, будет находится позади фронта

            //TODO Отправить группы по всей линии фронта

        }



        }

        return;

    }

    /**
     * Вспомогательный метод, позволяющий для указанного типа техники получить другой тип техники, такой, что первый
     * наиболее эффективен против второго.
     *
     * @param vehicleType Тип техники.
     * @return Тип техники в качестве приоритетной цели.
     */
    private static VehicleType getPreferredTargetType(VehicleType vehicleType) {
        switch (vehicleType) {
            case FIGHTER:
                return VehicleType.HELICOPTER;
            case HELICOPTER:
                return VehicleType.TANK;
            case IFV:
                return VehicleType.HELICOPTER;
            case TANK:
                return VehicleType.IFV;
            default:
                return null;
        }
    }

    private Stream<Vehicle> streamVehicles(Ownership ownership, VehicleType vehicleType) {
        Stream<Vehicle> stream = vehicleById.values().stream();

        switch (ownership) {
            case ALLY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() == me.getId());
                break;
            case ENEMY:
                stream = stream.filter(vehicle -> vehicle.getPlayerId() != me.getId());
                break;
            default:
        }

        if (vehicleType != null) {
            stream = stream.filter(vehicle -> vehicle.getType() == vehicleType);
        }

        return stream;
    }

    private Stream<Vehicle> streamVehicles(Ownership ownership) {
        return streamVehicles(ownership, null);
    }

    private Stream<Vehicle> streamVehicles() {
        return streamVehicles(Ownership.ANY);
    }

    private enum Ownership {
        ANY,

        ALLY,

        ENEMY
    }
}
