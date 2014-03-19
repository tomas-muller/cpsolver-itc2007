package net.sf.cpsolver.itc.exam.neighbours;


import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.exam.model.ExExam;
import net.sf.cpsolver.itc.exam.model.ExModel;
import net.sf.cpsolver.itc.exam.model.ExPeriod;
import net.sf.cpsolver.itc.exam.model.ExPlacement;

/**
 * Try to swap periods between two randomly selected exams.
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
public class ExTimeSwapMove implements NeighbourSelection<ExExam, ExPlacement> {
	
    /** Constructor */
    public ExTimeSwapMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<ExExam, ExPlacement> solver) {}
    
    /** Neighbour selection */
    public Neighbour<ExExam, ExPlacement> selectNeighbour(Solution<ExExam, ExPlacement> solution) {
        ExModel model = (ExModel)solution.getModel();
        Assignment<ExExam, ExPlacement> assignment = solution.getAssignment();
        ExExam exam = (ExExam)ToolBox.random(model.variables());
        ExPlacement placement = assignment.getValue(exam);
        if (placement==null) return null;
        int px = ToolBox.random(model.getNrPeriods());
        for (int t=0;t<model.getNrPeriods();t++) {
            int periodIdx = (t + px) % model.getNrPeriods();
            ExPeriod period = model.getPeriod(periodIdx);
            if (exam.getLength()>period.getLength()) continue;
            ExPlacement p = new ExPlacement(exam, period, placement.getRoom());
            Set<ExPlacement> conflicts = model.conflictValues(assignment, p);
            if (conflicts.size()==1) {
                Neighbour<ExExam, ExPlacement> n = exam.findSwap(assignment, conflicts.iterator().next().variable());
                if (n!=null) return n;
            }
        }
        return null;
    }
}