package net.sf.cpsolver.itc.exam.neighbours;

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
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;

/**
 * Randomly select a new non conflicting placement for a randomly
 * selected exam (if such placement exists).
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
public class ExNotConflictingMove implements NeighbourSelection<ExExam, ExPlacement> {
    /** Constructor */
    public ExNotConflictingMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<ExExam, ExPlacement> solver) {}
    
    /** Neighbour selection */
    public Neighbour<ExExam, ExPlacement> selectNeighbour(Solution<ExExam, ExPlacement> solution) {
        ExModel model = (ExModel)solution.getModel();
        Assignment<ExExam, ExPlacement> assignment = solution.getAssignment();
        ExExam exam = (ExExam)ToolBox.random(model.variables());
        int px = ToolBox.random(model.getNrPeriods());
        for (int t=0;t<model.getNrPeriods();t++) {
            ExPeriod period = model.getPeriod((t + px) % model.getNrPeriods());
            if (exam.getLength()>period.getLength()) continue;
            ExPlacement p = exam.findPlacement(assignment, period);
            if (p==null) continue;
            if (!model.areBinaryViolationsAllowed() || !model.areDirectConflictsAllowed()) {
                if (model.inConflict(assignment, p)) continue;
            }
            return new ItcSimpleNeighbour<ExExam, ExPlacement>(assignment, exam, p);
        }
        return null;
    }
}
