package net.sf.cpsolver.itc.ctt.neighbours;

import java.util.Enumeration;
import java.util.HashSet;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
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
public class CttCourseMinDaysMove implements NeighbourSelection, HillClimberSelection {
    private static Logger sLog = Logger.getLogger(CttCourseMinDaysMove.class);
    private boolean iHC = false;

    /** Constructor */
    public CttCourseMinDaysMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver solver) {}
    /** Set hill-climber mode (worsening moves are skipped) */
    public void setHcMode(boolean hcMode) { iHC = hcMode; }

    /** Neighbour selection */
    public Neighbour selectNeighbour(Solution solution) {
        CttModel model = (CttModel)solution.getModel();
        if (model.getMinDaysPenalty(false)<=0) return null;
        int cx = ToolBox.random(model.getCourses().size());
        for (int c=0;c<model.getCourses().size();c++) {
            CttCourse course = (CttCourse)model.getCourses().elementAt((c+cx)%model.getCourses().size());
            int days = 0, nrDays = 0;
            HashSet adepts = new HashSet();
            for (int i=0;i<course.getNrLectures();i++) {
                if (course.getLecture(i).getAssignment()==null) {
                    nrDays++;
                } else {
                    int d = (((CttPlacement)course.getLecture(i).getAssignment()).getDay());
                    int day = 1 << d;
                    if ((days & day) == 0) nrDays ++;
                    else adepts.add(new Integer(d));
                    days |= day;
                }
            }
            if (nrDays>=course.getMinDays()) continue;
            return findNeighbour(course,days,adepts);
        }
        return null;
    }
    
    private Neighbour findNeighbour(CttCourse course, int days, HashSet adepts) {
        int lx = ToolBox.random(course.getNrLectures());
        for (int l=0;l<course.getNrLectures();l++) {
            CttLecture lecture = (CttLecture)course.getLecture((l+lx)%course.getNrLectures());
            CttPlacement placement = (CttPlacement)lecture.getAssignment();
            if (placement==null || !adepts.contains(new Integer(placement.getDay()))) continue;
            int minDayPenalty = placement.getMinDaysPenalty();
            int slot = placement.getSlot();
            CttRoom originalRoom = placement.getRoom();
            int dx = ToolBox.random(course.getModel().getNrDays());
            day: for (int d=0;d<course.getModel().getNrDays();d++) {
                int day = (d+dx)%course.getModel().getNrDays();
                if ((days & (1<<day))!=0) continue;
                if (!lecture.getCourse().isAvailable(day,slot)) continue;
                int minDayChange = placement.getMinDaysPenalty(day) - minDayPenalty;
                if (minDayChange>=0) continue;
                if (lecture.getCourse().getTeacher().getConstraint().getPlacement(day,slot)!=null) continue;
                for (Enumeration e=lecture.getCourse().getCurriculas().elements();e.hasMoreElements();) {
                    CttCurricula curricula = (CttCurricula)e.nextElement();
                    if (curricula.getConstraint().getPlacement(day,slot)!=null) continue day;
                }
                if ((CttPlacement)originalRoom.getConstraint().getPlacement(day, slot)==null) {
                    ItcSimpleNeighbour n = new ItcSimpleNeighbour(lecture, new CttPlacement(lecture, originalRoom, day, slot));
                    if (!iHC || n.value()<=0) return n;
                }
                int rx = ToolBox.random(course.getModel().getRooms().size());
                for (int r=0;r<course.getModel().getRooms().size();r++) {
                    CttRoom room = (CttRoom)course.getModel().getRooms().elementAt((r+rx)%course.getModel().getRooms().size());
                    if (room.getSize()<course.getNrStudents()) continue;
                    if ((CttPlacement)room.getConstraint().getPlacement(day, slot)==null) { 
                        ItcSimpleNeighbour n = new ItcSimpleNeighbour(lecture, new CttPlacement(lecture, room, day, slot));
                        if (!iHC || n.value()<=0) return n;
                    }
                }
            }
        }
        return null;
    }
}
