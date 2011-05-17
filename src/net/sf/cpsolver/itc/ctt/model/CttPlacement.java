package net.sf.cpsolver.itc.ctt.model;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.itc.heuristics.search.ItcTabuSearch.TabuElement;

/**
 * Representation of a placement of a lecture in time and space (value). A lecture can be placed in any room at any available day and time.
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
public class CttPlacement extends Value<CttLecture, CttPlacement> implements TabuElement {
    private CttRoom iRoom;
    private int iDay = 0;
    private int iSlot = 0;
    private int iHashCode = -1;
    
    /** 
     * Constraint
     * @param lecture appropriate lecture
     * @param room room assignment
     * @param day day assignment
     * @param slot time assignment
     */
    public CttPlacement(CttLecture lecture, CttRoom room, int day, int slot) {
        super(lecture);
        iRoom = room;
        iDay = day;
        iSlot = slot;
        iHashCode = (lecture.getCourse().getId()+" "+getRoom().getId()+" "+getDay()+" "+getSlot()).hashCode();
    }
    
    /** Return room assignment */
    public CttRoom getRoom() {
        return iRoom;
    }
    
    /** Return day assignment */
    public int getDay() {
        return iDay;
    }
    
    /** Return time assignment */
    public int getSlot() {
        return iSlot;
    }
    
    /** String representation */
    public String toString() {
        CttLecture lecture = variable();
        int compactPenalty = 0;
        for (CttCurricula curricula: lecture.getCourse().getCurriculas())
            compactPenalty += curricula.getCompactPenalty(this);
        return lecture.getName()+" = "+getRoom().getId()+" "+getDay()+" "+getSlot()+" ["+getRoomCapPenalty()+"+"+getMinDaysPenalty()+"+"+compactPenalty+"+"+getRoomPenalty()+"]";
    }
    
    /** Compute room capacity penalty for this placement:
     * For each lecture, the number of students that attend the course must be less or equal than 
     * the number of seats of all the rooms that host its lectures. 
     * Each student above the capacity counts as 1 point of penalty.
     */
    public int getRoomCapPenalty() {
        return Math.max(0,((CttLecture)variable()).getCourse().getNrStudents() - getRoom().getSize());
    }
    
    /** Compute room stability penalty for this placement:
     * All lectures of a course should be given in the same room. 
     * Each distinct room used for the lectures of a course, but the first, counts as 1 point of penalty.
     */
    public int getRoomPenalty() {
        CttLecture lecture = (CttLecture)variable();
        int same = 0;
        int different = 0;
        for (int i=0;i<lecture.getCourse().getNrLectures();i++) {
            if (i==lecture.getIdx()) continue;
            CttPlacement p = (CttPlacement)lecture.getCourse().getLecture(i).getAssignment();
            if (p==null) continue;
            if (p.getRoom().equals(getRoom())) same++; else different++;
        }
        return (different==0 || same!=0?0:1);
    }
    
    /** Compute room stability penalty for the given room:
     * All lectures of a course should be given in the same room. 
     * Each distinct room used for the lectures of a course, but the first, counts as 1 point of penalty.
     */
    public int getRoomPenalty(CttRoom room) {
        CttLecture lecture = (CttLecture)variable();
        int same = 0;
        int different = 0;
        for (int i=0;i<lecture.getCourse().getNrLectures();i++) {
            if (i==lecture.getIdx()) continue;
            CttPlacement p = (CttPlacement)lecture.getCourse().getLecture(i).getAssignment();
            if (p==null) continue;
            if (p.getRoom().equals(room)) same++; else different++;
        }
        return (different==0 || same!=0?0:1);
    }

    /**
     * Compute minimum working days penalty for this placement:
     * The lectures of each course must be spread into a minimum number of days. 
     * Each day below the minimum counts as 5 points of penalty.
     */
    public int getMinDaysPenalty() {
        CttLecture lecture = (CttLecture)variable();
        int days = 0;
        int nrSameDays = 0;
        boolean sameDay = false;
        for (int i=0;i<lecture.getCourse().getNrLectures();i++) {
            CttPlacement p = (i==lecture.getIdx()?this:(CttPlacement)lecture.getCourse().getLecture(i).getAssignment());
            if (p==null) continue;
            if (i!=lecture.getIdx() && p.getDay()==getDay()) sameDay=true;
            int day = 1 << p.getDay();
            if ((days & day) != 0) nrSameDays ++;
            days |= day;
        }
        return (sameDay && lecture.getCourse().getNrLectures()-nrSameDays<lecture.getCourse().getMinDays()?5:0);
    }
    
    /**
     * Compute minimum working days penalty for the given day:
     * The lectures of each course must be spread into a minimum number of days. 
     * Each day below the minimum counts as 5 points of penalty.
     */
    public int getMinDaysPenalty(int newDay) {
        CttLecture lecture = (CttLecture)variable();
        int days = 0;
        int nrSameDays = 0;
        boolean sameDay = false;
        for (int i=0;i<lecture.getCourse().getNrLectures();i++) {
            CttPlacement p = (i==lecture.getIdx()?this:(CttPlacement)lecture.getCourse().getLecture(i).getAssignment());
            if (p==null) continue;
            if (i!=lecture.getIdx() && p.getDay()==newDay) sameDay=true;
            int day = 1 << p.getDay();
            if ((days & day) != 0) nrSameDays ++;
            days |= day;
        }
        return (sameDay && lecture.getCourse().getNrLectures()-nrSameDays<lecture.getCourse().getMinDays()?5:0);
    }
    
    /**
     * Compute curriculum compactness penalty for this placement:
     * Lectures belonging to a curriculum should be adjacent to each other (i.e., in consecutive periods). 
     * For a given curriculum we account for a violation every time there is one lecture not adjacent to any 
     * other lecture within the same day. Each isolated lecture in a curriculum counts as 2 points of penalty.
     */
    public int getCompactPenalty() {
        int compactPenalty = 0;
        for (CttCurricula curricula: variable().getCourse().getCurriculas())
            compactPenalty += curricula.getCompactPenalty(this);
        return compactPenalty;
    }

    /**
     * Overall penalty for this placement
     */
    public int toInt() {
        return getRoomCapPenalty()+getRoomPenalty()+getMinDaysPenalty()+getCompactPenalty();
    }
    
    /** Overall penalty for this placement */
    public double toDouble() {
        return toInt();
    }
    
    /** Compare two placements for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttPlacement)) return false;
        CttPlacement p = (CttPlacement)o;
        if (!((CttLecture)p.variable()).getCourse().equals(((CttLecture)variable()).getCourse())) return false;
        //if (!p.variable().equals(variable())) return false;
        return (p.getDay()==getDay() && p.getSlot()==getSlot() && p.getRoom().equals(getRoom()));
    }
    
    /** Placement name (that is room id, day and time) */
    public String getName() {
        return getRoom().getId()+" "+getDay()+" "+getSlot();
    }

    /** Hash code */
    public int hashCode() {
        return iHashCode;
    }
    
    /** Placement as an element of tabu list (course id, day, time and room id -- lecture index is ignored)*/
    public Object tabuElement() {
        return ((CttLecture)variable()).getCourse().getId()+":"+getDay()+":"+getSlot()+":"+getRoom().getId();
    }
}
