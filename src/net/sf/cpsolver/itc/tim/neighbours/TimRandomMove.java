package net.sf.cpsolver.itc.tim.neighbours;

import java.util.Enumeration;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.tim.model.TimEvent;
import net.sf.cpsolver.itc.tim.model.TimLocation;
import net.sf.cpsolver.itc.tim.model.TimRoom;
import net.sf.cpsolver.itc.tim.model.TimStudent;

/**
 * Reassign a randomly selected event with a new time and room.
 * If parameter TimRandomMove.AllowIncreaseLastTimePenalty is false, 
 * it is not allowed to select last time slot of a day.
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
public class TimRandomMove implements NeighbourSelection {
    
    /** Constructor */
    public TimRandomMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver solver) {}
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        Model model = solution.getModel();
        TimEvent evt = (TimEvent)ToolBox.random(model.variables());
        TimLocation loc = (TimLocation)evt.getAssignment();
        int slot = ToolBox.random(45);
        if (!evt.isAvailable(slot)) return null;
        if (loc!=null && loc.time()==slot) return null;
        return findChange(evt, slot, (loc==null?null:loc.room()));
    }
    
    private static ItcSimpleNeighbour findChange(TimEvent event, int slot) {
        return findChange(event, slot, null);
    }
    
    private static ItcSimpleNeighbour findChange(TimEvent event, int slot, TimRoom prefRoom) {
        TimLocation loc = (TimLocation)event.getAssignment();
        if (loc==null || loc.time()!=slot)
            for (Enumeration e=event.students().elements();e.hasMoreElements();) {
                TimStudent s = (TimStudent)e.nextElement();
                if (s.getTable()[slot]!=null) return null;
            }
        if (prefRoom!=null && prefRoom.getLocation(slot)==null)
            return new ItcSimpleNeighbour(event, new TimLocation(event,slot,prefRoom));
        int rx = ToolBox.random(event.rooms().size());
        for (int r=0;r<event.rooms().size();r++) {
            TimRoom room = (TimRoom)event.rooms().elementAt((r+rx)%event.rooms().size());
            if (room.getLocation(slot)==null) 
                return new ItcSimpleNeighbour(event, new TimLocation(event,slot,room));
        }
        return null;
    }
}