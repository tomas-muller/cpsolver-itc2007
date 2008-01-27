package net.sf.cpsolver.itc.heuristics;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Model;
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
public class ItcUnassignedVariableSelection implements VariableSelection {
    private static Logger sLog = Logger.getLogger(ItcUnassignedVariableSelection.class);
    private Vector iVariables = null;
    private long iIteration = -1;
    
    /** Constructor */
    public ItcUnassignedVariableSelection(DataProperties properties) {
    }
    
    /** Initialization */
    public void init(Solver solver) {}
    
    /** Variable selection */
    public Variable selectVariable(Solution solution) {
        Model model = solution.getModel();
        if (model.nrUnassignedVariables()==0) return null;
        Variable variable = null;
        for (Enumeration e=model.unassignedVariables().elements();e.hasMoreElements();) {
            Variable v = (Variable)e.nextElement();
            if (variable==null || v.compareTo(variable)<0) variable = v;
        }
        return variable;
    }
}
