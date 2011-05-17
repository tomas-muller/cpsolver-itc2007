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
public class ItcSwapMove<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V,T> {
    private double iValueWeight = 1;
    /** Constructor */
    public ItcSwapMove(DataProperties properties) {
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }
    /** Initialization */
    public void init(Solver<V,T> solver) {}
    /** Neighbour selection */
    @SuppressWarnings("unchecked")
	public Neighbour<V,T> selectNeighbour(Solution<V,T> solution) {
        Model<V,T> model = solution.getModel();
        V variable = ToolBox.random(model.variables());
        if (!(variable instanceof Swapable)) return null;
        V anotherVariable = ToolBox.random(model.variables());
        if (variable.equals(anotherVariable)) return null;
        return ((Swapable<V,T>)variable).findSwap(anotherVariable);
    }
}
