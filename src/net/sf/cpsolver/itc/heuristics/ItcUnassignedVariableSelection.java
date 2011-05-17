package net.sf.cpsolver.itc.heuristics;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Unassigned variable selection. The "biggest" variable (using {@link Variable#compareTo(Object)})
 * unassigned variable is selected. One is selected randomly if there are more than one of
 * such variables.
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
public class ItcUnassignedVariableSelection<V extends Variable<V, T>, T extends Value<V, T>> implements VariableSelection<V,T> {
    /** Constructor */
    public ItcUnassignedVariableSelection(DataProperties properties) {
    }
    
    /** Initialization */
    public void init(Solver<V,T> solver) {}
    
    /** Variable selection */
    public V selectVariable(Solution<V,T> solution) {
        Model<V,T> model = solution.getModel();
        if (model.nrUnassignedVariables()==0) return null;
        V variable = null;
        for (V v: model.unassignedVariables()) {
            if (variable==null || v.compareTo(variable)<0) variable = v;
        }
        return variable;
    }
}
