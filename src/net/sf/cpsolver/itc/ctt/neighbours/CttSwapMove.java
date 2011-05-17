package net.sf.cpsolver.itc.ctt.neighbours;

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
        CttLecture lecture = (CttLecture)ToolBox.random(model.variables());
        CttPlacement placement = (CttPlacement)lecture.getAssignment();
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
                    CttPlacement conflict = (CttPlacement)room.getConstraint().getPlacement(day, slot);
                    if (conflict==null) {
                        if (inConflict==null) {
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) {
                                inConflict=Boolean.TRUE;
                                continue room;
                            }
                            for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                                if (curricula.getConstraint().getPlacement(day,slot)!=null) {
                                    inConflict=Boolean.TRUE;
                                    continue room;
                                }
                            }
                            inConflict=Boolean.FALSE;
                        } else if (inConflict.booleanValue()) continue room;
                        Neighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(lecture, new CttPlacement(lecture, room, day, slot));
                        if (!iHC || n.value()<=0) return n;
                    } else {
                        CttLecture confLect = (CttLecture)conflict.variable();
                        if (lecture.getCourse().equals(confLect.getCourse())) continue;
                        if (!confLect.getCourse().isAvailable(placement.getDay(), placement.getSlot())) continue;
                        if (!confLect.getCourse().getTeacher().equals(lecture.getCourse().getTeacher())) {
                            if (confLect.getCourse().getTeacher().getConstraint().getPlacement(placement.getDay(), placement.getSlot())!=null) continue;
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) continue;
                        }
                        for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                            CttPlacement conf = curricula.getConstraint().getPlacement(day,slot);
                            if (conf!=null && !conf.variable().equals(confLect)) continue room;
                        }
                        for (CttCurricula curricula: confLect.getCourse().getCurriculas()) {
                            CttPlacement conf = curricula.getConstraint().getPlacement(placement.getDay(), placement.getSlot());
                            if (conf!=null && !conf.variable().equals(lecture)) continue room;
                        }
                        return new ItcLazySwap<CttLecture, CttPlacement>(
                                new CttPlacement(lecture, room, day, slot),
                                new CttPlacement(confLect, placement.getRoom(), placement.getDay(), placement.getSlot()));
                    }
                }
            }
        return null;
    }
    
    public ItcSwap<CttLecture, CttPlacement> makeSwap(CttLecture l1, CttLecture l2, CttPlacement p1, CttPlacement p2, CttPlacement np1, CttPlacement np2) {
        double value = 0;
        value += np1.getRoomCapPenalty() + np2.getRoomCapPenalty() - p1.getRoomCapPenalty() - p2.getRoomCapPenalty();
        value += np1.getMinDaysPenalty() + np2.getMinDaysPenalty() - p1.getMinDaysPenalty() - p2.getMinDaysPenalty();
        value += np1.getRoomPenalty() + np2.getRoomPenalty() - p1.getRoomPenalty() - p2.getRoomPenalty();
        for (CttCurricula curricula: l1.getCourse().getCurriculas()) {
            if (l2.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np1) - curricula.getCompactPenalty(p1);
        }
        for (CttCurricula curricula: l2.getCourse().getCurriculas()) {
            if (l1.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np2) - curricula.getCompactPenalty(p2);
        }
        return new ItcSwap<CttLecture, CttPlacement>(np1, np2, value);
    }
}