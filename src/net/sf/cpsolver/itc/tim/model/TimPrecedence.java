package net.sf.cpsolver.itc.tim.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Representation of a precedence constraint. The first given event has
 * to be placed in a time slot that is preceding a time slot
 * of the second given event. 
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
public class TimPrecedence extends BinaryConstraint {
    private boolean iIsHard = false;
    
    /** Constructor
     * @param first first event
     * @param second second event
     */
    public TimPrecedence(Variable first, Variable second) {
        super();
        iAssignedVariables=null;
        addVariable(first);
        addVariable(second);
    }

    /**
     * Compute conflicts: check start time of the other event versus the start time of the given event
     */
    public void computeConflicts(Value value, Set conflicts) {
        if (!iIsHard) return;
        if (inConflict(value))
            conflicts.add(another(value.variable()).getAssignment());
    }
    
    /**
     * Check for conflicts: check start time of the other event versus the start time of the given event
     */
    public boolean inConflict(Value value) {
        if (!iIsHard) return false;
        TimLocation first = (TimLocation)(first().equals(value.variable())?value:first().getAssignment());
        TimLocation second = (TimLocation)(second().equals(value.variable())?value:second().getAssignment());
        return (first!=null && second!=null && first.time()>=second.time());
    }
    
    /**
     * First event has to be placed in a smaller time slot than the second event.
     */
    public boolean isConsistent(Value value1, Value value2) {
        TimLocation first = (TimLocation)(first().equals(value1.variable())?value1:value2);
        TimLocation second = (TimLocation)(first().equals(value1.variable())?value2:value1);
        return (first.time()<second.time());
    }
    
    /**
     * Check for conflicts: check start time of the other event versus the start time of the given event
     */
    public boolean isConsistent(Value value, int time) {
        if (first().equals(value.variable())) {
            return ((TimLocation)value).time()<time;
        } else {
            return time<((TimLocation)value).time();
        }
    }
    
    /** True if violations of this constraint are not allowed */
    public boolean isHardPrecedence() {
        return iIsHard;
    }
    
    /** Set whether violations of this constraint are allowed */
    public void setHardPrecedence(boolean hard) {
        iIsHard = hard;
    }
    
    /**
     * True if the first event is preceding the second event.
     */
    public boolean isSatisfied() {
        TimLocation first = (TimLocation)first().getAssignment();
        TimLocation second = (TimLocation)second().getAssignment();
        return (first==null || second==null || first.time()<second.time());
    }
    
    public void assigned(long iteration, Value value) {
        if (isHard() && inConflict(value)) {
            HashSet confs = new HashSet(); confs.add(another(value.variable()).getAssignment());
            another(value.variable()).unassign(iteration);
            if (iConstraintListeners!=null)
                for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                    ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, confs);
        }
    }
        
    public void unassigned(long iteration, Value value) {
    }
    
}
