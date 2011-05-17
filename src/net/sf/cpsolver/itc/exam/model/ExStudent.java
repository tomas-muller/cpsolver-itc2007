package net.sf.cpsolver.itc.exam.model;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Constraint;

/**
 * Representation of a student. Either {@link ExStudentHard} or {@link ExStudentSoft} is to be used.
 * {@link ExStudentHard} does not allow for direct conflicts.
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
public abstract class ExStudent extends Constraint<ExExam, ExPlacement> {
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
    public abstract Set<ExExam> getExams(int period);
    /** List of exams that this student has at the given period */
    public Set<ExExam> getExams(ExPeriod period) {
        return getExams(period.getIndex());
    }
    /** List of exams that this student has at the given period */
    public String getExamStr(ExPeriod period) {
        StringBuffer sb = new StringBuffer();
        for (Iterator<ExExam> i=getExams(period.getIndex()).iterator();i.hasNext();) {
            sb.append(i.next().getId());
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
            for (ExExam exam: variables()) {
                for (ExExam spex: exam.getSamePeriodExams()) {
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
    public boolean isConsistent(ExPlacement p1, ExPlacement p2) {
        ExExam ex1 = p1.variable();
        ExExam ex2 = p2.variable();
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
    public abstract void afterAssigned(long iteration, ExPlacement value);
    /** Update student assignments */
    public abstract void afterUnassigned(long iteration, ExPlacement value);
}
