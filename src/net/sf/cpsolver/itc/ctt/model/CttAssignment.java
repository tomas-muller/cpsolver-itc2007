package net.sf.cpsolver.itc.ctt.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentAbstract;
import org.cpsolver.ifs.assignment.context.AssignmentContextHolder;
import org.cpsolver.ifs.assignment.context.DefaultParallelAssignmentContextHolder;
import org.cpsolver.ifs.model.Model;

/**
 * Curriculum based Course Timetabling (CTT) assignment model.
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
public class CttAssignment extends AssignmentAbstract<CttLecture, CttPlacement> {
	protected CttModel iModel;
	protected int iIndex;
	protected int iNrAssigned = 0;
	protected CttPlacement[] iAssignment = null;
	protected CttAssignment iParent = null;
	
	public CttAssignment(CttModel model, int index, Assignment<CttLecture, CttPlacement> assignment) {
		super(new DefaultParallelAssignmentContextHolder<CttLecture, CttPlacement>(index));
		iIndex = index;
		iModel = model;
		iParent = (CttAssignment) assignment;
		if (iParent == null) {
			iAssignment = new CttPlacement[model.variables().size()];
			iNrAssigned = 0;
		} else {
			iAssignment = Arrays.copyOf(((CttAssignment)iParent).toArray(), model.variables().size());
			iNrAssigned = iParent.nrAssignedVariables();
		}
	}
	
	protected CttAssignment(AssignmentContextHolder<CttLecture, CttPlacement> contextHolder) {
		super(contextHolder);
	}
	
	@Override
    protected CttPlacement assign(long iteration, CttLecture variable, CttPlacement value) {
        Model<CttLecture, CttPlacement> model = variable.getModel();
        
        // unassign old value, if assigned
        CttPlacement old = getValueInternal(variable);
        if (old != null) {
            model.beforeUnassigned(this, iteration, old);
            setValueInternal(iteration, variable, null);
        	
            old.getRoom().getConstraint().unassigned(this, iteration, old);
            for (CttCurricula curricula: variable.getCourse().getCurriculas())
                curricula.getConstraint().unassigned(this, iteration, old);
            variable.getCourse().getTeacher().getConstraint().unassigned(this, iteration, old);

            model.afterUnassigned(this, iteration, old);
        }
        
        // assign new value, if provided
        if (value != null) {
            model.beforeAssigned(this, iteration, value);
            setValueInternal(iteration, variable, value);
            
            value.getRoom().getConstraint().assigned(this, iteration, value);
            for (CttCurricula curricula: variable.getCourse().getCurriculas())
                curricula.getConstraint().assigned(this, iteration, value);
            variable.getCourse().getTeacher().getConstraint().assigned(this, iteration, value);

            model.afterAssigned(this, iteration, value);
        }
        
        // return old value
        return old;
    }

	@Override
	public long getIteration(CttLecture variable) {
		return 0;
	}

	@Override
	public Collection<CttLecture> assignedVariables() {
		List<CttLecture> assigned = new ArrayList<CttLecture>(iModel.variables().size());
		for (CttLecture variable: iModel.variables()) {
			if (getValueInternal(variable) != null)
				assigned.add(variable);
		}
		return assigned;
	}

	@Override
    protected CttPlacement getValueInternal(CttLecture variable) {
        // return (CttPlacement) variable.getAssignments()[iIndex];
		return iAssignment[variable.getIndex()];
    }
    
	@Override
    protected void setValueInternal(long iteration, CttLecture variable, CttPlacement value) {
    	//variable.getAssignments()[iIndex] = value;
    	iAssignment[variable.getIndex()] = value;
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
    
    public CttPlacement[] toArray() {
    	return iAssignment;
    }

	public Assignment<CttLecture, CttPlacement> getParentAssignment() {
		return iParent;
	}
}
