package net.sf.cpsolver.itc.tim.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentAbstract;
import org.cpsolver.ifs.assignment.context.DefaultParallelAssignmentContextHolder;
import org.cpsolver.ifs.model.Model;

/**
 * Post Enrollment based Course Timetabling (tim) assignment model.
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2014 Tomas Muller<br>
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
public class TimAssignment extends AssignmentAbstract<TimEvent, TimLocation> {
	private Model<TimEvent, TimLocation> iModel;
	private int iIndex;
	private int iNrAssigned = 0;
	
	public TimAssignment(Model<TimEvent, TimLocation> model, int index, Assignment<TimEvent, TimLocation> assignment) {
		super(new DefaultParallelAssignmentContextHolder<TimEvent, TimLocation>(index));
		iIndex = index;
		iModel = model;
		if (assignment != null)
            for (TimLocation value: assignment.assignedValues())
                setValueInternal(0, value.variable(), value);
	}
	
	@Override
    protected TimLocation assign(long iteration, TimEvent variable, TimLocation value) {
        Model<TimEvent, TimLocation> model = variable.getModel();
        
        // unassign old value, if assigned
        TimLocation old = getValueInternal(variable);
        if (old != null) {
            model.beforeUnassigned(this, iteration, old);
            setValueInternal(iteration, variable, null);
        	
            if (old.room() != null) old.room().unassigned(this, iteration, old);
            for (TimStudent student: variable.students())
            	student.unassigned(this, iteration, old);

            model.afterUnassigned(this, iteration, old);
        }
        
        // assign new value, if provided
        if (value != null) {
            model.beforeAssigned(this, iteration, value);
            setValueInternal(iteration, variable, value);
            
            if (value.room() != null) value.room().assigned(this, iteration, value);
            for (TimStudent student: variable.students())
                student.assigned(this, iteration, value);
            if (!((TimModel)model).isAllowPrecedenceViolations())
            	for (TimPrecedence precedence: variable.getPrecedences())
            		precedence.assigned(this, iteration, value);

            model.afterAssigned(this, iteration, value);
        }
        
        // return old value
        return old;
    }

	@Override
	public long getIteration(TimEvent variable) {
		return 0;
	}

	@Override
	public Collection<TimEvent> assignedVariables() {
		List<TimEvent> assigned = new ArrayList<TimEvent>(iModel.variables().size());
		for (TimEvent variable: iModel.variables()) {
			if (getValueInternal(variable) != null)
				assigned.add(variable);
		}
		return assigned;
	}

    protected TimLocation[] getAssignments(TimEvent variable) {
        synchronized (variable) {
            TimLocation[] assignments = (TimLocation[])variable.getExtra();
            if (assignments == null) {
                assignments = (TimLocation[])new TimLocation[Math.max(10, 1 + iIndex)];
                variable.setExtra(assignments);
            } else if (assignments.length <= iIndex) {
                assignments = Arrays.copyOf(assignments, 10 + iIndex);
                variable.setExtra(assignments);
            }
            return assignments;
        }
    }
    
	@Override
    @SuppressWarnings("deprecation")
    protected TimLocation getValueInternal(TimEvent variable) {
    	if (iIndex == 1)
    		return variable.getAssignment();
        return getAssignments(variable)[iIndex];
    }
    
	@Override
    @SuppressWarnings("deprecation")
    protected void setValueInternal(long iteration, TimEvent variable, TimLocation value) {
    	if (iIndex == 1)
    		variable.setAssignment(value);
    	else
    		getAssignments(variable)[iIndex] = value;
        if (value == null)
        	iNrAssigned --;
        else
        	iNrAssigned ++;
    }

    @Override
    public int getIndex() {
        return iIndex;
    }
    
    @Override
    public int nrAssignedVariables() {
        return iNrAssigned;
    }
}
