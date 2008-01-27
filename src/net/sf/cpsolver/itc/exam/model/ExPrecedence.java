package net.sf.cpsolver.itc.exam.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Precedence binary constraint.
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
public class ExPrecedence extends BinaryConstraint {
    private boolean iIsHard = false;
    
    /** Constructor
     * @param first first exam
     * @param second second exam
     * @param hard true if the constraint is hard (default, can be changed by Exam.AllowBinaryViolations parameter)
     */
    public ExPrecedence(Variable first, Variable second, boolean hard) {
        super();
        iAssignedVariables=null;
        iIsHard = hard;
        addVariable(first);
        addVariable(second);
    }

    /**
     * Compute conflicts: two exams are in conflict if the second one is not placed after the first one
     */
    public void computeConflicts(Value value, Set conflicts) {
        if (!iIsHard) return;
        if (inConflict(value))
            conflicts.add(another(value.variable()).getAssignment());
    }
    
    /**
     * Check for conflicts: two exams are in conflict if the second one is not placed after the first one
     */
    public boolean inConflict(Value value) {
        if (!iIsHard) return false;
        ExPlacement first, second;
        if (value.variable().equals(first())) {
            first = (ExPlacement)value;
            second = (ExPlacement)second().getAssignment();
        } else {
            first = (ExPlacement)first().getAssignment();
            second = (ExPlacement)value;
        }
        return (first!=null && second!=null && first.getPeriodIndex()>=second.getPeriodIndex());
    }
    
    /**
     * Two exams are in conflict if the second one is not placed after the first one
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExPlacement first, second;
        if (value1.variable().equals(first())) {
            first = (ExPlacement)value1;
            second = (ExPlacement)value2;
        } else {
            first = (ExPlacement)value2;
            second = (ExPlacement)value1;
        }
        return first.getPeriodIndex()<second.getPeriodIndex();
    }
    
    /**
     * Compare two constraints for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExPrecedence)) return false;
        ExPrecedence c = (ExPrecedence)o;
        return first().equals(c.first()) && second().equals(c.second());
    }
    
    /**
     * True if the constraint is hard (cannot be violated)
     */
    public boolean isHard() {
        return iIsHard;
    }
    
    public void assigned(long iteration, Value value) {
        if (!isHard()) return;
        if (value.variable().equals(first())) {
            ExPlacement first = (ExPlacement)value;
            ExPlacement second = (ExPlacement)second().getAssignment();
            if (first!=null && second!=null && first.getPeriodIndex()>=second.getPeriodIndex()) {
                HashSet conf = new HashSet(); conf.add(second);
                second.variable().unassign(iteration);
                if (iConstraintListeners!=null)
                    for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                        ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, conf);
            }
        } else {
            ExPlacement first = (ExPlacement)first().getAssignment();
            ExPlacement second = (ExPlacement)value;
            if (first!=null && second!=null && first.getPeriodIndex()>=second.getPeriodIndex()) {
                HashSet conf = new HashSet(); conf.add(first);
                first.variable().unassign(iteration);
                if (iConstraintListeners!=null)
                    for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                        ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, conf);
            }
        }
    }
    
    public void unassigned(long iteration, Value value) {}
    
}
