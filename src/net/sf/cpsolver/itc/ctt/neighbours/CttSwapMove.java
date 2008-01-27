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
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

import org.apache.log4j.Logger;

/**
 * Two randomly selected lectures are swapped.
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
public class CttSwapMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(CttSwapMove.class);
    private boolean iHC=false;
    
    /** Constructor */
    public CttSwapMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
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
                    CttRoom room = (CttRoom)model.getRooms().elementAt((r+rx)%model.getRooms().size());
                    CttPlacement conflict = (CttPlacement)room.getConstraint().getPlacement(day, slot);
                    if (conflict==null) {
                        if (inConflict==null) {
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) {
                                inConflict=Boolean.TRUE;
                                continue room;
                            }
                            for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
                                CttCurricula curricula = (CttCurricula)e.nextElement();
                                if (curricula.getConstraint().getPlacement(day,slot)!=null) {
                                    inConflict=Boolean.TRUE;
                                    continue room;
                                }
                            }
                            inConflict=Boolean.FALSE;
                        } else if (inConflict.booleanValue()) continue room;
                        Neighbour n = new ItcSimpleNeighbour(lecture, new CttPlacement(lecture, room, day, slot));
                        if (!iHC || n.value()<=0) return n;
                    } else {
                        CttLecture confLect = (CttLecture)conflict.variable();
                        if (lecture.getCourse().equals(confLect.getCourse())) continue;
                        if (!confLect.getCourse().isAvailable(placement.getDay(), placement.getSlot())) continue;
                        if (!confLect.getCourse().getTeacher().equals(lecture.getCourse().getTeacher())) {
                            if (confLect.getCourse().getTeacher().getConstraint().getPlacement(placement.getDay(), placement.getSlot())!=null) continue;
                            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) continue;
                        }
                        for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
                            CttCurricula curricula = (CttCurricula)e.nextElement();
                            CttPlacement conf = curricula.getConstraint().getPlacement(day,slot);
                            if (conf!=null && !conf.variable().equals(confLect)) continue room;
                        }
                        for (Enumeration e=confLect.getCourse().getCurriculas().elements();e.hasMoreElements();) {
                            CttCurricula curricula = (CttCurricula)e.nextElement();
                            CttPlacement conf = curricula.getConstraint().getPlacement(placement.getDay(), placement.getSlot());
                            if (conf!=null && !conf.variable().equals(lecture)) continue room;
                        }
                        return new ItcLazySwap(
                                new CttPlacement(lecture, room, day, slot),
                                new CttPlacement(confLect, placement.getRoom(), placement.getDay(), placement.getSlot()));
                    }
                }
            }
        return null;
    }
    
    public ItcSwap makeSwap(CttLecture l1, CttLecture l2, CttPlacement p1, CttPlacement p2, CttPlacement np1, CttPlacement np2) {
        double value = 0;
        value += np1.getRoomCapPenalty() + np2.getRoomCapPenalty() - p1.getRoomCapPenalty() - p2.getRoomCapPenalty();
        value += np1.getMinDaysPenalty() + np2.getMinDaysPenalty() - p1.getMinDaysPenalty() - p2.getMinDaysPenalty();
        value += np1.getRoomPenalty() + np2.getRoomPenalty() - p1.getRoomPenalty() - p2.getRoomPenalty();
        for (Enumeration e=l1.getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            if (l2.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np1) - curricula.getCompactPenalty(p1);
        }
        for (Enumeration e=l2.getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            if (l1.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np2) - curricula.getCompactPenalty(p2);
        }
        return new ItcSwap(np1, np2, value);
    }
}