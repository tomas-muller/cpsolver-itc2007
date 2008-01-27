package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
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

public class ItcNotConflictingMove implements NeighbourSelection {
    /** Constructor */
    public ItcNotConflictingMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        Model model = solution.getModel();
        Variable variable = (Variable)ToolBox.random(model.variables());
        Value value = (Value)ToolBox.random(variable.values());
        if (value.equals(variable.getAssignment())) return null;
        if (model.inConflict(value)) return null;
        return new ItcSimpleNeighbour(variable,value);
    }
}
