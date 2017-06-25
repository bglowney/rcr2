package com.rcr2.cells;

import com.rcr2.Context;
import com.rcr2.SequenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CellsContext extends Context<CellFrame,CellsContext> {
    public static final int CELLS_LENGTH = 10;
    public static final int CELLS_WIDTH = 10;
    final CellFrame.Player mainCell;

    final Place[][] places;

    CellsContext(SequenceProvider<CellFrame,CellsContext> sequenceProvider, CellFrame.Player mainCell, int length, int width) {
        super(sequenceProvider);
        places = new Place[length][width];
        this.mainCell = mainCell;
        Place mainPlace = new Place(mainCell.x, mainCell.y);
        mainPlace.above = mainCell;
        places[mainCell.x][mainCell.y] = mainPlace;
        setCells();
    }

    private void setCells() {
        for (int i = 0; i < places.length; i++) {
            Place[] row = places[i];
            for (int j = 0; j < row.length; j++) {
                Place place = row[j];
                if (place == null) {
                    place = new Place(i, j);
                    places[i][j] = place;
                }

                if (mainCell.x == i && mainCell.y == j)
                    continue;

                if (ThreadLocalRandom.current().nextInt(0, 10) < 2)
                    place.above = new CellFrame.Animate(i, j);
                if (ThreadLocalRandom.current().nextInt(0, 10) < 7)
                    place.ground = new CellFrame.Inanimate(i, j);
            }
        }
    }

    public CellFrame successFrame() {
        return new CellFrame(mainCell);
    }

    boolean move(CellFrame.Cell cell, int newX, int newY) {
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

    public List<Place> nearby(CellFrame.Cell cell) {
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

    boolean consume(CellFrame.Animate animate) {
        Place currentPlace = places[animate.x][animate.y];
        if (currentPlace.ground != null) {
            CellFrame.Inanimate inanimate = (CellFrame.Inanimate) currentPlace.ground;
            if (inanimate.energy > 0) {
                inanimate.energy--;
                animate.energy++;
                return true;
            }
        }

        return false;
    }

    boolean multiply(CellFrame.Animate animate) {
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
                    place.above = new CellFrame.Animate(place.x, place.y);
                    animate.energy = 1;
                });

        return true;
    }

    static String displayCells(List<CellFrame.Cell> cells) {
        Place[][] places = new Place[CELLS_LENGTH][CELLS_WIDTH];
        for (CellFrame.Cell cell : cells) {
            Place place = places[cell.x][cell.y];
            if (place == null) {
                place = new Place(cell.x, cell.y);
                places[cell.x][cell.y] = place;
            }
            if (cell instanceof CellFrame.Animate)
                place.above = cell;
            else if (cell instanceof CellFrame.Inanimate)
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

    static class Place {
        CellFrame.Cell ground;
        CellFrame.Cell above;

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
            if (above != null && above instanceof CellFrame.Player && ground != null)
                return MAIN_AND_GROUND;
            else if (ground != null && above != null)
                return BOTH_OCCUPIED;
            else if (above != null && above instanceof CellFrame.Player)
                return ABOVE_MAIN;
            else if (ground != null)
                return GROUND_OCCUPIED;
            else if (above != null)
                return ABOVE_OCCUPIED;
            return EMPTY_PLACE;
        }
    }
}
