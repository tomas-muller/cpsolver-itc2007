package net.sf.cpsolver.itc.exam.model;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a room. It is ensured that the room size as well as
 * exam exclusivity is not violated.
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
public class ExRoom extends Constraint<ExExam, ExPlacement> {
    private int iSize;
    private int iPenalty;
    private int[] iUsedSpace;
    private boolean[] iExclusive;
    private Set<ExPlacement>[] iExams;
    
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
    @SuppressWarnings("unchecked")
	public void init() {
        ExModel m = (ExModel)getModel();
        iUsedSpace = new int[m.getNrPeriods()];
        iExams = new HashSet[m.getNrPeriods()];
        iExclusive = new boolean[m.getNrPeriods()];
        for (int i=0;i<m.getNrPeriods();i++) {
            iUsedSpace[i]=0;
            iExams[i]=new HashSet<ExPlacement>();
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
    public void computeConflicts(ExPlacement p, Set<ExPlacement> conflicts) {
        if (p.getRoom().equals(this)) {
            ExExam ex = p.variable();
            if (ex.isRoomExclusive() || iExclusive[p.getPeriodIndex()]) {
                conflicts.addAll(iExams[p.getPeriodIndex()]);
                return;
            }
            if (iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()<=getSize()) return;
            Set<ExPlacement> adepts = new HashSet<ExPlacement>(iExams[p.getPeriodIndex()]);
            int rem = 0;
            for (ExPlacement xp: conflicts) {
                if (xp.getRoom().equals(this) && xp.getPeriodIndex()==p.getPeriodIndex()) {
                    rem += ((ExExam)xp.variable()).getStudents().size();
                    adepts.remove(xp);
                }
            }
            while (iUsedSpace[p.getPeriodIndex()]+ex.getStudents().size()-rem>getSize()) {
                ExPlacement adept = null;
                int adeptDiff = 0;
                for (ExPlacement xp: adepts) {
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
    public boolean inConflict(ExPlacement p) {
        if (!p.getRoom().equals(this)) return false;
        ExExam ex = p.variable();
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
    public boolean isConsistent(ExPlacement p1, ExPlacement p2) {
        return (!p1.getRoom().equals(this) || 
                !p2.getRoom().equals(this) ||
                p1.getPeriodIndex()!=p2.getPeriodIndex() || 
                ((ExExam)p1.variable()).getStudents().size()+((ExExam)p2.variable()).getStudents().size()<getSize());
    }
    
    /**
     * Update assignments of this room
     */
    public void assigned(long iteration, ExPlacement p) {
        //super.assigned(iteration, p);
        if (p.getRoom().equals(this)) {
            Set<ExPlacement> confs = new HashSet<ExPlacement>();
            computeConflicts(p, confs);
            for (ExPlacement conflict: confs)
                conflict.variable().unassign(iteration);
            if (iConstraintListeners!=null)
                for (ConstraintListener<ExPlacement> listener: iConstraintListeners)
                    listener.constraintAfterAssigned(iteration, this, p, confs);
        }
    }
        
    /**
     * Update assignments of this room
     */
    public void unassigned(long iteration, ExPlacement p) {
        //super.unassigned(iteration, p);
    }

    /**
     * Update assignments of this room
     */
    public void afterAssigned(long iteration, ExPlacement p) {
        ExExam ex = p.variable();
        iExams[p.getPeriodIndex()].add(p);
        iUsedSpace[p.getPeriodIndex()]+=ex.getStudents().size();
        iExclusive[p.getPeriodIndex()]=ex.isRoomExclusive();
    }
    
    /**
     * Update assignments of this room
     */
    public void afterUnassigned(long iteration, ExPlacement p) {
        ExExam ex = p.variable();
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
        TreeSet<Integer> durations = new TreeSet<Integer>();
        for (ExPlacement p: iExams[period.getIndex()])
            durations.add(new Integer(p.variable().getLength()));
        StringBuffer sb = new StringBuffer();
        for (Integer length: durations) {
            int cnt = 0;
            for (ExPlacement p: iExams[period.getIndex()])
                if (p.variable().getLength()==length) cnt++;
            if (sb.length()>0) sb.append(", ");
            sb.append(cnt + "x" + length);
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
                Set<Integer> durations = new HashSet<Integer>();
                for (ExPlacement q: iExams[p.getIndex()])
                    durations.add(new Integer(q.variable().getLength()));
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
    public Set<ExPlacement> getExams(int period) {
        return iExams[period];
    }
    /**
     * List of exams that are assigned into this room at the given period
     */
    public Set<ExPlacement> getExams(ExPeriod period) {
        return getExams(period.getIndex());
    }
    
}
