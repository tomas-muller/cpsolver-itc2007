package net.sf.cpsolver.itc.ctt.neighbours;

import java.util.Enumeration;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ctt.model.CttCurricula;
import net.sf.cpsolver.itc.ctt.model.CttLecture;
import net.sf.cpsolver.itc.ctt.model.CttModel;
import net.sf.cpsolver.itc.ctt.model.CttPlacement;
import net.sf.cpsolver.itc.ctt.model.CttRoom;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

import org.apache.log4j.Logger;

/**
 * A day and a time is changed for a randomly selected lecture.
 * First not conflicting day and time after a randomly selected day and
 * time is returned.
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
public class CttTimeMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(CttSwapMove.class);
    private boolean iHC = false;
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public CttTimeMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }

    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        CttModel model = (CttModel)solution.getModel();
        CttLecture lecture = (CttLecture)ToolBox.random(model.variables());
        CttRoom room = (lecture.getAssignment()==null?
                (CttRoom)ToolBox.random(model.getRooms()):
                ((CttPlacement)lecture.getAssignment()).getRoom());
        int dx = ToolBox.random(model.getNrDays());
        int sx = ToolBox.random(model.getNrSlotsPerDay());
        for (int d=0;d<model.getNrDays();d++)
            slot: for (int s=0;s<model.getNrSlotsPerDay();s++) {
                int day = (d + dx) % model.getNrDays();
                int slot = (s + sx) % model.getNrSlotsPerDay();
                if (!lecture.getCourse().isAvailable(day,slot)) continue;
                if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) continue;
                if (room.getConstraint().getPlacement(day, slot)!=null) continue;
                for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
                    CttCurricula curricula = (CttCurricula)e.nextElement();
                    if (curricula.getConstraint().getPlacement(day,slot)!=null) continue slot;
                }
                Neighbour n = new ItcSimpleNeighbour(lecture, new CttPlacement(lecture, room, day, slot));
                if (!iHC || n.value()<=0) return n;
            }
        return null;
    }
}