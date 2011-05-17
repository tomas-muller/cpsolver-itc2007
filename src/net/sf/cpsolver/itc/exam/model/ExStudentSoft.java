package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;

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
    private Set<ExExam>[] iExams = null;
    private int[] iNrExams = null;
    
    /**
     * Constructor
     * @param id student unique identifier
     */
    public ExStudentSoft(int id) {
        super(id);
    }
    
    /**
     * Initialization
     */
    @SuppressWarnings("unchecked")
	public void init() {
        super.init();
        ExModel m = (ExModel)getModel();
        iExams = new HashSet[m.getNrPeriods()];
        iNrExams = new int[m.getNrPeriods()];
        for (int i=0;i<m.getNrPeriods();i++) {
            iExams[i]=new HashSet<ExExam>();
            iNrExams[i]=0;
        }
    }
    
    /**
     * No conflicts
     */
    public void computeConflicts(ExPlacement value, Set<ExPlacement> conflicts) {
    }
    
    /**
     * No conflicts
     */
    public boolean inConflict(ExPlacement value) {
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
    public void afterAssigned(long iteration, ExPlacement p) {
        if (iExams[p.getPeriodIndex()].add(p.variable()))
            iNrExams[p.getPeriodIndex()]++;
    }
        
    /**
     * Update assignment table
     */
    public void afterUnassigned(long iteration, ExPlacement p) {
        if (iExams[p.getPeriodIndex()].remove(p.variable()))
            iNrExams[p.getPeriodIndex()]--;
    }
    
    /** List of exams that this student has at the given period */
    public Set<ExExam> getExams(int period) {
        return iExams[period];
    }
    /** True, if this student has one or more exams at the given period */
    public boolean hasExam(int period) {
        return iNrExams[period]>0;
    }
    /** Number of exams that this student has at the given period */
    public int nrExams(int period) {
        return iNrExams[period];
    }
    /** True, if this student has one or more exams at the given period (given exam excluded)*/
    public boolean hasExam(int period, ExExam exclude) {
        if (exclude==null) return hasExam(period);
        return (iExams[period].size()>(iExams[period].contains(exclude)?1:0));
    }
    /** Number of exams that this student has at the given period (given exam excluded)*/
    public int nrExams(int period, ExExam exclude) {
        if (exclude==null) return nrExams(period);
        return iExams[period].size()-(iExams[period].contains(exclude)?1:0);
    }
    
    public void assigned(long iteration, ExPlacement value) {
    }
        
    public void unassigned(long iteration, ExPlacement value) {
    }    
    
}
