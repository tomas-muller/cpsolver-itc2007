package net.sf.cpsolver.itc.tim.neighbours;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;
import net.sf.cpsolver.itc.tim.model.TimEvent;
import net.sf.cpsolver.itc.tim.model.TimLocation;
import net.sf.cpsolver.itc.tim.model.TimModel;
import net.sf.cpsolver.itc.tim.model.TimPrecedence;
import net.sf.cpsolver.itc.tim.model.TimRoom;
import net.sf.cpsolver.itc.tim.model.TimStudent;

/**
 * Try to decrease number of violated precedence constraints by reassigning an event into a 
 * different time and room. A violated precedence constraint is selected randomly, 
 * one of its events (randomly selected) is placed in a different time and room that
 * does not violate the selected constraint. 
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
public class TimPrecedenceMove implements NeighbourSelection<TimEvent, TimLocation>, HillClimberSelection {
    private boolean iHC=false;

    /** Constructor */
    public TimPrecedenceMove(DataProperties properties) {
    }
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    /** Initialization */
    public void init(Solver<TimEvent, TimLocation> solver) {}

    /** Neighbour selection */
    public Neighbour<TimEvent, TimLocation> selectNeighbour(Solution<TimEvent, TimLocation> solution) {
        TimModel model = (TimModel)solution.getModel();
        Assignment<TimEvent, TimLocation> assignment = solution.getAssignment();
        if (model.precedenceViolations(assignment, false)<=0) return null;
        int px = ToolBox.random(model.getPrecedences().size());
        for (int p=0;p<model.getPrecedences().size();p++) {
            TimPrecedence precedence = model.getPrecedences().get((p+px)%model.getPrecedences().size());
            if (precedence.isHardPrecedence()) continue;
            if (precedence.isSatisfied(assignment)) continue;
            int swap = ToolBox.random(3);
            if (swap==0) {
                Neighbour<TimEvent, TimLocation> n = ((Swapable<TimEvent, TimLocation>)precedence.first()).findSwap(assignment, precedence.second());
                if (n!=null) return n;
            }
            TimEvent event = (TimEvent)(ToolBox.random(2)==0?precedence.first():precedence.second());
            TimEvent other = (TimEvent)precedence.another(event);
            TimLocation otherLoc = (TimLocation)assignment.getValue(other);
            int tx = ToolBox.random(45);
            int rx = ToolBox.random(event.rooms().size());
            boolean swapRoom = (ToolBox.random(2)==0);
            time: for (int t=0;t<45;t++) {
                int time = (t + tx) % 45;
                if (!event.isAvailable(time)) continue;
                if (!precedence.isConsistent(otherLoc,t)) continue;
                for (TimStudent student: event.students()) {
                    if (student.getLocation(assignment, time)!=null) continue time;
                }
                for (int r=0;r<event.rooms().size();r++) {
                    TimRoom room = event.rooms().get((r+rx)%event.rooms().size());
                    if (room.getLocation(assignment, time)!=null) {
                        if (swapRoom) {
                            Neighbour<TimEvent, TimLocation> n = event.findSwap(assignment, room.getLocation(assignment, time).variable());
                            if (n!=null) return n;
                        }
                    } else {
                        Neighbour<TimEvent, TimLocation> n = new ItcSimpleNeighbour<TimEvent, TimLocation>(assignment, event, new TimLocation(event, time, room));                    
                        if (!iHC || n.value(assignment)<=0) return n;
                    }
                }
            }
            if (swap==1) {
                Neighbour<TimEvent, TimLocation> n = ((Swapable<TimEvent, TimLocation>)precedence.first()).findSwap(assignment, precedence.second());
                if (n!=null) return n;
            }
            TimEvent x=event; event=other; other=x;
            otherLoc = (TimLocation)assignment.getValue(other);
            tx = ToolBox.random(45);
            rx = ToolBox.random(event.rooms().size());
            time: for (int t=0;t<45;t++) {
                int time = (t + tx) % 45;
                if (!event.isAvailable(time)) continue;
                if (!precedence.isConsistent(otherLoc,t)) continue;
                for (TimStudent student: event.students()) {
                    if (student.getLocation(assignment, time)!=null) continue time;
                }
                for (int r=0;r<event.rooms().size();r++) {
                    TimRoom room = event.rooms().get((r+rx)%event.rooms().size());
                    if (room.getLocation(assignment, time)!=null) {
                        if (swapRoom) {
                            Neighbour<TimEvent, TimLocation> n = event.findSwap(assignment, room.getLocation(assignment, time).variable());
                            if (n!=null) return n;
                        }
                    } else {
                        Neighbour<TimEvent, TimLocation> n = new ItcSimpleNeighbour<TimEvent, TimLocation>(assignment, event, new TimLocation(event, time, room));                    
                        if (!iHC || n.value(assignment)<=0) return n;
                    }
                }
            }
            if (swap==2) {
                Neighbour<TimEvent, TimLocation> n = ((Swapable<TimEvent, TimLocation>)precedence.first()).findSwap(assignment, precedence.second());
                if (n!=null) return n;
            }
        }
        return null;
    }

}
