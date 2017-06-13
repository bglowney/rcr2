package com.rcr2.cells;

import com.rcr2.impl.DisplayableFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CellFrame implements DisplayableFrame<CellFrame> {

    public abstract static class Cell {
        int x;
        int y;
        int energy = 0;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public abstract void tic();

        public int distanceFrom(Cell other) {
            return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
        }
    }

    private final Cell mainCell;

    CellFrame() {
        mainCell = null;
    }

    CellFrame(Cell mainCell) {
        this.mainCell = mainCell;
    }

    @Override
    public String display() {
        List<Cell> cells = new ArrayList<>(this.getCells());
        if (this.mainCell != null)
            cells.add(this.mainCell);
        String display = "";
        if (mainCell != null)
            display = displayCell(mainCell);
        return display + "\n" + Functions.CellsContext.displayCells(cells);
    }

    public static String displayCell(Cell cell) {
        return "Cell Energy: " + cell.energy;
    }

    public static class Animate extends Cell {
        private int wait = 0;

        public Animate(int x, int y) {
            super(x, y);
            energy = 5;
        }

        @Override
        public void tic() {
            wait++;
            if (wait == 4) {
                energy--;
                wait = 0;
            }
        }
    }

    public static class Player extends Animate {
        public Player(int x, int y) {
            super(x, y);
        }
    }

    public static class Inanimate extends Cell {
        private int wait = 0;

        public Inanimate(int x, int y) {
            super(x, y);
            energy = 1;
        }

        public void tic() {
            wait++;
            if (wait == 4) {
                energy = Math.min(energy + 1, 8);
                wait = 0;
            }
        }
    }

    private List<Cell> cells = new ArrayList<>();

    public CellFrame withCell(Cell cell) {
        cells.add(cell);
        return this;
    }

    public CellFrame withCells(Collection<Cell> others) {
        this.cells.addAll(others);
        return this;
    }

    public List<Cell> getCells() {
        return cells;
    }

    @Override
    public CellFrame copy() {
        CellFrame copy = new CellFrame(this.mainCell);
        copy.withCells(new ArrayList<>(this.cells));
        return copy;
    }

}
