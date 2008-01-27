package net.sf.cpsolver.itc.ctt.neighbours;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ctt.model.CttLecture;
import net.sf.cpsolver.itc.ctt.model.CttModel;
import net.sf.cpsolver.itc.ctt.model.CttPlacement;
import net.sf.cpsolver.itc.ctt.model.CttRoom;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

import org.apache.log4j.Logger;

/**
 * A room is changed for a randomly selected lecture.
 * First not conflicting room after a randomly selected one is returned.
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
public class CttRoomMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(CttRoomMove.class);
    private boolean iHC = false;
    
    /** Constructor */
    public CttRoomMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        CttModel model = (CttModel)solution.getModel();
        CttLecture lecture = (CttLecture)ToolBox.random(model.variables());
        CttPlacement placement = (CttPlacement)lecture.getAssignment();
        int day = (placement==null?ToolBox.random(model.getNrDays()):placement.getDay());
        int slot = (placement==null?ToolBox.random(model.getNrSlotsPerDay()):placement.getSlot());
        if (placement==null && !lecture.getCourse().isAvailable(day, slot)) return null;
        int rx = ToolBox.random(model.getRooms().size());
        for (int r=0;r<model.getRooms().size();r++) {
            CttRoom room = (CttRoom)model.getRooms().elementAt((r+rx)%model.getRooms().size());
            CttPlacement conflict = (CttPlacement)room.getConstraint().getPlacement(day, slot);
            if (conflict==null) {
                ItcSimpleNeighbour n = new ItcSimpleNeighbour(lecture, new CttPlacement(lecture, room, day, slot));
                if (!iHC || n.value()<=0) return n;
            }
        }
        return null;
    }
}
