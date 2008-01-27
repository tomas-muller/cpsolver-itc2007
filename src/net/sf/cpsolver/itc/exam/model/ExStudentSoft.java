package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of a student. Direct student conflicts are allowed
 * (Problem propery Exam.AllowDirectConflict is true).
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
public class ExStudentSoft extends ExStudent {
    private HashSet[] iExams = null;
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
    public void init() {
        super.init();
        ExModel m = (ExModel)getModel();
        iExams = new HashSet[m.getNrPeriods()];
        iNrExams = new int[m.getNrPeriods()];
        for (int i=0;i<m.getNrPeriods();i++) {
            iExams[i]=new HashSet();
            iNrExams[i]=0;
        }
    }
    
    /**
     * No conflicts
     */
    public void computeConflicts(Value value, Set conflicts) {
    }
    
    /**
     * No conflicts
     */
    public boolean inConflict(Value value) {
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
    public void afterAssigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        if (iExams[p.getPeriodIndex()].add(p.variable()))
            iNrExams[p.getPeriodIndex()]++;
    }
        
    /**
     * Update assignment table
     */
    public void afterUnassigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        if (iExams[p.getPeriodIndex()].remove(p.variable()))
            iNrExams[p.getPeriodIndex()]--;
    }
    
    /** List of exams that this student has at the given period */
    public Set getExams(int period) {
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
    
    public void assigned(long iteration, Value value) {
    }
        
    public void unassigned(long iteration, Value value) {
    }    
    
}
