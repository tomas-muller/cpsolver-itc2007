package net.sf.cpsolver.itc.exam.model;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of a student. Either {@link ExStudentHard} or {@link ExStudentSoft} is to be used.
 * {@link ExStudentHard} does not allow for direct conflicts.
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
public abstract class ExStudent extends Constraint {
    protected static Logger sLog = Logger.getLogger(ExStudent.class);
    private Boolean iHasSamePeriodExams = null;

    /**
     * Constructor
     * @param id student unique id
     */
    public ExStudent(int id) {
        super();
        iAssignedVariables=null;
        iId = id;
    }
    
    /** Constraint initialization */
    public void init() {
        if (hasSamePeriodExams()) {
            sLog.warn("Student "+getId()+" has two or more exams that meet in the same period.");
        }
    }
    
    /** List of exams that this student has at the given period */
    public abstract Set getExams(int period);
    /** List of exams that this student has at the given period */
    public Set getExams(ExPeriod period) {
        return getExams(period.getIndex());
    }
    /** List of exams that this student has at the given period */
    public String getExamStr(ExPeriod period) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=getExams(period.getIndex()).iterator();i.hasNext();) {
            sb.append(((ExExam)i.next()).getId());
            sb.append(i.hasNext()?",":"");
        }
        return sb.toString();
    }
    /** True, if this student has one or more exams at the given period */
    public abstract boolean hasExam(int period);
    /** True, if this student has one or more exams at the given period */
    public boolean hasExam(ExPeriod period) {
        return hasExam(period.getIndex());
    }
    /** True, if this student has one or more exams at the given period (given exam excluded) */
    public abstract boolean hasExam(int period, ExExam exclude);
    /** True, if this student has one or more exams at the given period (given exam excluded) */
    public boolean hasExam(ExPeriod period, ExExam exclude) {
        return hasExam(period.getIndex(), exclude);
    }
    /** Number of exams that this student has at the given period */
    public abstract int nrExams(int period);
    /** Number of exams that this student has at the given period */
    public int nrExams(ExPeriod period) { return nrExams(period.getIndex()); }
    /** Number of exams that this student has at the given period (given exam excluded) */
    public abstract int nrExams(int period, ExExam exclude);
    /** Number of exams that this student has at the given period (given exam excluded) */
    public int nrExams(ExPeriod period, ExExam exclude) {
        return nrExams(period.getIndex(), exclude);
    }

    /** 
     * True if this student is attending two exams that are linked with same period constraint.
     * Such a case is currently not allowed.
     */
    public boolean hasSamePeriodExams() {
        if (iHasSamePeriodExams==null) {
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                ExExam exam = (ExExam)e.nextElement();
                for (Iterator i=exam.getSamePeriodExams().iterator();i.hasNext();) {
                    ExExam spex = (ExExam)i.next();
                    if (variables().contains(spex)) {
                        iHasSamePeriodExams = Boolean.TRUE; 
                        return true;
                    }
                }
            }
            iHasSamePeriodExams = Boolean.FALSE;
            return false;
        }
        return iHasSamePeriodExams.booleanValue();
    }
    
    /** Two exams are in conflict if both attended by this student and placed at the same period */
    public boolean isConsistent(Value value1, Value value2) {
        ExPlacement p1 = (ExPlacement)value1;
        ExPlacement p2 = (ExPlacement)value2;
        ExExam ex1 = (ExExam)value1.variable();
        ExExam ex2 = (ExExam)value2.variable();
        if (ex1.isSamePeriodExam(ex2)) return true;
        return (p1.getPeriodIndex()!=p2.getPeriodIndex()); 
    }
    
    /** String representation */
    public String toString() {
        return "S"+getId();
    }
    

    /** 
     * Number of two in a row exams for this student
     */
    public int getTwoInARow() {
        ExModel m = (ExModel)getModel();
        if (m.getNrTimes()<2) return 0;
        int penalty = 0;
        ExPeriod p = m.firstPeriod();
        while (p.next()!=null) {
            if (p.getDay()==p.next().getDay() && hasExam(p) && hasExam(p.next())) penalty++;
            p = p.next();
        }
        return penalty;
    }
    
    /** 
     * Number of two in a day exams for this student
     */
    public int getTwoInADay() {
        ExModel m = (ExModel)getModel();
        if (m.getNrTimes()<=2) return 0;
        int penalty = 0;
        ExPeriod p1 = m.firstPeriod();
        while (p1.next().next()!=null) {
            if (hasExam(p1)) {
                ExPeriod p2 = p1.next().next();
                while (p2!=null && p2.getDay()==p1.getDay()) {
                    if (hasExam(p2)) penalty++;
                    p2 = p2.next();
                }
            }
            p1 = p1.next();
        }
        return penalty;
    }
    
    /** 
     * Number of period spread exams for this student
     */
    public int getWiderSpread(int periodLength) {
        ExModel m = (ExModel)getModel();
        int penalty = 0;
        ExPeriod p1 = m.firstPeriod();
        while (p1.next()!=null) {
            if (hasExam(p1)) {
                ExPeriod p2 = p1.next();
                while (p2!=null && p2.getIndex()-p1.getIndex()<=periodLength) {
                    if (hasExam(p2)) penalty++;
                    p2 = p2.next();
                }
            }
            p1 = p1.next();
        }
        return penalty;
    }
    
    /** 
     * Number of direct conflicts for this student (that is exams that are placed at the same period)
     */
    public int getNrDirectConflicts() {
        int conflicts = 0;
        ExModel m = (ExModel)getModel();
        for (ExPeriod p=m.firstPeriod();p!=null;p=p.next())
            conflicts += Math.max(0, nrExams(p)-1);
        return conflicts;
    }
    
    /** Update student assignments */
    public abstract void afterAssigned(long iteration, Value value);
    /** Update student assignments */
    public abstract void afterUnassigned(long iteration, Value value);
}
