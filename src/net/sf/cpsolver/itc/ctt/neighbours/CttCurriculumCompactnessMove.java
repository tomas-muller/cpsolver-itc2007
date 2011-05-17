package net.sf.cpsolver.itc.ctt.neighbours;

import java.util.ArrayList;
import java.util.List;

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

/**
 * Try to find a move that decrease curriculum compactness penalty.
 * A curricula is selected randomly, a lecture that is not adjacent to any other
 * is selected, and placed to some other available time that has an 
 * adjacent lecture (if such placement exists and does not create any conflict).
 * A different room may be assigned to the lecture as well.
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
public class CttCurriculumCompactnessMove implements NeighbourSelection<CttLecture, CttPlacement>, HillClimberSelection {
    private boolean iHC = false;
    
    /** Constructor */
    public CttCurriculumCompactnessMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<CttLecture, CttPlacement> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour<CttLecture, CttPlacement> selectNeighbour(Solution<CttLecture, CttPlacement> solution) {
        CttModel model = (CttModel)solution.getModel();
        if (model.getCompactPenalty(false)<=0) return null;
        int cx = ToolBox.random(model.getCurriculas().size());
        for (int c=0;c<model.getCurriculas().size();c++) {
            CttCurricula curricula = model.getCurriculas().get((c+cx)%model.getCurriculas().size());
            List<CttPlacement> adepts = new ArrayList<CttPlacement>();
            for (int d=0;d<model.getNrDays();d++) {
                for (int s=0;s<model.getNrSlotsPerDay();s++) {
                    CttPlacement p = curricula.getConstraint().getPlacement(d, s);
                    if (p==null) continue;
                    CttPlacement prev = (s==0?null:curricula.getConstraint().getPlacement(d, s-1));
                    CttPlacement next = (s+1==model.getNrSlotsPerDay()?null:curricula.getConstraint().getPlacement(d, s+1));
                    if (next==null && prev==null) {
                        adepts.add(p);
                    }
                }
            }
            if (!adepts.isEmpty()) {
                int ax = ToolBox.random(adepts.size());
                for (int a=0;a<adepts.size();a++) {
                    CttPlacement adept = adepts.get((a+ax)%adepts.size());
                    Neighbour<CttLecture, CttPlacement> n = findNeighbour(curricula, adept);
                    if (n!=null) return n;
                }
            }
        }
        return null;
    }
    
    private Neighbour<CttLecture, CttPlacement> findNeighbour(CttCurricula curricula, CttPlacement placement) {
        int compactPenalty = placement.getCompactPenalty();
        CttModel model = (CttModel)placement.variable().getModel();
        int dx = ToolBox.random(model.getNrDays());
        int sx = ToolBox.random(model.getNrSlotsPerDay());
        for (int d=0;d<model.getNrDays();d++) {
            int day = (d+dx) % model.getNrDays();
            for (int s=0;s<model.getNrSlotsPerDay();s++) {
                int slot = (s+sx) % model.getNrSlotsPerDay();
                if (curricula.getConstraint().getPlacement(day, slot)!=null) continue;
                CttPlacement prev = (slot==0?null:curricula.getConstraint().getPlacement(day, slot-1));
                CttPlacement next = (slot+1==model.getNrSlotsPerDay()?null:curricula.getConstraint().getPlacement(day, slot+1));
                if ((next==null || next.equals(placement)) && (prev==null || prev.equals(placement))) continue;
                Neighbour<CttLecture, CttPlacement> n = findNeighbour((CttLecture)placement.variable(), compactPenalty, day, slot, placement.getRoom());
                if (n!=null) return n;
            }
        }
        return null;
    }    
    
    private Neighbour<CttLecture, CttPlacement> findNeighbour(CttLecture lecture, int compactPenalty, int day, int slot, CttRoom originalRoom) {
        if (!lecture.getCourse().isAvailable(day,slot)) return null;
        if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) return null;
        for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
            if (curricula.getConstraint().getPlacement(day,slot)!=null) return null;
        }
        if ((CttPlacement)originalRoom.getConstraint().getPlacement(day, slot)==null) {
            CttPlacement newPlacement = new CttPlacement(lecture, originalRoom, day, slot);
            int compactPenaltyChange = newPlacement.getCompactPenalty() - compactPenalty;
            ItcSimpleNeighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(lecture, newPlacement);
            if ((!iHC || n.value()<=0) && compactPenaltyChange<0) return n;
        }
        int rx = ToolBox.random(lecture.getCourse().getModel().getRooms().size());
        for (int r=0;r<lecture.getCourse().getModel().getRooms().size();r++) {
            CttRoom room = lecture.getCourse().getModel().getRooms().get((r+rx)%lecture.getCourse().getModel().getRooms().size());
            if (room.getSize()<lecture.getCourse().getNrStudents()) continue;
            if ((CttPlacement)room.getConstraint().getPlacement(day, slot)==null) {
                CttPlacement newPlacement = new CttPlacement(lecture, room, day, slot);
                int compactPenaltyChange = newPlacement.getCompactPenalty() - compactPenalty;
                ItcSimpleNeighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(lecture, newPlacement);
                if (iHC && n.value()>0) continue;
                if (compactPenaltyChange<0) return n;
            }
        }
        return null;
    }
    
}