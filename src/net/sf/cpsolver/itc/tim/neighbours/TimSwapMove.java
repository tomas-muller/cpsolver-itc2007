package net.sf.cpsolver.itc.tim.neighbours;

import java.util.Enumeration;

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

import org.apache.log4j.Logger;

/**
 * Swap two randomly selected events. An event is randomly selected,
 * a new time and room is selected -- if there is a conflicting event
 * it is swapped with the randomly selected event (if possible).
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class TimSwapMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(TimSwapMove.class);
    private boolean iHC=false;
    
    /** Constructor */
    public TimSwapMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        TTComp02Model model = (TTComp02Model)solution.getModel();
        TimEvent event = (TimEvent)model.variables().elementAt(ToolBox.random(model.variables().size()));
        TimLocation location = (TimLocation)event.getAssignment();
        if (location==null) return null;
        int tx = ToolBox.random(45);
        int rx = ToolBox.random(event.rooms().size());
        for (int t=0;t<45;t++) {
            int time = (t + tx) % 45;
            if (!event.isAvailable(time)) continue;
            Boolean inConflict = null;
            room: for (int r=0;r<event.rooms().size();r++) {
                TimRoom room = (TimRoom)event.rooms().elementAt((r+rx)%event.rooms().size());
                TimLocation conflict = (TimLocation)room.getLocation(time);
                if (conflict==null) {
                    if (inConflict==null) {
                        for (Enumeration e=event.students().elements();e.hasMoreElements();) {
                            TimStudent student = (TimStudent)e.nextElement();
                            if (student.getLocation(time)!=null) {
                                inConflict=Boolean.TRUE;
                                continue room;
                            }
                        }
                        inConflict=Boolean.FALSE;
                    } else if (inConflict.booleanValue()) continue room;
                    Neighbour n = new ItcSimpleNeighbour(event, new TimLocation(event, time, room));
                    if (!iHC || n.value()<=0) return n;
                } else {
                    TimEvent confEvt = (TimEvent)conflict.variable();
                    if (!confEvt.isAvailable(location.time())) continue;
                    if (!confEvt.rooms().contains(location.room())) continue;
                    for (Enumeration e=event.students().elements();e.hasMoreElements();) {
                        TimStudent student = (TimStudent)e.nextElement();
                        TimLocation conf = student.getLocation(time);
                        if (conf!=null && !conf.variable().equals(confEvt)) return null;
                    }
                    for (Enumeration e=confEvt.students().elements();e.hasMoreElements();) {
                        TimStudent student = (TimStudent)e.nextElement();
                        TimLocation conf = student.getLocation(location.time());
                        if (conf!=null && !conf.variable().equals(confEvt)) return null;
                    }
                    return new ItcLazySwap(
                            new TimLocation(event, time, room),
                            new TimLocation(confEvt, location.time(), location.room()));
                }
            }
        }
        return null;
    }
}