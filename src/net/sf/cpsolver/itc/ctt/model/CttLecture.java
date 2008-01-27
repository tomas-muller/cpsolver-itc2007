package net.sf.cpsolver.itc.ctt.model;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Representation of a lecture (variable). A lecture is associated with a course
 * and can be placed in a room of sufficient capacity at available day and time
 * respecting all existing constraints.
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
public class CttLecture extends Variable implements Swapable {
    private int iIdx;
    private CttCourse iCourse = null;
    
    /** Constructor
     * @param course course to which this lecture belongs
     * @param idx index of this lecture within given course
     */
    public CttLecture(CttCourse course, int idx) {
        super();
        iIdx = idx;
        setModel(course.getModel());
        iCourse = course;
        for (Enumeration e=getCourse().getModel().getRooms().elements();e.hasMoreElements();) {
            CttRoom room = (CttRoom)e.nextElement();
            room.getConstraint().addVariable(this);
        }
        course.getTeacher().getConstraint().addVariable(this);
        for (Enumeration e=getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            curricula.getConstraint().addVariable(this);
        }
        setValues(computeValues());
        getModel().addVariable(this);
    }
    
    /** Return course to which this lecture belong */
    public CttCourse getCourse() {
        return iCourse;
    }
    
    /** Domain: cartesian product of all available days and times (see {@link CttCourse#isAvailable(int, int)}) and 
     * all rooms.
     */ 
    public Vector computeValues() {
        Vector values = new Vector();
        for (int d=0;d<getCourse().getModel().getNrDays();d++)
            for (int s=0;s<getCourse().getModel().getNrSlotsPerDay();s++) {
                if (getCourse().isAvailable(d,s)) {
                    for (Enumeration e=getCourse().getModel().getRooms().elements();e.hasMoreElements();) {
                        CttRoom room = (CttRoom)e.nextElement();
                        //if (room.getSize()<getCourse().getNrStudents()) continue;
                        values.add(new CttPlacement(this, room, d, s));
                    }
                }
            }
        return values;
    }

    /** Lecture name: course id / index */
    public String getName() {
        return getCourse().getId()+"/"+iIdx;
    }

    /** String representation */
    public String toString() {
        return getCourse().getId()+"/"+iIdx;
    }
    
    /** Lecture index within given course */
    public int getIdx() {
        return iIdx;
    }
    
    /** Compare two lectures for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttLecture)) return false;
        CttLecture l = (CttLecture)o;
        return (l.getIdx()==getIdx() && l.getCourse().equals(getCourse()));
    }
    
    /** Hash code */
    public int hashCode() {
        return toString().hashCode();
    }
    
    /** Compare two lectures, return the harder one. That is the one with more curriculas, or smaller domain/constraint ratio. */
    public int compareTo(Object o) {
        CttLecture l = (CttLecture)o;
        
        if (getCourse().equals(l.getCourse()))
            return Double.compare(getIdx(),l.getIdx());

        int cmp = -Double.compare(getCourse().getCurriculas().size(), l.getCourse().getCurriculas().size());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)values().size())/constraints().size(), ((double)l.values().size())/l.constraints().size());
        if (cmp!=0) return cmp;
        
        return super.compareTo(o);
    }
    
    /** Find a swap with a placement of another lecture */
    public Neighbour findSwap(Variable another) {
        CttLecture lecture = (CttLecture)another;
        if (getCourse().equals(lecture.getCourse())) return null;
        CttPlacement p1 = (CttPlacement)getAssignment();
        CttPlacement p2 = (CttPlacement)lecture.getAssignment();
        if (p1==null || p2==null) return null;
        if (!getCourse().isAvailable(p2.getDay(),p2.getSlot())) return null;
        if (!lecture.getCourse().isAvailable(p1.getDay(), p1.getSlot())) return null;
        if (!getCourse().getTeacher().equals(lecture.getCourse().getTeacher())) {
            if (getCourse().getTeacher().getConstraint().getPlacement(p2.getDay(), p2.getSlot())!=null) return null;
            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(p1.getDay(), p1.getSlot())!=null) return null;
        }
        for (Enumeration e=getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula c = (CttCurricula)e.nextElement();
            CttPlacement conflict = c.getConstraint().getPlacement(p2.getDay(), p2.getSlot());
            if (conflict!=null && !conflict.variable().equals(lecture)) return null;
        }
        for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula c = (CttCurricula)e.nextElement();
            CttPlacement conflict = c.getConstraint().getPlacement(p1.getDay(), p1.getSlot());
            if (conflict!=null && !conflict.variable().equals(this)) return null;
        }
        CttPlacement np1 = new CttPlacement(this, p2.getRoom(), p2.getDay(), p2.getSlot());
        CttPlacement np2 = new CttPlacement(lecture, p1.getRoom(), p1.getDay(), p1.getSlot());
        return new ItcLazySwap(np1, np2);
        /*
        double value = 0;
        //value += np1.extraPenalty() + np2.extraPenalty() - p1.extraPenalty() - p2.extraPenalty();
        value += np1.getRoomCapPenalty() + np2.getRoomCapPenalty() - p1.getRoomCapPenalty() - p2.getRoomCapPenalty();
        value += np1.getMinDaysPenalty() + np2.getMinDaysPenalty() - p1.getMinDaysPenalty() - p2.getMinDaysPenalty();
        value += np1.getRoomPenalty() + np2.getRoomPenalty() - p1.getRoomPenalty() - p2.getRoomPenalty();
        double[] mvalue = new double[] { 
                np1.getRoomCapPenalty() + np2.getRoomCapPenalty() - p1.getRoomCapPenalty() - p2.getRoomCapPenalty(),
                np1.getMinDaysPenalty() + np2.getMinDaysPenalty() - p1.getMinDaysPenalty() - p2.getMinDaysPenalty(),
                0,
                np1.getRoomPenalty() + np2.getRoomPenalty() - p1.getRoomPenalty() - p2.getRoomPenalty()};
        for (Enumeration e=getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            if (lecture.getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np1) - curricula.getCompactPenalty(p1);
            mvalue[2] += curricula.getCompactPenalty(np1) - curricula.getCompactPenalty(p1);
        }
        for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            if (getCourse().getCurriculas().contains(curricula)) continue;
            value += curricula.getCompactPenalty(np2) - curricula.getCompactPenalty(p2);
            mvalue[2] += curricula.getCompactPenalty(np2) - curricula.getCompactPenalty(p2);
        }
        return new MultiCriterialSwap(np1, np2, value, mvalue);*/
    }
    
    /** A placement was assigned to this lecture -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void assign(long iteration, Value value) {
        getModel().beforeAssigned(iteration,value);
        if (iValue!=null) unassign(iteration);
        if (value==null) return;
        iValue = value;
        CttPlacement placement = (CttPlacement)iValue;
        placement.getRoom().getConstraint().assigned(iteration, value);
        for (Enumeration e=getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            curricula.getConstraint().assigned(iteration, value);
        }
        getCourse().getTeacher().getConstraint().assigned(iteration, value);
        value.assigned(iteration);
        getModel().afterAssigned(iteration,value);
    }
    
    /** A placement was unassigned from this lecture -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void unassign(long iteration) {
        if (iValue==null) return;
        getModel().beforeUnassigned(iteration,iValue);
        Value oldValue = iValue;
        iValue = null;
        CttPlacement placement = (CttPlacement)oldValue;
        placement.getRoom().getConstraint().unassigned(iteration, oldValue);
        for (Enumeration e=getCourse().getCurriculas().elements();e.hasMoreElements();) {
            CttCurricula curricula = (CttCurricula)e.nextElement();
            curricula.getConstraint().unassigned(iteration, oldValue);
        }
        getCourse().getTeacher().getConstraint().unassigned(iteration, oldValue);
        getModel().afterUnassigned(iteration,oldValue);
    }
    
}
