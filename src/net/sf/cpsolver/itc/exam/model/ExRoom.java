package net.sf.cpsolver.itc.exam.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of a room. It is ensured that the room size as well as
 * exam exclusivity is not violated.
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
public class ExRoom extends Constraint {
    private int iSize;
    private int iPenalty;
    private int[] iUsedSpace;
    private boolean[] iExclusive;
    private HashSet[] iExams;
    
    /**
     * Constructor
     * @param id unique identifier
     * @param size room size
     * @param penalty penalty for using this room
     */
    public ExRoom(int id, int size, int penalty) {
        super();
        iAssignedVariables=null;
        iId = id;
        iSize = size;
        iPenalty = penalty;
    }
    
    /**
     * Initialization
     */
    public void init() {
        ExModel m = (ExModel)getModel();
        iUsedSpace = new int[m.getNrPeriods()];
        iExams = new HashSet[m.getNrPeriods()];
        iExclusive = new boolean[m.getNrPeriods()];
        for (int i=0;i<m.getNrPeriods();i++) {
            iUsedSpace[i]=0;
            iExams[i]=new HashSet();
            iExclusive[i]=false;
        }
    }
    
    /** Return room size */
    public int getSize() {
        return iSize;
    }
    
    /** Return available space in the given period */
    public int getAvailableSpace(int period) {
        return iSize - iUsedSpace[period];
    }
    /** Return available space in the given period */
    public int getAvailableSpace(ExPeriod period) {
        return getAvailableSpace(period.getIndex());
    }

    /** Return penalty for use of this room */
    public int getPenalty() {
        return iPenalty;
    }
    
    /** Compute conflicts: check room size as well as exclusivity of the given exam, also
     * check exclusivity of assigned exams in the given period.
     */ 
    public void computeConflicts(Value value, Set conflicts) {
        ExPlacement p = (ExPlacement)value;
        if (p.getRoom().equals(this)) {
            ExExam ex = (ExExam)value.variable();
            if (ex.isRoomExclusive() || iExclusive[p.getPeriodIndex()]) {
                conflicts.addAll(iExams[p.getPeriodIndex()]);
                return;
            }
            if (iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()<=getSize()) return;
            HashSet adepts = new HashSet(iExams[p.getPeriodIndex()]);
            int rem = 0;
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                ExPlacement xp = (ExPlacement)i.next();
                if (xp.getRoom().equals(this) && xp.getPeriodIndex()==p.getPeriodIndex()) {
                    rem += ((ExExam)xp.variable()).getStudents().size();
                    adepts.remove(xp);
                }
            }
            while (iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()-rem>getSize()) {
                ExPlacement adept = null;
                int adeptDiff = 0;
                boolean adeptSufficient = false;
                for (Iterator i=adepts.iterator();i.hasNext();) {
                    ExPlacement xp = (ExPlacement)i.next();
                    int diff = getSize()-iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()-rem-xp.getNrStudents();
                    if (adept==null || (adeptDiff>0 && diff<adeptDiff) || (adeptDiff<0 && diff>adeptDiff)) {
                        adept = xp; adeptDiff = diff;
                        if (adeptDiff==0) break;
                    } 
                }
                adepts.remove(adept);
                rem += ((ExExam)adept.variable()).getStudents().size();
                conflicts.add(adept);
            }
        }
    }
    
    /** Check for conflicts: check room size as well as exclusivity of the given exam, also
     * check exclusivity of assigned exams in the given period.
     */ 
    public boolean inConflict(Value value) {
        ExPlacement p = (ExPlacement)value;
        if (!p.getRoom().equals(this)) return false;
        ExExam ex = (ExExam)value.variable();
        return (
                iExclusive[p.getPeriodIndex()] || 
                (ex.isRoomExclusive() && !iExams[p.getPeriodIndex()].isEmpty()) || 
                (iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()>getSize())
               );
    }
    
    /** Check for conflicts: check room size as well as exclusivity of the given exam, also
     * check exclusivity of assigned exams in the given period.
     */ 
    public boolean inConflict(ExExam exam, ExPeriod period) {
        return (
                iExclusive[period.getIndex()] || 
                (exam.isRoomExclusive() && !iExams[period.getIndex()].isEmpty()) || 
                (iUsedSpace[period.getIndex()]+exam.getStudents().size()>getSize())
               );
    }

    /**
     * Two exams are in conflict if they are using this room at the same time and 
     * the room is not big enough or one of these exams is room exclusive.
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExPlacement p1 = (ExPlacement)value1;
        ExPlacement p2 = (ExPlacement)value2;
        return (!p1.getRoom().equals(this) || 
                !p2.getRoom().equals(this) ||
                p1.getPeriodIndex()!=p2.getPeriodIndex() || 
                ((ExExam)p1.variable()).getStudents().size()+((ExExam)p2.variable()).getStudents().size()<getSize());
    }
    
    /**
     * Update assignments of this room
     */
    public void assigned(long iteration, Value value) {
        //super.assigned(iteration, value);
        ExPlacement p = (ExPlacement)value;
        if (p.getRoom().equals(this)) {
            HashSet confs = new HashSet();
            computeConflicts(value, confs);
            for (Iterator i=confs.iterator();i.hasNext();) {
                Value conflict = (Value)i.next();
                conflict.variable().unassign(iteration);
            }
            if (iConstraintListeners!=null)
                for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                    ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, confs);
        }
    }
        
    /**
     * Update assignments of this room
     */
    public void unassigned(long iteration, Value value) {
        //super.unassigned(iteration, value);
    }

    /**
     * Update assignments of this room
     */
    public void afterAssigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value; ExExam ex = (ExExam)value.variable();
        iExams[p.getPeriodIndex()].add(p);
        iUsedSpace[p.getPeriodIndex()]+=ex.getStudents().size();
        iExclusive[p.getPeriodIndex()]=ex.isRoomExclusive();
    }
    
    /**
     * Update assignments of this room
     */
    public void afterUnassigned(long iteration, Value value) {
        ExPlacement p = (ExPlacement)value;
        ExExam ex = (ExExam)value.variable();
        iExams[p.getPeriodIndex()].remove(p);
        iUsedSpace[p.getPeriodIndex()]-=ex.getStudents().size();
        iExclusive[p.getPeriodIndex()]=false;
    }
    
    /**
     * Room name (R + room id)
     */
    public String getName() {
        return "R"+getId();
    }
    
    /**
     * String representation
     */
    public String toString() {
        return getName()+"["+getSize()+","+getPenalty()+"]";
    }
    
    /**
     * List of durations that are of exams in the given period
     */
    public String getDurations(ExPeriod period) {
        TreeSet durations = new TreeSet();
        for (Iterator i=iExams[period.getIndex()].iterator();i.hasNext();) {
            ExExam ex = (ExExam)((ExPlacement)i.next()).variable();
            durations.add(new Integer(ex.getLength()));
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i=durations.iterator();i.hasNext();) {
            int length = ((Integer)i.next()).intValue();
            int cnt = 0;
            for (Iterator j=iExams[period.getIndex()].iterator();j.hasNext();) {
                ExExam ex = (ExExam)((ExPlacement)j.next()).variable();
                if (ex.getLength()==length) cnt++;
            }
            if (sb.length()>0) sb.append(", ");
            sb.append(cnt+"x"+length);
        }
        return "["+sb+"]";
    }
    
    /**
     * Number of mixed durations 
     */
    public int getMixedDurations() {
        ExModel m = (ExModel)getModel();
        int penalty = 0;
        for (ExPeriod p=m.firstPeriod(); p!=null; p=p.next()) {
            if (iExams[p.getIndex()].size()>1) {
                HashSet durations = new HashSet();
                for (Iterator i=iExams[p.getIndex()].iterator();i.hasNext();) {
                    ExExam ex = (ExExam)((ExPlacement)i.next()).variable();
                    durations.add(new Integer(ex.getLength()));
                }
                if (durations.size()>1)
                    penalty += durations.size()-1;
            }
        }
        return penalty;
    }

    /**
     * Room penalty
     */
    public int getRoomPenalty() {
        if (iPenalty==0) return 0;
        ExModel m = (ExModel)getModel();
        int penalty = 0;
        for (ExPeriod p=m.firstPeriod(); p!=null; p=p.next()) {
            penalty += iExams[p.getIndex()].size();
        }
        return iPenalty*penalty;
    }
    
    /**
     * List of exams that are assigned into this room at the given period
     */
    public Set getExams(int period) {
        return iExams[period];
    }
    /**
     * List of exams that are assigned into this room at the given period
     */
    public Set getExams(ExPeriod period) {
        return getExams(period.getIndex());
    }
    
}
