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
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

/**
 * Two randomly selected lectures are swapped.
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
public class CttSwapMove implements NeighbourSelection<CttLecture, CttPlacement>, HillClimberSelection {
    private boolean iHC=false;
    
    /** Constructor */
    public CttSwapMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<CttLecture, CttPlacement> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour<CttLecture, CttPlacement> selectNeighbour(Solution<CttLecture, CttPlacement> solution) {
        CttModel model = (CttModel)solution.getModel();
        Assignment<CttLecture, CttPlacement> assignment = solution.getAssignment();
        CttLecture lecture = (CttLecture)ToolBox.random(model.variables());
        CttPlacement placement = assignment.getValue(lecture);
        if (placement==null) return null;
        int dx = ToolBox.random(model.getNrDays());
        int sx = ToolBox.random(model.getNrSlotsPerDay());
        int rx = ToolBox.random(model.getRooms().size());
        for (int d=0;d<model.getNrDays();d++)
            for (int s=0;s<model.getNrSlotsPerDay();s++) {
                int day = (d + dx) % model.getNrDays();
                int slot = (s + sx) % model.getNrSlotsPerDay();
                if (!lecture.getCourse().isAvailable(day,slot)) continue;
                Boolean inConflict = null;
                room: for (int r=0;r<model.getRooms().size();r++) {
                    CttRoom room = model.getRooms().get((r+rx)%model.getRooms().size());
                    CttPlacement conflict = (CttPlacement)room.getConstraint().getPlacement(assignment, day, slot);
                    if (conflict==null) {
                        if (inConflict==null) {
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(assignment, day,slot)!=null) {
                                inConflict=Boolean.TRUE;
                                continue room;
                            }
                            for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                                if (curricula.getConstraint().getPlacement(assignment, day,slot)!=null) {
                                    inConflict=Boolean.TRUE;
                                    continue room;
                                }
                            }
                            inConflict=Boolean.FALSE;
                        } else if (inConflict.booleanValue()) continue room;
                        Neighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(assignment, lecture, new CttPlacement(lecture, room, day, slot));
                        if (!iHC || n.value(assignment)<=0) return n;
                    } else {
                        CttLecture confLect = (CttLecture)conflict.variable();
                        if (lecture.getCourse().equals(confLect.getCourse())) continue;
                        if (!confLect.getCourse().isAvailable(placement.getDay(), placement.getSlot())) continue;
                        if (!confLect.getCourse().getTeacher().equals(lecture.getCourse().getTeacher())) {
                            if (confLect.getCourse().getTeacher().getConstraint().getPlacement(assignment, placement.getDay(), placement.getSlot())!=null) continue;
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(assignment, day,slot)!=null) continue;
                        }
                        for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                            CttPlacement conf = curricula.getConstraint().getPlacement(assignment, day,slot);
                            if (conf!=null && !conf.variable().equals(confLect)) continue room;
                        }
                        for (CttCurricula curricula: confLect.getCourse().getCurriculas()) {
                            CttPlacement conf = curricula.getConstraint().getPlacement(assignment, placement.getDay(), placement.getSlot());
                            if (conf!=null && !conf.variable().equals(lecture)) continue room;
                        }
                        return new ItcLazySwap<CttLecture, CttPlacement>(assignment,
                                new CttPlacement(lecture, room, day, slot),
                                new CttPlacement(confLect, placement.getRoom(), placement.getDay(), placement.getSlot()));
                    }
                }
            }
        return null;
    }
    
    public ItcSwap<CttLecture, CttPlacement> makeSwap(Assignment<CttLecture, CttPlacement> assignment, CttLecture l1, CttLecture l2, CttPlacement p1, CttPlacement p2, CttPlacement np1, CttPlacement np2) {
        double value = 0;
        value += np1.getRoomCapPenalty() + np2.getRoomCapPenalty() - p1.getRoomCapPenalty() - p2.getRoomCapPenalty();
        value += np1.getMinDaysPenalty(assignment) + np2.getMinDaysPenalty(assignment) - p1.getMinDaysPenalty(assignment) - p2.getMinDaysPenalty(assignment);
        value += np1.getRoomPenalty(assignment) + np2.getRoomPenalty(assignment) - p1.getRoomPenalty(assignment) - p2.getRoomPenalty(assignment);
        for (CttCurricula curricula: l1.getCourse().getCurriculas()) {
            if (l2.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(assignment, np1) - curricula.getCompactPenalty(assignment, p1);
        }
        for (CttCurricula curricula: l2.getCourse().getCurriculas()) {
            if (l1.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(assignment, np2) - curricula.getCompactPenalty(assignment, p2);
        }
        return new ItcSwap<CttLecture, CttPlacement>(np1, np2, value);
    }
}