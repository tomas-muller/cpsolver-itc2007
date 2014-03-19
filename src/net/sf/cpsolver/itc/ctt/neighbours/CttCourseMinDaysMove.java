package net.sf.cpsolver.itc.ctt.neighbours;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ctt.model.CttCourse;
import net.sf.cpsolver.itc.ctt.model.CttCurricula;
import net.sf.cpsolver.itc.ctt.model.CttLecture;
import net.sf.cpsolver.itc.ctt.model.CttModel;
import net.sf.cpsolver.itc.ctt.model.CttPlacement;
import net.sf.cpsolver.itc.ctt.model.CttRoom;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSimpleNeighbour;
import net.sf.cpsolver.itc.heuristics.search.ItcHillClimber.HillClimberSelection;

/**
 * Try to find a move that decrease course minimum days penalty.
 * A course with positive penalty is selected randomly, a day
 * on which two or more lectures are taught is selected and one of 
 * lectures of that day are moved to a day that is not being taught on.
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
public class CttCourseMinDaysMove implements NeighbourSelection<CttLecture, CttPlacement>, HillClimberSelection {
    private boolean iHC = false;

    /** Constructor */
    public CttCourseMinDaysMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<CttLecture, CttPlacement> solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }

    /** Neighbour selection */
    public Neighbour<CttLecture, CttPlacement> selectNeighbour(Solution<CttLecture, CttPlacement> solution) {
        CttModel model = (CttModel)solution.getModel();
        Assignment<CttLecture, CttPlacement> assignment = solution.getAssignment();
        if (model.getMinDaysPenalty(assignment, false)<=0) return null;
        int cx = ToolBox.random(model.getCourses().size());
        for (int c=0;c<model.getCourses().size();c++) {
            CttCourse course = model.getCourses().get((c+cx)%model.getCourses().size());
            int days = 0, nrDays = 0;
            Set<Integer> adepts = new HashSet<Integer>();
            for (int i=0;i<course.getNrLectures();i++) {
            	CttPlacement p = assignment.getValue(course.getLecture(i));
                if (p == null) {
                    nrDays++;
                } else {
                    int day = 1 << p.getDay();
                    if ((days & day) == 0) nrDays ++;
                    else adepts.add(new Integer(p.getDay()));
                    days |= day;
                }
            }
            if (nrDays>=course.getMinDays()) continue;
            return findNeighbour(assignment,course,days,adepts);
        }
        return null;
    }
    
    private Neighbour<CttLecture, CttPlacement> findNeighbour(Assignment<CttLecture, CttPlacement> assignment, CttCourse course, int days, Set<Integer> adepts) {
        int lx = ToolBox.random(course.getNrLectures());
        for (int l=0;l<course.getNrLectures();l++) {
            CttLecture lecture = course.getLecture((l+lx)%course.getNrLectures());
            CttPlacement placement = assignment.getValue(lecture);
            if (placement==null || !adepts.contains(new Integer(placement.getDay()))) continue;
            int minDayPenalty = placement.getMinDaysPenalty(assignment);
            int slot = placement.getSlot();
            CttRoom originalRoom = placement.getRoom();
            int dx = ToolBox.random(course.getModel().getNrDays());
            day: for (int d=0;d<course.getModel().getNrDays();d++) {
                int day = (d+dx)%course.getModel().getNrDays();
                if ((days & (1<<day))!=0) continue;
                if (!lecture.getCourse().isAvailable(day,slot)) continue;
                int minDayChange = placement.getMinDaysPenalty(assignment, day) - minDayPenalty;
                if (minDayChange>=0) continue;
                if (lecture.getCourse().getTeacher().getConstraint().getPlacement(assignment, day,slot)!=null) continue;
                for (CttCurricula curricula: lecture.getCourse().getCurriculas()) {
                    if (curricula.getConstraint().getPlacement(assignment, day,slot)!=null) continue day;
                }
                if (originalRoom.getConstraint().getPlacement(assignment, day, slot)==null) {
                    ItcSimpleNeighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(assignment, lecture, new CttPlacement(lecture, originalRoom, day, slot));
                    if (!iHC || n.value(assignment)<=0) return n;
                }
                int rx = ToolBox.random(course.getModel().getRooms().size());
                for (int r=0;r<course.getModel().getRooms().size();r++) {
                    CttRoom room = course.getModel().getRooms().get((r+rx)%course.getModel().getRooms().size());
                    if (room.getSize()<course.getNrStudents()) continue;
                    if (room.getConstraint().getPlacement(assignment, day, slot)==null) { 
                        ItcSimpleNeighbour<CttLecture, CttPlacement> n = new ItcSimpleNeighbour<CttLecture, CttPlacement>(assignment, lecture, new CttPlacement(lecture, room, day, slot));
                        if (!iHC || n.value(assignment)<=0) return n;
                    }
                }
            }
        }
        return null;
    }
}
