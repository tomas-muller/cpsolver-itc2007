package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import java.util.Set;

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
 * Randomly select a variable and assign it a new randomly 
 * selected value. Unlike {@link ItcNotConflictingMove}, a value
 * can conflict with assignments of other variables. 
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
public class ItcRandomMove implements NeighbourSelection {
    private double iConflictWeight = 100;
    private double iValueWeight = 1;
    public ItcRandomMove(DataProperties properties) {
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }
    public void init(Solver solver) {}
    public Neighbour selectNeighbour(Solution solution) {
        Model model = solution.getModel();
        boolean canConflict = model.nrAssignedVariables()!=0;
        Variable variable = (Variable)ToolBox.random(model.variables());
        Value value = (Value)ToolBox.random(variable.values());
        if (value.equals(variable.getAssignment())) return null;
        Set conflicts = model.conflictValues(value);
        double eval = iValueWeight * value.toDouble();
        if (variable.getAssignment()!=null)
            eval -= iValueWeight * variable.getAssignment().toDouble();
        else
            eval -= iConflictWeight;
        eval += iConflictWeight * model.conflictValues(value).size();
        return new ItcSimpleNeighbour(variable,value,eval);
    }
}