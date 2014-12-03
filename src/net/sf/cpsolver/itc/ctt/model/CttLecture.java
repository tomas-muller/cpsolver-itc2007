package net.sf.cpsolver.itc.ctt.model;

import java.util.ArrayList;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.LazySwap;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Variable;

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
public class CttLecture extends Variable<CttLecture, CttPlacement> implements Swapable<CttLecture, CttPlacement> {
    private int iIdx;
    private CttCourse iCourse = null;
    private int iHashCode;
    
    /** Constructor
     * @param course course to which this lecture belongs
     * @param idx index of this lecture within given course
     */
    public CttLecture(CttCourse course, int idx) {
        super();
        iIdx = idx;
        setModel(course.getModel());
        iCourse = course;
        for (CttRoom room: getCourse().getModel().getRooms())
            room.getConstraint().addVariable(this);
        course.getTeacher().getConstraint().addVariable(this);
        for (CttCurricula curricula: getCourse().getCurriculas())
            curricula.getConstraint().addVariable(this);
        setValues(computeValues());
        getModel().addVariable(this);
        iHashCode = toString().hashCode();
    }
    
    /** Return course to which this lecture belong */
    public CttCourse getCourse() {
        return iCourse;
    }
    
    /** Domain: cartesian product of all available days and times (see {@link CttCourse#isAvailable(int, int)}) and 
     * all rooms.
     */ 
    public List<CttPlacement> computeValues() {
    	List<CttPlacement> values = new ArrayList<CttPlacement>();
        for (int d=0;d<getCourse().getModel().getNrDays();d++)
            for (int s=0;s<getCourse().getModel().getNrSlotsPerDay();s++) {
                if (getCourse().isAvailable(d,s)) {
                    for (CttRoom room: getCourse().getModel().getRooms()) {
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
        return iHashCode;
    }
    
    /** Compare two lectures, return the harder one. That is the one with more curriculas, or smaller domain/constraint ratio. */
    public int compareTo(CttLecture l) {
        if (getCourse().equals(l.getCourse()))
            return Double.compare(getIdx(),l.getIdx());

        int cmp = -Double.compare(getCourse().getCurriculas().size(), l.getCourse().getCurriculas().size());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)values().size())/constraints().size(), ((double)l.values().size())/l.constraints().size());
        if (cmp!=0) return cmp;
        
        return super.compareTo(l);
    }
    
    /** Find a swap with a placement of another lecture */
    public Neighbour<CttLecture, CttPlacement> findSwap(Assignment<CttLecture, CttPlacement> assignment, CttLecture another) {
        CttLecture lecture = (CttLecture)another;
        if (getCourse().equals(lecture.getCourse())) return null;
        CttPlacement p1 = (CttPlacement)assignment.getValue(this);
        CttPlacement p2 = (CttPlacement)assignment.getValue(lecture);
        if (p1==null || p2==null) return null;
        if (!getCourse().isAvailable(p2.getDay(),p2.getSlot())) return null;
        if (!lecture.getCourse().isAvailable(p1.getDay(), p1.getSlot())) return null;
        if (!getCourse().getTeacher().equals(lecture.getCourse().getTeacher())) {
            if (getCourse().getTeacher().getConstraint().getPlacement(assignment, p2.getDay(), p2.getSlot())!=null) return null;
            if (lecture.getCourse().getTeacher().getConstraint().getPlacement(assignment, p1.getDay(), p1.getSlot())!=null) return null;
        }
        for (CttCurricula c: getCourse().getCurriculas()) {
            CttPlacement conflict = c.getConstraint().getPlacement(assignment, p2.getDay(), p2.getSlot());
            if (conflict!=null && !conflict.variable().equals(lecture)) return null;
        }
        for (CttCurricula c: lecture.getCourse().getCurriculas()) {
            CttPlacement conflict = c.getConstraint().getPlacement(assignment, p1.getDay(), p1.getSlot());
            if (conflict!=null && !conflict.variable().equals(this)) return null;
        }
        CttPlacement np1 = new CttPlacement(this, p2.getRoom(), p2.getDay(), p2.getSlot());
        CttPlacement np2 = new CttPlacement(lecture, p1.getRoom(), p1.getDay(), p1.getSlot());
        return new LazySwap<CttLecture, CttPlacement>(np1, np2);
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
    /*
    @Override
    public void variableAssigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement value) {
        value.getRoom().getConstraint().assigned(assignment, iteration, value);
        for (CttCurricula curricula: getCourse().getCurriculas())
            curricula.getConstraint().assigned(assignment, iteration, value);
        getCourse().getTeacher().getConstraint().assigned(assignment, iteration, value);
    }
	*/
    
    /** A placement was unassigned from this lecture -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
	/*
    @Override
    public void variableUnassigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement value) {
    	value.getRoom().getConstraint().unassigned(assignment, iteration, value);
        for (CttCurricula curricula: getCourse().getCurriculas())
            curricula.getConstraint().unassigned(assignment, iteration, value);
        getCourse().getTeacher().getConstraint().unassigned(assignment, iteration, value);
    }
    */
    
}
