package net.sf.cpsolver.itc.exam.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of a student. Direct student conflicts are not allowed
 * (Problem propery Exam.AllowDirectConflict is false).
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
public class ExStudentHard extends ExStudent {
    private static Logger sLog = Logger.getLogger(ExStudent.class);
    private ExPlacement[] iTable = null;
    private Boolean iHasSamePeriodExams = null;
    
    /**
     * Constructor
     * @param id unique identifier
     */
    public ExStudentHard(int id) {
        super(id);
    }
    
    /**
     * Initialization
     */
    public void init() {
        super.init();
        ExModel m = (ExModel)getModel();
        iTable = new ExPlacement[m.getNrPeriods()];
        for (int i=0;i<m.getNrPeriods();i++)
            iTable[i]=null;
    }
    
    /**
     * Compute conflicts: i.e., exams that are placed at the same period as the given one
     */
    public void computeConflicts(Value value, Set conflicts) {
        ExPlacement p = (ExPlacement)value;
        if (iTable[p.getPeriodIndex()]!=null && !iTable[p.getPeriodIndex()].variable().equals(p.variable())) 
            conflicts.add(iTable[p.getPeriodIndex()]);
    }
    
    /**
     * Check for conflicts: i.e., exams that are placed at the same period as the given one
     */
    public boolean inConflict(Value value) {
        ExPlacement p = (ExPlacement)value;
        return iTable[p.getPeriodIndex()]!=null && !iTable[p.getPeriodIndex()].variable().equals(p.variable());
    }
    
    public void assigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        if (iTable[p.getPeriodIndex()]!=null) {
            HashSet confs = new HashSet(); confs.add(iTable[p.getPeriodIndex()]);
            iTable[p.getPeriodIndex()].variable().unassign(iteration);
            if (iConstraintListeners!=null)
                for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                    ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, confs);
        }
    }
        
    public void unassigned(long iteration, Value value) {
    }    
    
    /**
     * Update assignment table
     */
    public void afterAssigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        //if (iTable[p.getPeriodIndex()]!=null) throw new RuntimeException("Direct conflic between "+p+" and "+iTable[p.getPeriodIndex()]+" for student "+this);
        iTable[p.getPeriodIndex()] = p;
    }
        
    /**
     * Update assignment table
     */
    public void afterUnassigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        //if (iTable[p.getPeriodIndex()]==null) throw new RuntimeException("Nothing assigned to "+p.getPeriod()+" for student "+this);
        iTable[p.getPeriodIndex()] = null;
    }
    
    /**
     * Return an exam that is assigned to the given period (if any)
     */
    public ExPlacement getPlacement(int period) {
        return iTable[period];
    }
    /**
     * Return an exam that is assigned to the given period (if any)
     */
    public ExPlacement getPlacement(ExPeriod period) {
        return getPlacement(period.getIndex());
    }
    /**
     * Return exams that are assigned to the given period
     */
    public Set getExams(int period) {
        HashSet set = new HashSet();
        if (iTable[period]!=null) set.add(iTable[period].variable());
        return set;
    }
    
    /** True, if this student has one or more exams at the given period */
    public boolean hasExam(int period) {
        return (iTable[period]!=null);
    }
    /** True, if this student has one or more exams at the given period (given exam excluded) */
    public boolean hasExam(int period, ExExam exclude) {
        return (iTable[period]!=null && !iTable[period].variable().equals(exclude));
    }
    /** Number of exams that this student has at the given period */
    public int nrExams(int period) {
        return (hasExam(period)?1:0);
    }
    /** Number of exams that this student has at the given period (given exam excluded)*/
    public int nrExams(int period, ExExam exclude) {
        return (hasExam(period,exclude)?1:0);
    }
}
