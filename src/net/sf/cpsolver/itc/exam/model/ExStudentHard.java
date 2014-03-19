package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a student. Direct student conflicts are not allowed
 * (Problem propery Exam.AllowDirectConflict is false).
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
public class ExStudentHard extends ExStudent {
    
    /**
     * Constructor
     * @param id unique identifier
     */
    public ExStudentHard(int id) {
        super(id);
    }
    
    /**
     * Compute conflicts: i.e., exams that are placed at the same period as the given one
     */
    public void computeConflicts(Assignment<ExExam, ExPlacement> assignment, ExPlacement p, Set<ExPlacement> conflicts) {
    	ExPlacement conflict = getContext(assignment).getPlacement(p.getPeriodIndex());
        if (conflict != null && !conflict.variable().equals(p.variable())) 
            conflicts.add(conflict);
    }
    
    /**
     * Check for conflicts: i.e., exams that are placed at the same period as the given one
     */
    public boolean inConflict(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
    	ExPlacement conflict = getContext(assignment).getPlacement(p.getPeriodIndex());
        return conflict != null && !conflict.variable().equals(p.variable());
    }
    
    public void assigned(Assignment<ExExam, ExPlacement> assignment, long iteration, ExPlacement p) {
    	ExPlacement conflict = getContext(assignment).getPlacement(p.getPeriodIndex());
        if (conflict != null) {
            HashSet<ExPlacement> confs = new HashSet<ExPlacement>();
            confs.add(conflict);
            assignment.unassign(iteration, conflict.variable());
            if (iConstraintListeners!=null)
                for (ConstraintListener<ExExam, ExPlacement> listener: iConstraintListeners)
                    listener.constraintAfterAssigned(assignment, iteration, this, p, confs);
        }
    }
        
    public void unassigned(long iteration, ExPlacement value) {
    }    
    
    /**
     * Update assignment table
     */
    public void afterAssigned(Assignment<ExExam, ExPlacement> assignment, long iteration, ExPlacement p) {
    	getContext(assignment).assigned(assignment, p);
    }
        
    /**
     * Update assignment table
     */
    public void afterUnassigned(Assignment<ExExam, ExPlacement> assignment, long iteration, ExPlacement p) {
    	getContext(assignment).unassigned(assignment, p);
    }
    
    /**
     * Return an exam that is assigned to the given period (if any)
     */
    public ExPlacement getPlacement(Assignment<ExExam, ExPlacement> assignment, int period) {
        return getContext(assignment).getPlacement(period);
    }
    /**
     * Return an exam that is assigned to the given period (if any)
     */
    public ExPlacement getPlacement(Assignment<ExExam, ExPlacement> assignment, ExPeriod period) {
        return getPlacement(assignment, period.getIndex());
    }
    /**
     * Return exams that are assigned to the given period
     */
    public Set<ExExam> getExams(Assignment<ExExam, ExPlacement> assignment, int period) {
        Set<ExExam> set = new HashSet<ExExam>();
        ExPlacement p = getPlacement(assignment, period);
        if (p != null) set.add(p.variable());
        return set;
    }
    
    /** True, if this student has one or more exams at the given period */
    public boolean hasExam(Assignment<ExExam, ExPlacement> assignment, int period) {
    	return getPlacement(assignment, period) != null;
    }
    /** True, if this student has one or more exams at the given period (given exam excluded) */
    public boolean hasExam(Assignment<ExExam, ExPlacement> assignment, int period, ExExam exclude) {
    	ExPlacement p = getPlacement(assignment, period);
    	return p != null && !p.variable().equals(exclude);
    }
    /** Number of exams that this student has at the given period */
    public int nrExams(Assignment<ExExam, ExPlacement> assignment, int period) {
        return (hasExam(assignment, period)?1:0);
    }
    /** Number of exams that this student has at the given period (given exam excluded)*/
    public int nrExams(Assignment<ExExam, ExPlacement> assignment, int period, ExExam exclude) {
        return (hasExam(assignment, period,exclude)?1:0);
    }
    
    public class Context implements ExStudent.Context {
        private ExPlacement[] iTable = null;
        
        private Context(ExModel m, Assignment<ExExam, ExPlacement> assignment) {
            iTable = new ExPlacement[m.getNrPeriods()];
            for (int i=0;i<m.getNrPeriods();i++)
                iTable[i]=null;
            if (assignment.nrAssignedVariables() > 0)
                for (ExExam exam: variables()) {
                	ExPlacement p = assignment.getValue(exam);
                	if (p != null)
                		assigned(assignment, p);
                }
        }

		@Override
		public void assigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
	        iTable[p.getPeriodIndex()] = p;
		}

		@Override
		public void unassigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
	        iTable[p.getPeriodIndex()] = null;
		}
		
		@Override
		public ExPlacement getPlacement(int period) {
			return iTable[period];
		}

		@Override
		public Set<ExExam> getExams(int period) {
	        Set<ExExam> set = new HashSet<ExExam>();
	        if (iTable[period] != null) set.add(iTable[period].variable());
	        return set;
		}

		@Override
		public int nrExams(int period) {
			return iTable[period] != null ? 1 : 0;
		}
    }

	@Override
	public Context createAssignmentContext(Assignment<ExExam, ExPlacement> assignment) {
		return new Context((ExModel)getModel(), assignment);
	}
}
