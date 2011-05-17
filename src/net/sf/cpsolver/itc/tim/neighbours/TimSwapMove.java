package net.sf.cpsolver.itc.tim.neighbours;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;
import net.sf.cpsolver.itc.tim.model.TTComp02Model;
import net.sf.cpsolver.itc.tim.model.TimEvent;
import net.sf.cpsolver.itc.tim.model.TimLocation;
import net.sf.cpsolver.itc.tim.model.TimRoom;
import net.sf.cpsolver.itc.tim.model.TimStudent;

/**
 * Swap two randomly selected events. An event is randomly selected,
 * a new time and room is selected -- if there is a conflicting event
 * it is swapped with the randomly selected event (if possible).
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not see
 * <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class TimSwapMove implements NeighbourSelection<TimEvent, TimLocation>, HillClimberSelection {
    private boolean iHC=false;
    
    /** Constructor */
    public TimSwapMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver<TimEvent, TimLocation> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour<TimEvent, TimLocation> selectNeighbour(Solution<TimEvent, TimLocation> solution) {
        TTComp02Model model = (TTComp02Model)solution.getModel();
        TimEvent event = model.variables().get(ToolBox.random(model.variables().size()));
        TimLocation location = (TimLocation)event.getAssignment();
        if (location==null) return null;
        int tx = ToolBox.random(45);
        int rx = ToolBox.random(event.rooms().size());
        for (int t=0;t<45;t++) {
            int time = (t + tx) % 45;
            if (!event.isAvailable(time)) continue;
            Boolean inConflict = null;
            room: for (int r=0;r<event.rooms().size();r++) {
                TimRoom room = event.rooms().get((r+rx)%event.rooms().size());
                TimLocation conflict = (TimLocation)room.getLocation(time);
                if (conflict==null) {
                    if (inConflict==null) {
                        for (TimStudent student: event.students()) {
                            if (student.getLocation(time)!=null) {
                                inConflict=Boolean.TRUE;
                                continue room;
                            }
                        }
                        inConflict=Boolean.FALSE;
                    } else if (inConflict.booleanValue()) continue room;
                    Neighbour<TimEvent, TimLocation> n = new ItcSimpleNeighbour<TimEvent, TimLocation>(event, new TimLocation(event, time, room));
                    if (!iHC || n.value()<=0) return n;
                } else {
                    TimEvent confEvt = (TimEvent)conflict.variable();
                    if (!confEvt.isAvailable(location.time())) continue;
                    if (!confEvt.rooms().contains(location.room())) continue;
                    for (TimStudent student: event.students()) {
                        TimLocation conf = student.getLocation(time);
                        if (conf!=null && !conf.variable().equals(confEvt)) return null;
                    }
                    for (TimStudent student: confEvt.students()) {
                        TimLocation conf = student.getLocation(location.time());
                        if (conf!=null && !conf.variable().equals(confEvt)) return null;
                    }
                    return new ItcLazySwap<TimEvent, TimLocation>(
                            new TimLocation(event, time, room),
                            new TimLocation(confEvt, location.time(), location.room()));
                }
            }
        }
        return null;
    }
}