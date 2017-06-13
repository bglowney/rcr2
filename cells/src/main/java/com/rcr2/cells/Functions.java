package com.rcr2.cells;

import com.rcr2.Context;
import com.rcr2.EmptyFrame;
import com.rcr2.cells.CellFrame.Cell;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.rcr2.cells.CellFrame.*;

public class Functions {

    static class Place {
        Cell ground;
        Cell above;

        final int x;
        final int y;

        public Place(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public static final String BOTH_OCCUPIED = " _^ ";
        public static final String GROUND_OCCUPIED = " __ ";
        public static final String ABOVE_OCCUPIED = " ^^ ";
        public static final String ABOVE_MAIN = " Mm ";
        public static final String MAIN_AND_GROUND = " M_ ";
        public static final String EMPTY_PLACE = " .. ";
        public static final String INVISIBLE_PLACE = "    ";


        @Override
        public String toString() {
            if (above != null && above instanceof Player && ground != null)
                return MAIN_AND_GROUND;
            else if (ground != null && above != null)
                return BOTH_OCCUPIED;
            else if (above != null && above instanceof Player)
                return ABOVE_MAIN;
            else if (ground != null)
                return GROUND_OCCUPIED;
            else if (above != null)
                return ABOVE_OCCUPIED;
            return EMPTY_PLACE;
        }
    }

    static class EmptyCellFrame extends CellFrame implements EmptyFrame<CellFrame> {
        @Override
        public String display() {
            return "empty";
        }
    }

    static final EmptyFrame EMPTY_CELL_FRAME = new EmptyCellFrame();

    public static class CellsContext extends Context<CellFrame> {
        public static final int CELLS_LENGTH = 10;
        public static final int CELLS_WIDTH = 10;
        final Player mainCell;

        final Place[][] places;

        CellsContext(int length, int width, int ix, int iy) {
            places = new Place[length][width];
            if (ix < 0 || ix > width - 1 || iy < 0 || iy > length - 1)
                throw new IllegalArgumentException("Illegal starting coordinates");
            mainCell = new Player(ix, iy);
            Place mainPlace = new Place(ix, iy);
            mainPlace.above = mainCell;
            places[ix][iy] = mainPlace;
        }

        public CellFrame successFrame() {
            return new CellFrame(mainCell);
        }

        boolean move(Cell cell, int newX, int newY) {
            Place newPlace = places[newX][newY];
            if (newPlace.above != null)
                return false;
            Place oldPlace = places[cell.x][cell.y];
            oldPlace.above = null;
            newPlace.above = cell;
            cell.x = newX;
            cell.y = newY;
            return true;
        }

        public List<Place> nearby(Cell cell) {
            final List<Place> nearbyPlaces = new ArrayList<>();
            final int il = cell.x - 1 >= 0 ? cell.x - 1 : 0;
            final int ir = cell.x < places.length - 1 ? cell.x + 1 : places.length - 1;
            for (int i = il; i <= ir; i++) {
                Place[] row = places[i];
                final int jt = cell.y - 1 >= 0 ? cell.y - 1 : 0;
                final int jb = cell.y < row.length - 1 ? cell.y + 1 : row.length - 1;
                for (int j = jt; j <= jb; j++) {
                    Place place = places[i][j];
                    if (place != null)
                        nearbyPlaces.add(place);
                }
            }
            return nearbyPlaces;
        }

        boolean consume(Animate animate) {
            Place currentPlace = places[animate.x][animate.y];
            if (currentPlace.ground != null){
                Inanimate inanimate = (Inanimate) currentPlace.ground;
                if (inanimate.energy > 0) {
                    inanimate.energy--;
                    animate.energy++;
                    return true;
                }
            }

            return false;
        }

        boolean multiply(Animate animate) {
            // try to multiply
            List<Place> destinations = nearby(animate)
                    .stream()
                    .filter(place -> place.above == null)
                    .collect(Collectors.toList());

            if (destinations.isEmpty()) return false;

            destinations
                    .stream()
                    .skip(ThreadLocalRandom.current().nextInt(0, destinations.size()))
                    .findFirst()
                    .ifPresent(place -> {
                        place.above = new Animate(place.x, place.y);
                        animate.energy = 1;
                    });

            return true;
        }

        static String displayCells(List<Cell> cells) {
            Place[][] places = new Place[CELLS_LENGTH][CELLS_WIDTH];
            for (Cell cell: cells) {
                Place place = places[cell.x][cell.y];
                if (place == null) {
                    place = new Place(cell.x, cell.y);
                    places[cell.x][cell.y] = place;
                }
                if (cell instanceof Animate)
                    place.above = cell;
                else if (cell instanceof Inanimate)
                    place.ground = cell;
            }
            return displayCells(places);
        }

        static String displayCells(Place[][] cells) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < cells.length; i++) {
                Place[] row = cells[i];
                for (int j = 0; j < row.length; j++) {
                    Place place = row[j];
                    if (place != null)
                        builder.append(place.toString());
                    else
                        builder.append(Place.INVISIBLE_PLACE);
                }
                builder.append("\n");
            }
            return builder.toString();
        }

    }

    private static Optional<CellFrame> cellFrameOptional(CellFrame cellFrame) {
        if (cellFrame.getCells().isEmpty())
            return Optional.of((CellFrame) EMPTY_CELL_FRAME);
        return Optional.of(cellFrame);
    }

    public static CellsContext newContext() {

        final CellsContext context = new CellsContext(CellsContext.CELLS_LENGTH,
                CellsContext.CELLS_WIDTH, 5, 5);

        setContextFunctions(context);
        setCells(context);

        return context;
    }

    private static void setCells(CellsContext context) {
        for (int i = 0; i < context.places.length; i++) {
            Place[] row = context.places[i];
            for (int j = 0; j < row.length; j++) {
                Place place = row[j];
                if (place == null) {
                    place = new Place(i,j);
                    context.places[i][j] = place;
                }

                if (context.mainCell.x == i && context.mainCell.y == j)
                    continue;

                if (ThreadLocalRandom.current().nextInt(0,10) < 2)
                    place.above = new Animate(i, j);
                if (ThreadLocalRandom.current().nextInt(0,10) < 7)
                    place.ground = new Inanimate(i, j);
            }
        }
    }

    private static void setContextFunctions(CellsContext context) {
        context
            .withPureFunction("nearby", 0, args -> {
                CellFrame cellFrame = new CellFrame(context.mainCell);
                    context.nearby(context.mainCell)
                        .forEach(place -> {
                            if (place.above != null)
                                cellFrame.withCell(place.above);
                            if (place.ground != null)
                                cellFrame.withCell(place.ground);
                        });

                return cellFrameOptional(cellFrame);
            })
            .withPureFunction("threats", 1, args -> {
                List<Cell> predators = args.get(0).getCells()
                        .stream()
                        .filter(cell -> cell instanceof Animate)
                        .collect(Collectors.toList());

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(predators));
            })
            .withPureFunction("resources", 1, args -> {
                List<Cell> resources = args.get(0).getCells()
                        .stream()
                        .filter(cell -> cell instanceof Inanimate
                                && context.places[cell.x][cell.y].above == null)
                        .collect(Collectors.toList());

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(resources));
            })
            .withSideEffect("consume", 0, args -> {
                return context.consume(context.mainCell)
                        ? Optional.of(new CellFrame(context.mainCell))
                        : Optional.empty();
            })
            .withPureFunction("together", 2, args -> {
                // filter l1 for items that are near at least one item in l2
                List<Cell> l1 = args.get(0).getCells();
                List<Cell> l2 = args.get(1).getCells();

                Set<Cell> nearbyCells = new HashSet<>();
                for (Cell cell : l1) {
                    for (Cell other: l2) {
                        if (cell.distanceFrom(other) < 2)
                            nearbyCells.add(other);
                    }
                }

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(nearbyCells));
            })
            .withPureFunction("apart", 2, args -> {
                // filter l1 for items that are not near any item in l2
                List<Cell> l1 = args.get(0).getCells();
                List<Cell> l2 = args.get(1).getCells();

                Set<Cell> apartCells = new HashSet<>();
                for (Cell cell : l1) {
                    for (Cell other: l2) {
                        if (cell.distanceFrom(other) > 2)
                            apartCells.add(other);
                        else
                            apartCells.remove(other);
                    }
                }

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(apartCells));
            })
            .withSideEffect("advance", 1, args -> {
                // select random from list supplied and move toward it
                List<Cell> cells = args.get(0).getCells();
                if (cells.isEmpty())
                    return Optional.empty();

                Cell target = cells.get(ThreadLocalRandom.current().nextInt(0, cells.size()));
                return context.move(context.mainCell, target.x, target.y) ? Optional.of(context.successFrame()) : Optional.empty();
            })
            .withSideEffect("flee", 1, args -> {

                class CandidatePlace implements Comparable<CandidatePlace> {
                    final Place place;
                    final int x;
                    final int y;
                    final double distance;

                    CandidatePlace(Place place, int x, int y, List<Cell> targets) {
                        this.place = place;
                        this.x = x;
                        this.y = y;
                        int cumulative = 0;
                        for (Cell target: targets)
                            cumulative += Math.abs(x - target.x) + Math.abs(y - target.y);
                        this.distance = (double)cumulative / (double)targets.size();
                    }

                    @Override
                    public int compareTo(CandidatePlace o) {
                        return (int)(-1 * (this.distance - o.distance));
                    }
                }

                List<Cell> targets = args.get(0).getCells();

                final int il = context.mainCell.x - 1 >= 0 ? context.mainCell.x - 1 : 0;
                final int ir = context.mainCell.x < context.places.length - 1 ? context.mainCell.x + 1 : context.places.length - 1;

                // for each position surrounding main cell
                // calculate average distance from targets
                // move to position with greatest average
                List<CandidatePlace> candidates = new ArrayList<>();
                for (int i = il; i <= ir; i++) {
                    Place[] row = context.places[i];
                    final int jt = context.mainCell.y - 1 >= 0 ? context.mainCell.y - 1 : 0;
                    final int jb = context.mainCell.y < row.length - 1 ? context.mainCell.y + 1 : row.length - 1;

                    for (int j = jt; j <= jb; j++)
                        candidates.add(new CandidatePlace(row[j], i, j, targets));
                }

                Optional<CandidatePlace> best = candidates.stream()
                        .filter(candidate -> candidate.place.above == null)
                        .sorted(Comparator.naturalOrder())
                        .findFirst();

                if (best.isPresent()) {
                    context.move(context.mainCell, best.get().x, best.get().y);
                    return Optional.of(context.successFrame());
                }

                return Optional.of((CellFrame) EMPTY_CELL_FRAME);
            });
    }

}
