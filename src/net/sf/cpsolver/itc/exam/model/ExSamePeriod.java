package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;

/**
 * Same period binary constraint.
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
public class ExSamePeriod extends BinaryConstraint<ExExam, ExPlacement> {
    private boolean iIsHard = false;
    
    /** Constructor
     * @param first first exam
     * @param second second exam
     * @param hard true if the constraint is hard (default, can be changed by Exam.AllowBinaryViolations parameter)
     */
    public ExSamePeriod(ExExam first, ExExam second, boolean hard) {
        super();
        iAssignedVariables=null;
        iIsHard = hard;
        addVariable(first);
        addVariable(second);
    }

    /**
     * Compute conflicts: two exams are in conflict if placed in different periods
     */
    public void computeConflicts(ExPlacement value, Set<ExPlacement> conflicts) {
        if (!iIsHard) return;
        if (inConflict(value))
            conflicts.add(another(value.variable()).getAssignment());
    }
    
    /**
     * Check for conflicts: two exams are in conflict if placed in different periods
     */
    public boolean inConflict(ExPlacement value) {
        if (!iIsHard) return false;
        ExPlacement first, second;
        if (value.variable().equals(first())) {
            first = value;
            second = second().getAssignment();
        } else {
            first = first().getAssignment();
            second = value;
        }
        return (first!=null && second!=null && first.getPeriodIndex()!=second.getPeriodIndex());
    }
    
    /**
     * Two exams are in conflict if placed in different periods
     */
    public boolean isConsistent(ExPlacement value1, ExPlacement value2) {
        ExPlacement first, second;
        if (value1.variable().equals(first())) {
            first = value1;
            second = value2;
        } else {
            first = value2;
            second = value1;
        }
        return first.getPeriodIndex()==second.getPeriodIndex();
    }
    
    /**
     * Compare two constraints for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExSamePeriod)) return false;
        ExSamePeriod c = (ExSamePeriod)o;
        if (first().equals(c.first()) && second().equals(c.second())) return true;
        if (first().equals(c.second()) && second().equals(c.first())) return true;
        return false;
    }
    
    /**
     * True if the constraint is hard (cannot be violated)
     */
    public boolean isHard() {
        return iIsHard;
    }
    
    public void assigned(long iteration, ExPlacement value) {
        if (!isHard()) return;
        if (value.variable().equals(first())) {
            ExPlacement first = (ExPlacement)value;
            ExPlacement second = (ExPlacement)second().getAssignment();
            if (first!=null && second!=null && first.getPeriodIndex()!=second.getPeriodIndex()) {
                Set<ExPlacement> conf = new HashSet<ExPlacement>(); conf.add(second);
                second.variable().unassign(iteration);
                if (iConstraintListeners!=null)
                	for (ConstraintListener<ExPlacement> listener: iConstraintListeners)
                        listener.constraintAfterAssigned(iteration, this, value, conf);
            }
        } else {
            ExPlacement first = (ExPlacement)first().getAssignment();
            ExPlacement second = (ExPlacement)value;
            if (first!=null && second!=null && first.getPeriodIndex()!=second.getPeriodIndex()) {
                Set<ExPlacement> conf = new HashSet<ExPlacement>(); conf.add(first);
                first.variable().unassign(iteration);
                if (iConstraintListeners!=null)
                	for (ConstraintListener<ExPlacement> listener: iConstraintListeners)
                        listener.constraintAfterAssigned(iteration, this, value, conf);
            }
        }
    }
    
    public void unassigned(long iteration, ExPlacement value) {}
}
