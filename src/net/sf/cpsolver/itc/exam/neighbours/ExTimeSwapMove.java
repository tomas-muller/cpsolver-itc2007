package net.sf.cpsolver.itc.exam.neighbours;


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

import org.apache.log4j.Logger;

/**
 * Try to swap periods between two randomly selected exams.
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
public class ExTimeSwapMove implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(ExTimeMove.class);
    
    /** Constructor */
    public ExTimeSwapMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        ExModel model = (ExModel)solution.getModel();
        ExExam exam = (ExExam)ToolBox.random(model.variables());
        ExPlacement placement = (ExPlacement)exam.getAssignment();
        if (placement==null) return null;
        int px = ToolBox.random(model.getNrPeriods());
        for (int t=0;t<model.getNrPeriods();t++) {
            int periodIdx = (t + px) % model.getNrPeriods();
            ExPeriod period = model.getPeriod(periodIdx);
            if (exam.getLength()>period.getLength()) continue;
            ExPlacement p = new ExPlacement(exam, period, placement.getRoom());
            Set conflicts = model.conflictValues(p);
            if (conflicts.size()==1) {
                Neighbour n = exam.findSwap(((ExPlacement)conflicts.iterator().next()).variable());
                if (n!=null) return n;
            }
        }
        return null;
    }
}