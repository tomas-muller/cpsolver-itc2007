package net.sf.cpsolver.itc.ctt.neighbours;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ctt.model.CttCurricula;
import net.sf.cpsolver.itc.ctt.model.CttLecture;
import net.sf.cpsolver.itc.ctt.model.CttModel;
import net.sf.cpsolver.itc.ctt.model.CttPlacement;
import net.sf.cpsolver.itc.ctt.model.CttRoom;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

/**
 * A day and a time is changed for a randomly selected lecture.
 * First not conflicting day and time after a randomly selected day and
 * time is returned.
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
public class CttTimeMove implements NeighbourSelection<CttLecture, CttPlacement>, HillClimberSelection {
    private boolean iHC = false;
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public CttTimeMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver<CttLecture, CttPlacement> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }

    /** Neighbour selection */
    public Neighbour<CttLecture, CttPlacement> selectNeighbour(Solution<CttLecture, CttPlacement> solution) {
        CttModel model = (CttModel)solution.getModel();
        Assignment<CttLecture, CttPlacement> assignment = solution.getAssignment();
        CttLecture lecture = ToolBox.random(model.variables());
        CttPlacement placement = assignment.getValue(lecture);
        CttRoom room = (placement == null ?
                ToolBox.random(model.getRooms()):
                placement.getRoom());
        int dx = ToolBox.random(model.getNrDays());
        int sx = ToolBox.random(model.getNrSlotsPerDay());
        for (int d=0;d<model.getNrDays();d++)
            slot: for (int s=0;s<model.getNrSlotsPerDay();s++) {
                int day = (d + dx) % model.getNrDays();
                int slot = (s + sx) % model.getNrSlotsPerDay();
                if (!lecture.getCourse().isAvailable(day,slot)) continue;
                if (lecture.getCourse().getTeacher().getConstraint().getPlacement(assignment, day,slot)!=null) continue;
                if (room.getConstraint().getPlacement(assignment, day, slot)!=null) continue;
                for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                    if (curricula.getConstraint().getPlacement(assignment, day,slot)!=null) continue slot;
                }
                Neighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(assignment, lecture, new CttPlacement(lecture, room, day, slot));
                if (!iHC || n.value(assignment)<=0) return n;
            }
        return null;
    }
}