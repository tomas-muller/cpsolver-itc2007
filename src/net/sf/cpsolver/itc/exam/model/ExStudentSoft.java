package net.sf.cpsolver.itc.exam.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.CanInheritContext;

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
public class ExStudentSoft extends ExStudent implements CanInheritContext<ExExam, ExPlacement, ExStudentSoft.Context> {
    
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
    	
    	@SuppressWarnings("unchecked")
		private Context(ExModel m, Assignment<ExExam, ExPlacement> assignment, Context parent) {
    		synchronized (parent.iExams) {
        		iNrExams = Arrays.copyOf(parent.iNrExams, m.getNrPeriods());
                iExams = new HashSet[m.getNrPeriods()];
                for (int i=0;i<m.getNrPeriods();i++) {
                    iExams[i]=new HashSet<ExExam>(parent.iExams[i]);
                }
			}
    	}

		@Override
		public void assigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
			synchronized (iExams) {
		        if (iExams[p.getPeriodIndex()].add(p.variable()))
		            iNrExams[p.getPeriodIndex()]++;
			}
		}

		@Override
		public void unassigned(Assignment<ExExam, ExPlacement> assignment, ExPlacement p) {
			synchronized (iExams) {
				if (iExams[p.getPeriodIndex()].remove(p.variable()))
					iNrExams[p.getPeriodIndex()]--;
			}
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

		@Override
		public boolean hasExam(int period) {
			return iNrExams[period] > 0;
		}

		@Override
		public boolean hasExam(int period, ExExam exclude) {
			if (exclude == null) return iNrExams[period] > 0;
	        return (iNrExams[period] > (iExams[period].contains(exclude) ? 1 : 0));
		}

		@Override
		public int nrExams(int period, ExExam exclude) {
			if (exclude == null) return iNrExams[period];
	        return (iExams[period].contains(exclude) ? iNrExams[period] - 1 : iNrExams[period]);
		}
    }

	@Override
	public Context createAssignmentContext(Assignment<ExExam, ExPlacement> assignment) {
		return new Context((ExModel)getModel(), assignment);
	}

	@Override
	public Context inheritAssignmentContext(Assignment<ExExam, ExPlacement> assignment, Context parentContext) {
		return new Context((ExModel)getModel(), assignment, parentContext);
	}

}
