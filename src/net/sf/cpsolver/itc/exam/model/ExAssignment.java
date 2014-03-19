package net.sf.cpsolver.itc.exam.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentAbstract;
import org.cpsolver.ifs.assignment.context.DefaultParallelAssignmentContextHolder;
import org.cpsolver.ifs.model.BinaryConstraint;
import org.cpsolver.ifs.model.Model;

/**
 * Examination Timetabling (exam) assignment model.
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
public class ExAssignment extends AssignmentAbstract<ExExam, ExPlacement> {
	private ExModel iModel;
	private int iIndex;
	private int iNrAssigned = 0;
	
	public ExAssignment(ExModel model, int index, Assignment<ExExam, ExPlacement> assignment) {
		super(new DefaultParallelAssignmentContextHolder<ExExam, ExPlacement>(index));
		iIndex = index;
		iModel = model;
		if (assignment != null)
            for (ExPlacement value: assignment.assignedValues())
                setValueInternal(0, value.variable(), value);
	}
	
	@Override
    protected ExPlacement assign(long iteration, ExExam variable, ExPlacement value) {
        Model<ExExam, ExPlacement> model = variable.getModel();
        
        // unassign old value, if assigned
        ExPlacement old = getValueInternal(variable);
        if (old != null) {
            model.beforeUnassigned(this, iteration, old);
            setValueInternal(iteration, variable, null);
            
            old.getRoom().unassigned(this, iteration, old);
            for (ExStudent student: variable.getStudents())
                student.unassigned(this, iteration, old);
            if (!iModel.areBinaryViolationsAllowed()) {
                for (BinaryConstraint<ExExam, ExPlacement> bc: variable.binaryConstraints())
                    bc.unassigned(this, iteration, old);
            }

            model.afterUnassigned(this, iteration, old);
        }
        
        // assign new value, if provided
        if (value != null) {
            model.beforeAssigned(this, iteration, value);
            setValueInternal(iteration, variable, value);
            
            value.getRoom().assigned(this, iteration, value);
            for (ExStudent student: variable.getStudents())
                student.assigned(this, iteration, value);
            if (!iModel.areBinaryViolationsAllowed()) {
                for (BinaryConstraint<ExExam, ExPlacement> bc: variable.binaryConstraints())
                    bc.assigned(this, iteration, value);
            }

            model.afterAssigned(this, iteration, value);
        }
        
        // return old value
        return old;
    }

	@Override
	public long getIteration(ExExam variable) {
		return 0;
	}

	@Override
	public Collection<ExExam> assignedVariables() {
		List<ExExam> assigned = new ArrayList<ExExam>(iModel.variables().size());
		for (ExExam variable: iModel.variables()) {
			if (getValueInternal(variable) != null)
				assigned.add(variable);
		}
		return assigned;
	}

    protected ExPlacement[] getAssignments(ExExam variable) {
        synchronized (variable) {
            ExPlacement[] assignments = (ExPlacement[])variable.getExtra();
            if (assignments == null) {
                assignments = (ExPlacement[])new ExPlacement[Math.max(10, 1 + iIndex)];
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
    protected ExPlacement getValueInternal(ExExam variable) {
    	if (iIndex == 1)
    		return variable.getAssignment();
        return getAssignments(variable)[iIndex];
    }
    
	@Override
    @SuppressWarnings("deprecation")
    protected void setValueInternal(long iteration, ExExam variable, ExPlacement value) {
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
