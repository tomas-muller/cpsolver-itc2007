package net.sf.cpsolver.itc.heuristics;

import java.lang.reflect.Array;
import java.util.Collection;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

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
        Assignment<V, T> assignment = solution.getAssignment();
        if (assignment.nrAssignedVariables() == model.variables().size()) return null;
        int index = solution.getAssignment().getIndex();
        Collection<V> unassigned = assignment.unassignedVariables(model);
        if (solution.getAssignment() instanceof InheritedAssignment) {
            if (index < 0 || unassigned.size() < index) {
            	return ToolBox.random(unassigned);
            } else if (index <= 1) {
                V variable = null;
                for (V v: unassigned) {
                    if (variable==null || v.compareTo(variable)<0) variable = v;
                }
                return variable;
            } else {
            	@SuppressWarnings("unchecked")
    			V[] adepts = (V[])Array.newInstance(Variable.class, index);
            	for (V v: unassigned) {
            		for (int i = 0; i < adepts.length; i++) {
            			V x = adepts[i];
            			if (x == null) {
            				adepts[i] = v; break;
            			} else if (v.compareTo(x) < 0) {
            				adepts[i] = v; v = x;
            			}
            		}
            	}
            	return adepts[index - 1];
            }        	
        } else {
            V variable = null;
            for (V v: unassigned) {
                if (variable==null || v.compareTo(variable)<0) variable = v;
            }
            return variable;
        }
    }
}
