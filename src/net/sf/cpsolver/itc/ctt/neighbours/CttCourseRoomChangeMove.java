package net.sf.cpsolver.itc.ctt.neighbours;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.ctt.model.CttCourse;
import net.sf.cpsolver.itc.ctt.model.CttLecture;
import net.sf.cpsolver.itc.ctt.model.CttModel;
import net.sf.cpsolver.itc.ctt.model.CttPlacement;
import net.sf.cpsolver.itc.ctt.model.CttRoom;

/**
 * Try to find a move that decrease room stability penalty.
 * A course and a room is selected randomly. It tries to 
 * assign all lectures of the course into the selected room.
 * If there is already some other lecture in the room, 
 * it is reassinged to the room of the moving lecture.
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
public class CttCourseRoomChangeMove implements NeighbourSelection<CttLecture, CttPlacement> {
    /** Constructor */
    public CttCourseRoomChangeMove(DataProperties properties) {}
    /** Initialization */
    public void init(Solver<CttLecture, CttPlacement> solver) {}

    /** Neighbour selection */
    public Neighbour<CttLecture, CttPlacement> selectNeighbour(Solution<CttLecture, CttPlacement> solution) {
        CttModel model = (CttModel)solution.getModel();
        CttCourse course = (CttCourse)ToolBox.random(model.getCourses());
        CttRoom room = (CttRoom)ToolBox.random(model.getRooms());
        return new CttCourseRoomNeighbour(course, room);
    }
    
    /** 
     * Moves all lectures of the given class into the given room. 
     * If there is already some other lecture in the room, 
     * it is reassinged to the room of the moving lecture.
     */ 
    public static class CttCourseRoomNeighbour extends Neighbour<CttLecture, CttPlacement> {
        private CttCourse iCourse;
        private CttRoom iRoom;
        private double iValue;
        
        /** Constructor */
        public CttCourseRoomNeighbour(CttCourse course, CttRoom room) {
            iCourse = course; iRoom = room;
            iValue = -iCourse.getRoomPenalty();
            int nrStudents = course.getNrStudents();
            for (int i=0;i<iCourse.getNrLectures();i++) {
                CttLecture lecture = iCourse.getLecture(i);
                CttPlacement placement = (CttPlacement)lecture.getAssignment();
                if (placement==null) continue;
                CttPlacement conflict = iRoom.getConstraint().getPlacement(placement.getDay(), placement.getSlot());
                iValue += Math.max(0,nrStudents - iRoom.getSize());
                iValue -= Math.max(0,nrStudents - placement.getRoom().getSize());
                if (conflict!=null) {
                    CttLecture confLect = (CttLecture)conflict.variable();
                    iValue += Math.max(0,confLect.getCourse().getNrStudents() - placement.getRoom().getSize());
                    iValue -= Math.max(0,confLect.getCourse().getNrStudents() - iRoom.getSize());
                    iValue += conflict.getRoomPenalty(placement.getRoom());
                    iValue -= conflict.getRoomPenalty();
                }
            }
        }
        
        /** Make reassignments */
        public void assign(long iteration) {
            for (int i=0;i<iCourse.getNrLectures();i++) {
                CttLecture lecture = iCourse.getLecture(i);
                CttPlacement placement = (CttPlacement)lecture.getAssignment();
                if (placement==null) continue;
                CttPlacement conflict = iRoom.getConstraint().getPlacement(placement.getDay(), placement.getSlot());
                if (conflict!=null)
                    conflict.variable().assign(iteration,
                            new CttPlacement((CttLecture)conflict.variable(), 
                                    placement.getRoom(), placement.getDay(), placement.getSlot()));
                lecture.assign(iteration, 
                        new CttPlacement(lecture, 
                                iRoom, placement.getDay(), placement.getSlot()));
            }
        }
        
        /** Value of the move */
        public double value() {
            return iValue;
        }
        /** String representation */
        public String toString() {
            return "Course "+iCourse.getId()+" -> room "+iRoom.getId()+" (val:"+iValue+")";
        }
    }
}
