package net.sf.cpsolver.itc.tim.model;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.BinaryConstraint;
import org.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a precedence constraint. The first given event has
 * to be placed in a time slot that is preceding a time slot
 * of the second given event. 
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
public class TimPrecedence extends BinaryConstraint<TimEvent, TimLocation> {
    private boolean iIsHard = false;
    
    /** Constructor
     * @param first first event
     * @param second second event
     */
    public TimPrecedence(TimEvent first, TimEvent second) {
        super();
        addVariable(first);
        addVariable(second);
    }

    /**
     * Compute conflicts: check start time of the other event versus the start time of the given event
     */
    public void computeConflicts(Assignment<TimEvent, TimLocation> assignment, TimLocation value, Set<TimLocation> conflicts) {
        if (!iIsHard) return;
        if (inConflict(assignment, value))
            conflicts.add(assignment.getValue(another(value.variable())));
    }
    
    /**
     * Check for conflicts: check start time of the other event versus the start time of the given event
     */
    public boolean inConflict(Assignment<TimEvent, TimLocation> assignment, TimLocation value) {
        if (!iIsHard) return false;
        TimLocation first = (first().equals(value.variable()) ? value : assignment.getValue(first()));
        TimLocation second = (second().equals(value.variable()) ? value : assignment.getValue(second()));
        return (first!=null && second!=null && first.time()>=second.time());
    }
    
    /**
     * First event has to be placed in a smaller time slot than the second event.
     */
    public boolean isConsistent(TimLocation value1, TimLocation value2) {
        TimLocation first = (first().equals(value1.variable()) ? value1 : value2);
        TimLocation second = (first().equals(value1.variable()) ? value2 : value1);
        return (first.time()<second.time());
    }
    
    /**
     * Check for conflicts: check start time of the other event versus the start time of the given event
     */
    public boolean isConsistent(TimLocation value, int time) {
        if (first().equals(value.variable())) {
            return value.time() < time;
        } else {
            return time < value.time();
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
    public boolean isSatisfied(Assignment<TimEvent, TimLocation> assignment) {
        TimLocation first = (TimLocation)assignment.getValue(first());
        TimLocation second = (TimLocation)assignment.getValue(second());
        return (first==null || second==null || first.time()<second.time());
    }
    
    public void assigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation value) {
        if (isHard() && inConflict(assignment, value)) {
            if (iConstraintListeners!=null) {
                Set<TimLocation> confs = new HashSet<TimLocation>(); confs.add(assignment.getValue(another(value.variable())));
                assignment.unassign(iteration, another(value.variable()));
                for (ConstraintListener<TimEvent, TimLocation> listener: iConstraintListeners)
                    listener.constraintAfterAssigned(assignment, iteration, this, value, confs);
            } else {
            	assignment.unassign(iteration, another(value.variable()));
            }
        }
    }
        
    public void unassigned(long iteration, TimLocation value) {
    }
    
}
