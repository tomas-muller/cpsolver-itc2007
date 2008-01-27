package net.sf.cpsolver.itc.exam.neighbours;

import java.util.Vector;

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
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;

/**
 * Try to find a move that improves overall solution value.
 * Such a move can create some conflicts and therefore some
 * exams may become unassigned after it is assigned.
 * A lecture is selected randomly, a placement is selected randomly among
 * placements that are minimizing solution value.
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
public class ExBestMove implements NeighbourSelection {
    /** Constructor */
    public ExBestMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        ExModel model = (ExModel)solution.getModel();
        ExExam worstExam = null; double worstValue = 0;
        Vector assigned = new Vector(model.assignedVariables());
        int ex = ToolBox.random(assigned.size());
        for (int e=0;e<assigned.size();e++) {
            ExExam exam = (ExExam)assigned.elementAt((e+ex)%assigned.size());
            double value = exam.getAssignment().toDouble();
            if (worstExam==null || worstValue<value) {
                worstExam = exam; worstValue = value;
            }
        }
        ExPlacement bestPlacement = null; double bestValue = 0;
        if (worstExam==null || worstExam.getAssignment().toDouble()<=0) return null;
        int px = ToolBox.random(model.getNrPeriods());
        int rx = ToolBox.random(model.getRooms().size());
        for (int p=0;p<model.getNrPeriods();p++) {
            ExPeriod period = (ExPeriod)model.getPeriod((p+px)%model.getNrPeriods());
            if (period.getLength()<worstExam.getLength()) continue;
            for (int r=0;r<model.getRooms().size();r++) {
                ExRoom room = (ExRoom)model.getRooms().elementAt((r+rx)%model.getRooms().size());
                if (room.getSize()<worstExam.getStudents().size()) continue;
                ExPlacement placement = new ExPlacement(worstExam, period, room);
                double value = placement.toDouble();
                if (bestPlacement==null || bestValue>value) {
                    bestPlacement = placement; bestValue = value; 
                }
            }
        }
        if (bestPlacement==null || bestPlacement.equals(worstExam.getAssignment())) return null;
        return new ItcSimpleNeighbour(worstExam, bestPlacement, bestValue-worstValue);
    }
}
