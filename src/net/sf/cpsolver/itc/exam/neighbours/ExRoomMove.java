package net.sf.cpsolver.itc.exam.neighbours;

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
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

import org.apache.log4j.Logger;

/**
 * Try to find a new room assignment for a randomly selected exam.
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
public class ExRoomMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(ExRoomMove.class);
    private boolean iHC = false;
    /** Constructor */
    public ExRoomMove(DataProperties properties) {
    }
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }
    
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        ExModel model = (ExModel)solution.getModel();
        ExExam exam = (ExExam)ToolBox.random(model.variables());
        ExPlacement placement = (ExPlacement)exam.getAssignment();
        int periodIdx = (placement==null?ToolBox.random(model.getNrPeriods()):placement.getPeriodIndex());
        ExPeriod period = model.getPeriod(periodIdx);
        if (exam.getLength()>period.getLength()) return null;
        int rx = ToolBox.random(model.getRooms().size());
        for (int r=0;r<model.getRooms().size();r++) {
            ExRoom room = (ExRoom)model.getRooms().elementAt((r+rx)%model.getRooms().size());
            if (room.inConflict(exam,period)) continue;
            if (room.getSize()<exam.getStudents().size()) continue;
            ExPlacement p = new ExPlacement(exam, period, room);
            if (!model.areBinaryViolationsAllowed() || !model.areDirectConflictsAllowed()) {
                if (model.inConflict(p)) continue;
            }
            double value = 0;
            if (placement==null) {
                value = p.toDouble();
            } else {
                value = model.getMixedDurationWeight()*(p.mixedDurationsPenalty()-placement.mixedDurationsPenalty()) +
                        room.getPenalty() - placement.getRoom().getPenalty(); 
            }
            ItcSimpleNeighbour n = new ItcSimpleNeighbour(exam, p, value);
            if (!iHC || n.value()<=0) return n;
        }
        return null;
    }
}
