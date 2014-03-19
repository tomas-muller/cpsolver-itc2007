package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;

/**
 * Representation of a student. Direct student conflicts are allowed
 * (Problem propery Exam.AllowDirectConflict is true).
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
public class ExStudentSoft extends ExStudent {
    
    /**
     * Constructor
     * @param id student unique identifier
     */
    public ExStudentSoft(int id) {
        super(id);
    }
    
    /**
     * No conflicts
     */
    public void computeConflicts(Assignment<ExExam, ExPlacement> assignment, ExPlacement value, Set<ExPlacement> conflicts) {
    }
    
    /**
     * No conflicts
     */
    public boolean inConflict(Assignment<ExExam, ExPlacement> assignment, ExPlacement value) {
        return false;
    }
    
    /**
     * This constraint is not hard
     */
    public boolean isHard() {
        return false;
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
    
    /** List of exams that this student has at the given period */
    public Set<ExExam> getExams(Assignment<ExExam, ExPlacement> assignment, int period) {
        return getContext(assignment).getExams(period);
    }
    /** True, if this student has one or more exams at the given period */
    public boolean hasExam(Assignment<ExExam, ExPlacement> assignment, int period) {
        return getContext(assignment).nrExams(period) > 0;
    }
    /** Number of exams that this student has at the given period */
    public int nrExams(Assignment<ExExam, ExPlacement> assignment, int period) {
        return getContext(assignment).nrExams(period);
    }
    /** True, if this student has one or more exams at the given period (given exam excluded)*/
    public boolean hasExam(Assignment<ExExam, ExPlacement> assignment, int period, ExExam exclude) {
        if (exclude == null) return hasExam(assignment, period);
        ExStudent.Context context = getContext(assignment);
        return (context.nrExams(period) > (context.getExams(period).contains(exclude) ? 1 : 0));
    }
    /** Number of exams that this student has at the given period (given exam excluded)*/
    public int nrExams(Assignment<ExExam, ExPlacement> assignment, int period, ExExam exclude) {
        if (exclude == null) return nrExams(assignment, period);
        ExStudent.Context context = getContext(assignment);
        return context.nrExams(period) - (context.getExams(period).contains(exclude) ? 1 : 0);
    }
    
    public void assigned(long iteration, ExPlacement value) {
    }
        
    public void unassigned(long iteration, ExPlacement value) {
    }
    
    public class Context implements ExStudent.Context {
        private Set<ExExam>[] iExams = null;
        private int[] iNrExams = null;

    	@SuppressWarnings("unchecked")
		private Context(ExModel m, Assignment<ExExam, ExPlacement> assignment) {
            iExams = new HashSet[m.getNrPeriods()];
            iNrExams = new int[m.getNrPeriods()];
            for (int i=0;i<m.getNrPeriods();i++) {
                iExams[i]=new HashSet<ExExam>();
                iNrExams[i]=0;
            }
            if (assignment.nrAssignedVariables() > 0)
                for (ExExam exam: variables()) {
                	ExPlacement p = assignment.getValue(exam);
                	if (p != null)
                		assigned(assignment, p);
                }
    	}

		@Override
		public void assigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
	        if (iExams[p.getPeriodIndex()].add(p.variable()))
	            iNrExams[p.getPeriodIndex()]++;
		}

		@Override
		public void unassigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
	        if (iExams[p.getPeriodIndex()].remove(p.variable()))
	            iNrExams[p.getPeriodIndex()]--;
		}
		
		@Override
		public Set<ExExam> getExams(int period) {
			return iExams[period];
		}
		
		@Override
		public int nrExams(int period) {
			return iNrExams[period];
		}

		@Override
		public ExPlacement getPlacement(int period) {
			return null;
		}
    }

	@Override
	public Context createAssignmentContext(Assignment<ExExam, ExPlacement> assignment) {
		return new Context((ExModel)getModel(), assignment);
	}

}
