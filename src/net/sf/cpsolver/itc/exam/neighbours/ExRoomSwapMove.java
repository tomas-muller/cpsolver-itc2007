package net.sf.cpsolver.itc.exam.neighbours;


import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.exam.model.ExExam;
import net.sf.cpsolver.itc.exam.model.ExModel;
import net.sf.cpsolver.itc.exam.model.ExPeriod;
import net.sf.cpsolver.itc.exam.model.ExPlacement;
import net.sf.cpsolver.itc.exam.model.ExRoom;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

/**
 * Try to swap room assignments between two randomly selected exams.
 * 
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
public class ExRoomSwapMove implements NeighbourSelection<ExExam, ExPlacement>, HillClimberSelection {
    private boolean iHC=false;
    
    /** Constructor */
    public ExRoomSwapMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<ExExam, ExPlacement> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour<ExExam, ExPlacement> selectNeighbour(Solution<ExExam, ExPlacement> solution) {
        ExModel model = (ExModel)solution.getModel();
        ExExam exam = (ExExam)ToolBox.random(model.variables());
        ExPlacement placement = (ExPlacement)exam.getAssignment();
        if (placement==null) return null;
        ExPeriod period = placement.getPeriod();
        int rx = ToolBox.random(model.getRooms().size());
        for (int r=0;r<model.getRooms().size();r++) {
            ExRoom room = model.getRooms().get((r+rx)%model.getRooms().size());
            if (room.getSize()<exam.getStudents().size()) continue;
            ExPlacement p = new ExPlacement(exam, period, room);
            Set<ExPlacement> confs = new HashSet<ExPlacement>();
            room.computeConflicts(p, confs);
            if (confs.size()==1) {
                Neighbour<ExExam, ExPlacement> n = findRoomSwap(exam, confs.iterator().next().variable());
                if (n!=null && (!iHC || n.value()<=0)) return n;
            }
        }
        return null;
    }
    
    private int mixedDurationsPenalty(ExPlacement placement, int newLength, int weight) {
        int sameLength = 1, diffLength = 0;
        for (ExPlacement p: placement.getRoom().getExams(placement.getPeriod())) {
            if (p.variable().equals(placement.variable())) continue;
            if (p.variable().getLength()!=newLength) diffLength++; else sameLength++;
        }
        if (diffLength>0 && sameLength==1) return weight;
        return 0;
    }
    
    private ItcSwap<ExExam, ExPlacement> findRoomSwap(ExExam ex1, ExExam ex2) {
        ExModel model = (ExModel)ex1.getModel();
        ExPlacement p1 = ex1.getAssignment();
        ExPlacement p2 = ex2.getAssignment();
        if (p1==null || p2==null) return null;
        if (p1.getPeriodIndex()!=p2.getPeriodIndex()) return null;
        ExRoom r1 = p1.getRoom();
        ExRoom r2 = p2.getRoom();
        if (r1.equals(r2)) return null;
        if (ex1.isRoomExclusive() && r2.getExams(p2.getPeriod()).size()>1) return null;
        if (ex2.isRoomExclusive() && r1.getExams(p1.getPeriod()).size()>1) return null;
        if (r2.getAvailableSpace(p2.getPeriod())+ex2.getStudents().size()<ex1.getStudents().size()) return null;
        if (r1.getAvailableSpace(p1.getPeriod())+ex1.getStudents().size()<ex2.getStudents().size()) return null;
        ExPlacement np1 = new ExPlacement(ex1, p2.getPeriod(), p2.getRoom());
        ExPlacement np2 = new ExPlacement(ex2, p1.getPeriod(), p1.getRoom());
        double value = 0;
        if (ex1.getLength()!=ex2.getLength()) {
            value = mixedDurationsPenalty(p1,ex2.getLength(),model.getMixedDurationWeight())+
                    mixedDurationsPenalty(p2,ex1.getLength(),model.getMixedDurationWeight())-
                    model.getMixedDurationWeight()*(p1.mixedDurationsPenalty()+p2.mixedDurationsPenalty());
        }
        return new ItcSwap<ExExam, ExPlacement>(np1, np2, value); 
    }    
}