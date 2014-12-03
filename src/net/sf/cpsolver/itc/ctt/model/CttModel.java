package net.sf.cpsolver.itc.ctt.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.solution.Solution;

import net.sf.cpsolver.itc.ItcModel;

/**
 * Representation of Curriculum based Course Timetabling (CTT) problem model.
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
public class CttModel extends ItcModel<CttLecture, CttPlacement> implements CanInheritContext<CttLecture, CttPlacement, AssignmentConstraintContext<CttLecture, CttPlacement>> {
    private static Logger sLog = Logger.getLogger(CttModel.class); 
    private String iName = null;
    private int iNrDays = 0;
    private int iNrSlotsPerDay = 0;
    private List<CttCourse> iCourses = new ArrayList<CttCourse>();
    private List<CttRoom> iRooms = new ArrayList<CttRoom>();
    private List<CttCurricula> iCurriculas = new ArrayList<CttCurricula>();
    private List<CttTeacher> iTeachers = new ArrayList<CttTeacher>();
    
    /** Constructor */
    public CttModel() {
        super();
    }
    
    /** Return instance name */
    public String getName() {
        return iName;
    }
    /** Set instance name */
    public void setName(String name) {
        iName = name;
    }
    /** Return number of days */
    public int getNrDays() {
        return iNrDays;
    }
    /** Set number of days */
    public void setNrDays(int nrDays) {
        iNrDays = nrDays;
    }
    /** Return number of times (slots) per day */
    public int getNrSlotsPerDay() {
        return iNrSlotsPerDay;
    }
    /** Set number of times (slots) per day */
    public void setNrSlotsPerDay(int nrSlotsPerDay) {
        iNrSlotsPerDay = nrSlotsPerDay;
    }
    /** List of all courses */
    public List<CttCourse> getCourses() {
        return iCourses;
    }
    /** Return a course of given id */
    public CttCourse getCourse(String id) {
        for (CttCourse course: iCourses)
            if (id.equals(course.getId())) return course;
        return null;
    }

    /** List of all rooms */
    public List<CttRoom> getRooms() {
        return iRooms;
    }
    /** Return a room of given id */
    public CttRoom getRoom(String id) {
        for (CttRoom room: iRooms)
            if (id.equals(room.getId())) return room;
        return null;
    }
    
    /** List of all curriculas */
    public List<CttCurricula> getCurriculas() {
        return iCurriculas;
    }
    
    /** Return a curricula of given id */
    public CttCurricula getCurricula(String id) {
        for (CttCurricula curricula: iCurriculas)
            if (id.equals(curricula.getId())) return curricula;
        return null;
    }
    
    /** List of all teachers */
    public List<CttTeacher> getTeachers() {
        return iTeachers;
    }
    /** Return a teacher of given id */
    public CttTeacher getTeacher(String id) {
        for (CttTeacher teacher: iTeachers)
            if (id.equals(teacher.getId())) return teacher;
        CttTeacher teacher = new CttTeacher(this, id);
        iTeachers.add(teacher);
        return teacher;
    }
    
    public Penalties getPenalties(Assignment<CttLecture, CttPlacement> assignment) {
    	return (Penalties) getContext(assignment);
    }
    
    /** 
     * Curriculum compactness penalty:
     * Lectures belonging to a curriculum should be adjacent to each other (i.e., in consecutive periods). For a given curriculum we account for a violation every time there is one lecture not adjacent to any other lecture within the same day. Each isolated lecture in a curriculum counts as 2 points of penalty.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getCompactPenalty(Assignment<CttLecture, CttPlacement> assignment, boolean precise) {
        if (!precise) return getPenalties(assignment).getCompactPenalty();
        int penalty = 0;
        for (CttCurricula curricula: iCurriculas)
            penalty += curricula.getCompactPenalty(assignment);
        return penalty;
    }
    

    /** 
     * Room stability penalty:
     * All lectures of a course should be given in the same room. Each distinct room used for the lectures of a course, but the first, counts as 1 point of penalty.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getRoomPenalty(Assignment<CttLecture, CttPlacement> assignment, boolean precise) {
        if (!precise) return getPenalties(assignment).getRoomPenalty();
        int penalty = 0;
        for (CttCourse course: iCourses)
            penalty += course.getRoomPenalty(assignment);
        return penalty;
    }
    
    /**
     * Minimal working days penalty:
     * The lectures of each course must be spread into a minimum number of days. Each day below the minimum counts as 5 points of penalty.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getMinDaysPenalty(Assignment<CttLecture, CttPlacement> assignment, boolean precise) {
        if (!precise) return getPenalties(assignment).getMinDaysPenalty();
        int penalty = 0;
        for (CttCourse course: iCourses)
            penalty += course.getMinDaysPenalty(assignment);
        return penalty;
    }

    /**
     * Room capacity penalty:
     * For each lecture, the number of students that attend the course must be less or equal than the number of seats of all the rooms that host its lectures. Each student above the capacity counts as 1 point of penalty.
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public int getRoomCapPenalty(Assignment<CttLecture, CttPlacement> assignment, boolean precise) {
        if (!precise) return getPenalties(assignment).getRoomCapPenalty();
        int penalty = 0;
        for (CttLecture lecture: variables()) {
        	CttPlacement placement = assignment.getValue(lecture);
            if (placement != null)
                penalty += placement.getRoomCapPenalty();
        }
        return penalty;
    }
    
    /**
     * Overall solution value
     */
    public double getTotalValue(Assignment<CttLecture, CttPlacement> assignment) {
        return getTotalValue(assignment, false);
    }
        
    /**
     * Overall solution value
     * @param precise true -- precise computation, false -- use inner counter (for speed up)
     */
    public double getTotalValue(Assignment<CttLecture, CttPlacement> assignment, boolean precise) {
        return 
            getCompactPenalty(assignment, precise)+
            getRoomCapPenalty(assignment, precise)+
            getMinDaysPenalty(assignment, precise)+
            getRoomPenalty(assignment, precise);
    }
    
    /**
     * Solution info, added values of given criteria, that is, room capacity penalty, minimum working days penalty, curriculum compactness penalty, and room stability penalty.
     */
    public Map<String, String> getInfo(Assignment<CttLecture, CttPlacement> assignment) {
    	Map<String, String> info = super.getInfo(assignment);
        info.put("RoomCapacity",String.valueOf(getRoomCapPenalty(assignment, false)));
        info.put("MinimumWorkingDays",String.valueOf(getMinDaysPenalty(assignment, false)));
        info.put("CurriculumCompactness",String.valueOf(getCompactPenalty(assignment, false)));
        info.put("RoomStability",String.valueOf(getRoomPenalty(assignment, false)));
        return info;
    }
    
    /**
     * Extended solution info, added precise computation of values of given criteria, that is, room capacity penalty, minimum working days penalty, curriculum compactness penalty, and room stability penalty.
     */
    public Map<String, String> getExtendedInfo(Assignment<CttLecture, CttPlacement> assignment) {
    	Map<String, String> info = super.getExtendedInfo(assignment);
        info.put("RoomCapacity [p]",String.valueOf(getRoomCapPenalty(assignment, true)));
        info.put("MinimumWorkingDays [p]",String.valueOf(getMinDaysPenalty(assignment, true)));
        info.put("CurriculumCompactness [p]",String.valueOf(getCompactPenalty(assignment, true)));
        info.put("RoomStability [p]",String.valueOf(getRoomPenalty(assignment, true)));
        return info;
    }    
    
    /**
     * Load problem from a file, see <a href='http://www.cs.qub.ac.uk/itc2007/curriculmcourse/course_curriculm_index_files/Inputformat.htm'>input format</a> description.
     */
    public boolean load(File file) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line=in.readLine()).trim().length()>0) {
            String param = line.substring(0, line.indexOf(':')).trim();
            String value = line.substring(line.indexOf(':')+1).trim();
            sLog.info(param+": "+value);
            if ("Name".equals(param))
                setName(value);
            if ("Days".equals(param))
                setNrDays(Integer.parseInt(value));
            if ("Periods_per_day".equals(param))
                setNrSlotsPerDay(Integer.parseInt(value));
        }
        while ((line=in.readLine())!=null) {
            if ("END.".equals(line)) break;
            if ("COURSES:".equals(line)) {
                while ((line=in.readLine()).trim().length()>0) {
                    StringTokenizer stk = new StringTokenizer(line, " \t");
                    getCourses().add(new CttCourse(
                            this,
                            stk.nextToken(),
                            getTeacher(stk.nextToken()),
                            Integer.parseInt(stk.nextToken()),
                            Integer.parseInt(stk.nextToken()),
                            Integer.parseInt(stk.nextToken())));
                }
            }
            if ("ROOMS:".equals(line)) {
                while ((line=in.readLine()).trim().length()>0) {
                    StringTokenizer stk = new StringTokenizer(line, " \t");
                    getRooms().add(new CttRoom(
                            this,
                            stk.nextToken(),
                            Integer.parseInt(stk.nextToken())));
                }
            }
            if ("CURRICULA:".equals(line)) {
                while ((line=in.readLine()).trim().length()>0) {
                    StringTokenizer stk = new StringTokenizer(line, " \t");
                    CttCurricula curricula = new CttCurricula(this, stk.nextToken());
                    getCurriculas().add(curricula);
                    int nrCourses = Integer.parseInt(stk.nextToken());
                    for (int i=0;i<nrCourses;i++) {
                        CttCourse course = getCourse(stk.nextToken());
                        course.getCurriculas().add(curricula);
                        curricula.getCourses().add(course);
                    }
                }
            }
            if ("UNAVAILABILITY_CONSTRAINTS:".equals(line)) {
                while ((line=in.readLine()).trim().length()>0) {
                    StringTokenizer stk = new StringTokenizer(line, " \t");
                    CttCourse course = getCourse(stk.nextToken());
                    course.setAvailable(
                            Integer.parseInt(stk.nextToken()),
                            Integer.parseInt(stk.nextToken()),
                            false);
                }
            }
        }
        
        in.close();
        
        for (CttCourse course: getCourses())
            course.init();
        
        return true;
    }
    
    /**
     * Save solution into a file, see <a href='http://www.cs.qub.ac.uk/itc2007/curriculmcourse/course_curriculm_index_files/outputformat.htm'>output format</a> description.
     */
    public boolean save(Assignment<CttLecture, CttPlacement> assignment, File file) throws Exception {
        PrintWriter w = new PrintWriter(new FileWriter(file));
        for (CttCourse course: getCourses()) {
            for (int i=0;i<course.getNrLectures();i++) {
                CttLecture lecture = course.getLecture(i);
                CttPlacement placement = assignment.getValue(lecture);
                if (placement==null)
                    w.println(course.getId());
                else
                    w.println(course.getId()+" "+placement.getRoom().getId()+" "+placement.getDay()+" "+placement.getSlot());
            }
        }
        w.flush();
        w.close();
        
        return true;
    }
    
    /**
     * CSV header, that is, rc for room capacity penalty, md for minimum working days penalty, cc for curriculum compactness penalty, and rs for room stability penalty.
     */
    public String csvHeader() { 
        return "rc,md,cc,rs"; 
    }
    
    /**
     * CSV line, values of this solution, i.e. , room capacity penalty, minimum working days penalty, curriculum compactness penalty, and room stability penalty.
     */
    public String csvLine(Assignment<CttLecture, CttPlacement> assignment) { 
        return 
            getRoomCapPenalty(assignment, false) + "," +
            getMinDaysPenalty(assignment, false) + "," +
            getCompactPenalty(assignment, false) + "," +
            getRoomPenalty(assignment, false);
    }

	@Override
	public AssignmentConstraintContext<CttLecture, CttPlacement> createAssignmentContext(Assignment<CttLecture, CttPlacement> assignment) {
		return new Penalties(assignment);
	}
	
	@Override
	public AssignmentConstraintContext<CttLecture, CttPlacement> inheritAssignmentContext(Assignment<CttLecture, CttPlacement> assignment,
			AssignmentConstraintContext<CttLecture, CttPlacement> parentContext) {
		return new Penalties(assignment, (Penalties)parentContext);
	}
	
    /**
     * Update penalty counters when a variable is unassigned 
     */
    public void afterUnassigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement placement) {
        super.afterUnassigned(assignment, iteration, placement);
        getPenalties(assignment).afterUnassigned(assignment, placement);
    }
    
    /**
     * Update penalty counters when a variable is assigned 
     */
    public void beforeAssigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement placement) {
        super.beforeAssigned(assignment, iteration, placement);
        getPenalties(assignment).beforeAssigned(assignment, placement);
    }

    private class Penalties implements AssignmentConstraintContext<CttLecture, CttPlacement> {
        private int iCompactPenalty = 0;
        private int iRoomPenalty = 0;
        private int iMinDaysPenalty = 0;
        private int iRoomCapPenalty = 0;
        
		public Penalties(Assignment<CttLecture, CttPlacement> assignment) {
			iCompactPenalty = CttModel.this.getCompactPenalty(assignment, true);
			iRoomPenalty = CttModel.this.getRoomPenalty(assignment, true);
			iMinDaysPenalty = CttModel.this.getMinDaysPenalty(assignment, true);
			iRoomCapPenalty = CttModel.this.getRoomCapPenalty(assignment, true);
		}
		
		public Penalties(Assignment<CttLecture, CttPlacement> assignment, Penalties parent) {
			iCompactPenalty = parent.getCompactPenalty();
			iRoomPenalty = parent.getRoomPenalty();
			iMinDaysPenalty = parent.getMinDaysPenalty();
			iRoomCapPenalty = parent.getRoomCapPenalty();
		}
		
		@Override
		public void assigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement placement) {}
		
		public void beforeAssigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement placement) {
	        CttLecture lecture = placement.variable();
	        iMinDaysPenalty += placement.getMinDaysPenalty(assignment);
	        iRoomPenalty += placement.getRoomPenalty(assignment);
	        iRoomCapPenalty += placement.getRoomCapPenalty();
	        for (CttCurricula curricula: lecture.getCourse().getCurriculas())
	            iCompactPenalty += curricula.getCompactPenalty(assignment, placement);
		}
		
		@Override
		public void unassigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement placement) {}
		
		public void afterUnassigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement placement) {
	        CttLecture lecture = placement.variable();
	        iRoomPenalty -= placement.getRoomPenalty(assignment);
	        iRoomCapPenalty -= placement.getRoomCapPenalty();
	        iMinDaysPenalty -= placement.getMinDaysPenalty(assignment);
	        for (CttCurricula curricula: lecture.getCourse().getCurriculas())
	        	iCompactPenalty -= curricula.getCompactPenalty(assignment, placement);
		}
		
		public int getCompactPenalty() { return iCompactPenalty; }
		
		public int getRoomPenalty() { return iRoomPenalty; }
		
		public int getMinDaysPenalty() { return iMinDaysPenalty; }
		
		public int getRoomCapPenalty() { return iRoomCapPenalty; }
    }

	@Override
	public Assignment<CttLecture, CttPlacement> createAssignment(int index, Assignment<CttLecture, CttPlacement> assignment) {
		return new CttAssignment(this, index, assignment);
	}

	@Override
    public InheritedAssignment<CttLecture, CttPlacement> createInheritedAssignment(Solution<CttLecture, CttPlacement> solution, int index) {
        return new CttInheritedAssignment(solution, index);
    }

}
