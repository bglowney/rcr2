package com.rcr2.cells;

import com.rcr2.FrameProvider;
import com.rcr2.Persistence;
import com.rcr2.cells.CellFrame.Animate;
import com.rcr2.cells.CellFrame.Inanimate;
import com.rcr2.cells.CellFrame.Player;
import com.rcr2.cells.CellsContext.Place;
import com.rcr2.impl.CliSession;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CellSession extends CliSession<CellFrame,CellsContext> {

    public CellSession(Persistence<CellFrame,CellsContext> persistence, CellsContext cellsContext, FrameProvider<CellFrame> frameProvider) {
        super(
            persistence,
            cellsContext,
            new CellFrame(cellsContext.mainCell),
            frameProvider
        );
        this.setText(this.currentFrame);
    }

    @Override
    protected void beforeStep() {

    }

    @Override
    protected void afterStep() {
        Place[][] places = context.places;

        for (int i = 0; i < places.length; i++) {
            Place[] row = places[i];
            for (int j = 0; j < row.length; j++) {
                Place place = row[j];
                if (place != null && place.above != null) {
                    Animate animate = (Animate)place.above;
                    animate.tic();
                    if (!(place.above instanceof Player)
                        && ThreadLocalRandom.current().nextInt(0, 4) == 0) {
                        perturbAnimate(animate);
                    }
                }
                if (place != null && place.ground != null) {
                    Inanimate inanimate = (Inanimate)place.ground;
                    inanimate.tic();
                    if (ThreadLocalRandom.current().nextInt(0, 4) == 0)
                        perturbInanimate(inanimate);
                }
            }
        }
    }

    private void perturbInanimate(Inanimate inanimate) {
        // try to multiply
        if (inanimate.energy > 4) {
            List<Place> nearby = context.nearby(inanimate)
                .stream()
                .filter(place -> place.ground == null)
                .collect(Collectors.toList());

            if (nearby.isEmpty()) return;

            nearby.stream()
                .skip(ThreadLocalRandom.current().nextInt(0, nearby.size()))
                .findFirst()
                .ifPresent(place -> {
                    place.ground = new Inanimate(place.x, place.y);
                    inanimate.energy -= 3;
                });
        }
    }

    private void perturbAnimate(Animate animate) {
        if (animate.energy < 10) {
            // try to consume
            if (context.consume(animate)) return;

            // otherwise move to a nearby resource
            List<Inanimate> inanimates = context.nearby(animate)
                    .stream()
                    .filter(place -> place.above == null)
                    .filter(place -> place.ground != null)
                    .map(place -> (Inanimate)place.ground)
                    .collect(Collectors.toList());

            if (inanimates.isEmpty()) return;

            inanimates
                    .stream()
                    .skip(ThreadLocalRandom.current().nextInt(0, inanimates.size()))
                    .findFirst()
                    .ifPresent(inanimate -> {
                        context.move(animate, inanimate.x, inanimate.y);
                        animate.energy--;
                    });
        } else
            context.multiply(animate);
    }

    @Override
    protected String displayDebug() {
        return CellFrame.displayCell(context.mainCell) + "\n"
               + CellsContext.displayCells(context.places);
    }
}
