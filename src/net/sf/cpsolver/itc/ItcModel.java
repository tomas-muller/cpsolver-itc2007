package net.sf.cpsolver.itc;

import java.io.File;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ModelWithContext;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Few model extension, common for all three tracks.
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
public abstract class ItcModel<V extends Variable<V, T>, T extends Value<V, T>> extends ModelWithContext<V, T, AssignmentConstraintContext<V, T>> {
    private DataProperties iProperties;
    /** Set problem properties */
    public void setProperties(DataProperties properties) { iProperties = properties; }
    /** Get problem properties */
    public DataProperties getProperties() { return iProperties; } 
    /** Load input file */
    public abstract boolean load(File file) throws Exception;
    /** Save output solution */
    public abstract boolean save(Assignment<V, T> assignment, File file) throws Exception;
    /** Print CSV header (comma separated value text file) */
    public String csvHeader() { return ""; }
    /** Print CSV line (problem specific solution properties) */
    public String csvLine(Assignment<V, T> assignment) { return ""; }
    /** Return true if csv line should be printed (e.g., whether found solution is complete) */
    public boolean cvsPrint(Assignment<V, T> assignment) {
        return assignment.nrAssignedVariables() == variables().size();
    }
    /** Make feasible, executed by {@link ItcTest} after the solver is stopped */
    public void makeFeasible() {}
    
    public abstract Assignment<V, T> createAssignment(int index, Assignment<V, T> assignment);
}
