package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;

/**
 * Randomly select a variable and assign it a new value. A value is 
 * also randomly selected and must not create any hard conflict
 * with other assignment. 
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

public class ItcNotConflictingMove<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V,T> {
    /** Constructor */
    public ItcNotConflictingMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<V,T> solver) {}
    /** Neighbour selection */
    public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Model<V,T> model = solution.getModel();
        Assignment<V, T> assignment = solution.getAssignment();
        V variable = ToolBox.random(model.variables());
        T value = ToolBox.random(variable.values());
        if (value.equals(assignment.getValue(variable))) return null;
        if (model.inConflict(assignment, value)) return null;
        return new ItcSimpleNeighbour<V,T>(assignment, variable,value);
    }
}
