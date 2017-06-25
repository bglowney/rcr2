package com.rcr2.cells;

import com.rcr2.EmptyFrame;
import com.rcr2.cells.CellFrame.Cell;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.rcr2.cells.CellFrame.*;

public class Functions {

    static class EmptyCellFrame extends CellFrame implements EmptyFrame<CellFrame> {
        @Override
        public String display() {
            return "empty";
        }
    }

    static final EmptyFrame EMPTY_CELL_FRAME = new EmptyCellFrame();

    private static Optional<CellFrame> cellFrameOptional(CellFrame cellFrame) {
        if (cellFrame.getCells().isEmpty())
            return Optional.of((CellFrame) EMPTY_CELL_FRAME);
        return Optional.of(cellFrame);
    }

    public static void setContextFunctions(CellsContext context) {
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
            });
        context.withPureFunction("threats", 1, args -> {
                List<Cell> predators = args.get(0).getCells()
                        .stream()
                        .filter(cell -> cell instanceof Animate)
                        .collect(Collectors.toList());

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(predators));
            });
        context.withPureFunction("resources", 1, args -> {
                List<Cell> resources = args.get(0).getCells()
                        .stream()
                        .filter(cell -> cell instanceof Inanimate
                                && context.places[cell.x][cell.y].above == null)
                        .collect(Collectors.toList());

                return cellFrameOptional(new CellFrame(context.mainCell).withCells(resources));
            });
        context.withSideEffect("consume", 0, args -> {
                return context.consume(context.mainCell)
                        ? Optional.of(new CellFrame(context.mainCell))
                        : Optional.empty();
            });
        context.withPureFunction("together", 2, args -> {
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
            });
        context.withPureFunction("apart", 2, args -> {
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
            });
        context.withSideEffect("advance", 1, args -> {
                // select random from list supplied and move toward it
                List<Cell> cells = args.get(0).getCells();
                if (cells.isEmpty())
                    return Optional.empty();

                Cell target = cells.get(ThreadLocalRandom.current().nextInt(0, cells.size()));
                return context.move(context.mainCell, target.x, target.y) ? Optional.of(context.successFrame()) : Optional.empty();
            });
        context.withSideEffect("flee", 1, args -> {

                class CandidatePlace implements Comparable<CandidatePlace> {
                    final CellsContext.Place place;
                    final int x;
                    final int y;
                    final double distance;

                    CandidatePlace(CellsContext.Place place, int x, int y, List<Cell> targets) {
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
                    CellsContext.Place[] row = context.places[i];
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
