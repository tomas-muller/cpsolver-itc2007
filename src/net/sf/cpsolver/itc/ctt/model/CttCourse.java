package net.sf.cpsolver.itc.ctt.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;

/**
 * Representation of a course. A course consists of a teacher (see {@link CttTeacher}, given number of lectures (see {@link CttLecture}),
 * minimal number of days, number of students, and a list of curriculas (see {@link CttCurricula}. Moreover, some times may not be available.
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
public class CttCourse {
    private CttModel iModel = null;
    private String iId = null;
    private CttTeacher iTeacher = null;
    private int iNrLectures = 0;
    private int iMinDays = 0;
    private int iNrStudents = 0;
    private boolean[][] iAvailable = null;
    private CttLecture[] iLectures = null;
    private List<CttCurricula> iCurriculas = new ArrayList<CttCurricula>();
    
    /**
     * Constructor
     * @param model problem model
     * @param id unique identifier
     * @param teacher a teacher associated with this course
     * @param nrLectures number of lectures
     * @param minDays minimal number of days over which the given lectures should be spread
     * @param nrStudents number of students taking this course
     */
    public CttCourse(CttModel model, String id, CttTeacher teacher, int nrLectures, int minDays, int nrStudents) {
        iModel = model;
        iId = id;
        iTeacher = teacher;
        iNrLectures = nrLectures;
        iMinDays = minDays;
        iNrStudents = nrStudents;
        iAvailable = new boolean[getModel().getNrDays()][getModel().getNrSlotsPerDay()];
        for (int d=0;d<getModel().getNrDays();d++)
            for (int s=0;s<getModel().getNrSlotsPerDay();s++)
                iAvailable[d][s] = true;
    }
    
    /** Initialization */
    public void init() {
        if (iLectures!=null) return;
        iLectures=new CttLecture[getNrLectures()];
        for (int i=0;i<getNrLectures();i++) {
            iLectures[i] = new CttLecture(this,i);
        }
    }
    
    /** Return given lecture 
     * @param idx 0 .. {@link CttCourse#getNrLectures()} - 1
     */
    public CttLecture getLecture(int idx) {
        return iLectures[idx];
    }
    
    /** Return problem model */
    public CttModel getModel() {
        return iModel;
    }
    
    /** Return unique identifier */
    public String getId() {
        return iId;
    }
    
    /** Return teacher associated with this course */
    public CttTeacher getTeacher() {
        return iTeacher;
    }
    
    /** Return number of lectures of this course */
    public int getNrLectures() {
        return iNrLectures;
    }
    
    /** Return minimal number of days over which the lectures of this course should be spread */
    public int getMinDays() {
        return iMinDays;
    }
    
    /** Return number of students */
    public int getNrStudents() {
        return iNrStudents;
    }
    
    /** Return true when this course can have a lecture on given day and time */
    public boolean isAvailable(int day, int slot) {
        return iAvailable[day][slot];
    }
    
    /** Set whether this course can have a lecture on given day and time */
    public void setAvailable(int day, int slot, boolean av) {
        iAvailable[day][slot] = av;
    }

    /** Compare two courses for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttCourse)) return false;
        return getId().equals(((CttCourse)o).getId());
    }
    
    /** Hash code */
    public int hashCode() {
        return getId().hashCode();
    }
    
    /** String representation */
    public String toString() {
        return getId()+" "+getTeacher().getId()+" "+getNrLectures()+" "+getMinDays()+" "+getNrStudents();
    }
    
    /** Compute minimal days penalty: 
     * The lectures of each course must be spread into a minimum number of days. 
     * Each day below the minimum counts as 5 points of penalty.
     */
    public int getMinDaysPenalty(Assignment<CttLecture, CttPlacement> assignment) {
        int days = 0, nrDays = 0;
        for (int i=0;i<iLectures.length;i++) {
        	CttPlacement p = assignment.getValue(iLectures[i]);
            if (p == null) {
                nrDays++;
            } else {
                int day = 1 << p.getDay();
                if ((days & day) == 0) nrDays ++;
                days |= day;
            }
        }
        return 5*Math.max(0, getMinDays() - nrDays);
    }
    
    /** Compute room penalty:
     * All lectures of a course should be given in the same room. 
     * Each distinct room used for the lectures of a course, but the first, counts as 1 point of penalty.
     */
    public int getRoomPenalty(Assignment<CttLecture, CttPlacement> assignment) {
        return Math.max(0, getRooms(assignment).size() - 1);
    }
    
    /**
     * Compute all rooms into which lectures of this course are assigned.
     */
    public Set<CttRoom> getRooms(Assignment<CttLecture, CttPlacement> assignment) {
    	Set<CttRoom> rooms = new HashSet<CttRoom>();
        for (int i=0;i<iLectures.length;i++) {
            CttPlacement p = assignment.getValue(iLectures[i]);
            if (p!=null) rooms.add(p.getRoom());
        }
        return rooms;
    }
    
    /**
     * Return curriculas associated with this course.
     */
    public List<CttCurricula> getCurriculas() {
        return iCurriculas;
    }
}
