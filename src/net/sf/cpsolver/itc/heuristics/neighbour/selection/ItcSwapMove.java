package net.sf.cpsolver.itc.heuristics.neighbour.selection;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Select two variables and swap their assignments.
 * This neighbour selection can only be used when 
 * variables are implementing {@link Swapable} interface.
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
public class ItcSwapMove implements NeighbourSelection {
    private double iValueWeight = 1;
    /** Constructor */
    public ItcSwapMove(DataProperties properties) {
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }
    /** Initialization */
    public void init(Solver solver) {}
    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        Model model = solution.getModel();
        Variable variable = (Variable)ToolBox.random(model.variables());
        if (!(variable instanceof Swapable)) return null;
        Variable anotherVariable = (Variable)ToolBox.random(model.variables());
        if (variable.equals(anotherVariable)) return null;
        return ((Swapable)variable).findSwap(anotherVariable);
    }
}
