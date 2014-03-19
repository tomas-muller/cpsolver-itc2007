package net.sf.cpsolver.itc.exam.model;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.BinaryConstraint;
import org.cpsolver.ifs.model.Value;
import net.sf.cpsolver.itc.heuristics.search.ItcTabuSearch.TabuElement;

/**
 * Representation of a placement of an exam (value). That is a room and a period.
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
public class ExPlacement extends Value<ExExam, ExPlacement> implements TabuElement {
    private ExPeriod iPeriod;
    private ExRoom iRoom;
    private int iHashCode;
    
    /**
     * Constructor 
     * @param exam an exam
     * @param period assigned period 
     * @param room assigned room
     */
    public ExPlacement(ExExam exam, ExPeriod period, ExRoom room) {
        super(exam);
        iPeriod = period;
        iRoom = room;
        ExModel m = (ExModel)exam.getModel();
        iHashCode = (int)room.getId()*m.getNrPeriods()+period.getIndex();
    }
    
    /** Assigned period */
    public ExPeriod getPeriod() {
        return iPeriod;
    }
    
    /** Assigned room */
    public ExRoom getRoom() {
        return iRoom;
    }
    
    /** Number of students */
    public int getNrStudents() {
        return ((ExExam)variable()).getStudents().size();
    }
    
    /** Assignment name (period index, room name)*/
    public String getName() {
        return getPeriodIndex()+","+getRoom().getName();
    }
    
    /** String representation */
    public String toString(Assignment<ExExam, ExPlacement> assignment) {
        return variable().getName()+"="+getName()+" ("+toInt(assignment)+")";
    }
    
    /** Period penalty */
    public int getPeriodPenalty() {
        return getPeriod().getWeight();
    }
    
    /** Two exams in a row penalty */
    public int twoInARowPenalty(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        ExPeriod prevPeriod = getPeriod().prev();
        ExPeriod nextPeriod = getPeriod().next();
        boolean prev = prevPeriod!=null && getPeriod().getDay()==prevPeriod.getDay();
        boolean next = nextPeriod!=null && getPeriod().getDay()==nextPeriod.getDay(); 
        if (!prev && !next) return 0;
        int penalty = 0;
        for (ExStudent student: variable().getStudents()) {
            if (m.areDirectConflictsAllowed() && student.nrExams(assignment, getPeriod(), exam)>0) continue;
            if (next && student.hasExam(assignment, nextPeriod,exam)) penalty++;
            if (prev && student.hasExam(assignment, prevPeriod,exam)) penalty++;
        }
        return penalty;
    }
    
    /** Number of direct conflicts */
    public int nrDirectConflicts(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        if (!m.areDirectConflictsAllowed()) return 0;
        int conflicts = 0;
        for (ExStudent student: variable().getStudents())
            if (student.nrExams(assignment, getPeriod(), exam)>0) conflicts++;
        return conflicts;
    }

    /** Two exams in a day penalty */
    public int twoInADayPenalty(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        ExPeriod prevPeriod = (getPeriod().prev()==null?null:getPeriod().prev().prev());
        ExPeriod nextPeriod = (getPeriod().next()==null?null:getPeriod().next().next());
        boolean prev = prevPeriod!=null && getPeriod().getDay()==prevPeriod.getDay();
        boolean next = nextPeriod!=null && getPeriod().getDay()==nextPeriod.getDay(); 
        if (!prev && !next) return 0;
        int penalty = 0;
        for (ExStudent student: variable().getStudents()) {
            if (m.areDirectConflictsAllowed() && student.nrExams(assignment, getPeriod(), exam)>0) continue;
            if (prev) 
                for (ExPeriod p=prevPeriod;p!=null && p.getDay()==getPeriod().getDay();p=p.prev())
                    if (student.hasExam(assignment, p,exam)) penalty++;
            if (next)
                for (ExPeriod p=nextPeriod;p!=null && p.getDay()==getPeriod().getDay();p=p.next())
                    if (student.hasExam(assignment, p,exam)) penalty++; 
        }
        return penalty;
    }
    
    /** Period spread penalty */
    public int widerSpreadPenalty(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        int penalty = 0;
        for (ExStudent student: variable().getStudents()) {
            if (m.areDirectConflictsAllowed() && student.nrExams(assignment, getPeriod(), exam)>0) continue;
            for (ExPeriod p=getPeriod().next();p!=null && p.getIndex()-getPeriod().getIndex()<=m.getPeriodSpreadLength(); p=p.next())
                if (student.hasExam(assignment, p,exam)) penalty++;
            for (ExPeriod p=getPeriod().prev();p!=null && getPeriod().getIndex()-p.getIndex()<=m.getPeriodSpreadLength(); p=p.prev())
                if (student.hasExam(assignment, p,exam)) penalty++;
        }
        return penalty;
    }
    
    private int combinedPenalty(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        ExPeriod prevPeriod = getPeriod().prev();
        ExPeriod nextPeriod = getPeriod().next();
        boolean prev = prevPeriod!=null && getPeriod().getDay()==prevPeriod.getDay();
        boolean next = nextPeriod!=null && getPeriod().getDay()==nextPeriod.getDay(); 
        ExPeriod prevPeriod2 = (prevPeriod==null?null:prevPeriod.prev());
        ExPeriod nextPeriod2 = (nextPeriod==null?null:nextPeriod.next());
        boolean prev2 = prevPeriod2!=null && getPeriod().getDay()==prevPeriod2.getDay();
        boolean next2 = nextPeriod2!=null && getPeriod().getDay()==nextPeriod2.getDay(); 
        int penalty = 0;
        for (ExStudent student: variable().getStudents()) {

            if (m.areDirectConflictsAllowed()) {
                Set<ExExam> exams = student.getExams(assignment, getPeriod());
                int nrExams = exams.size() + (exams.contains(exam)?0:1);
                if (nrExams>1) {
                    penalty+=m.getDirectConflictWeight();
                    continue;
                }
            }

            for (ExPeriod p=getPeriod().next();p!=null && p.getIndex()-getPeriod().getIndex()<=m.getPeriodSpreadLength(); p=p.next())
                if (student.hasExam(assignment, p,exam)) penalty++;
            for (ExPeriod p=getPeriod().prev();p!=null && getPeriod().getIndex()-p.getIndex()<=m.getPeriodSpreadLength(); p=p.prev())
                if (student.hasExam(assignment, p,exam)) penalty++;
            
            if (prev && student.hasExam(assignment, prevPeriod,exam)) penalty+=m.getTwoInARowWeight();
            if (next && student.hasExam(assignment, nextPeriod,exam)) penalty+=m.getTwoInARowWeight();

            if (prev2) 
                for (ExPeriod p=prevPeriod2;p!=null && p.getDay()==getPeriod().getDay();p=p.prev())
                    if (student.hasExam(assignment, p,exam)) penalty+=m.getTwoInADayWeight(); 
            if (next2)
                for (ExPeriod p=nextPeriod2;p!=null && p.getDay()==getPeriod().getDay();p=p.next())
                    if (student.hasExam(assignment, p,exam)) penalty+=m.getTwoInADayWeight();
        }
        return penalty;
    }
    
    /** Mixed durations penalty */
    public int mixedDurationsPenalty(Assignment<ExExam, ExPlacement> assignment) {
        int length = ((ExExam)variable()).getLength();
        int sameLength = 1, diffLength = 0;
        ExExam exam = (ExExam)variable();
        for (ExPlacement p: getRoom().getExams(assignment, getPeriod())) {
            if (exam.equals(p.variable())) continue;
            if (p.variable().getLength()!=length) diffLength++; else sameLength++;
        }
        return (diffLength>0 && sameLength==1?1:0);
    }
    
    /** Front load penalty */
    public int frontLoadPenalty() {
        ExExam exam = (ExExam)variable();
        ExModel m = (ExModel)exam.getModel();
        if (exam.isLargest() && getPeriodIndex()>=m.getFronLoadThreshold()) return 1;
        return 0;
    }
    
    /** Number of violated binary constraints */ 
    public int nrBinaryViolations(Assignment<ExExam, ExPlacement> assignment) {
        ExExam exam = (ExExam)variable();
        int viol = 0;
        for (BinaryConstraint<ExExam, ExPlacement> bc: exam.binaryConstraints()) {
            if (bc.isHard()) continue;
            if (bc.first().equals(exam)) {
                ExPlacement another = assignment.getValue(bc.second());
                if (another!=null && !bc.isConsistent(this, another)) viol++;
            } else {
                ExPlacement another = assignment.getValue(bc.first());
                if (another!=null && !bc.isConsistent(another, this)) viol++;
            }
        }
        return viol;
    }
    
    /** Weighted sum of all criteria (violated soft constraints) */ 
    public int toInt(Assignment<ExExam, ExPlacement> assignment) {
        ExModel m = (ExModel)variable().getModel();
        return 
            m.getBinaryViolationWeight()*nrBinaryViolations(assignment)+
            combinedPenalty(assignment)+
            /*
            m.getDirectConflictWeight()*nrDirectConflicts()+
            m.getTwoInARowWeight()*twoInARowPenalty()+
            m.getTwoInADayWeight()*twoInADayPenalty()+
            widerSpreadPenalty()+*/
            m.getMixedDurationWeight()*mixedDurationsPenalty(assignment)+
            m.getFrontLoadWeight()*frontLoadPenalty()+
            getRoom().getPenalty()+
            getPeriodPenalty();
    }
    
    /** Weighted sum of all time-related criteria (violated soft constraints) */
    public double getTimeCost(Assignment<ExExam, ExPlacement> assignment) {
        ExModel m = (ExModel)variable().getModel();
        return 
            m.getBinaryViolationWeight()*nrBinaryViolations(assignment)+
            combinedPenalty(assignment)+
            m.getFrontLoadWeight()*frontLoadPenalty()+
            getPeriodPenalty();
    }
    
    /** Weighted sum of all room-related criteria (violated soft constraints) */
    public double getRoomCost(Assignment<ExExam, ExPlacement> assignment) {
        ExModel m = (ExModel)variable().getModel();
        return 
            m.getMixedDurationWeight()*mixedDurationsPenalty(assignment)+
            getRoom().getPenalty();
    }

    /** Weighted sum of all criteria (violated soft constraints) */
    public double toDouble(Assignment<ExExam, ExPlacement> assignment) {
        return toInt(assignment); 
    }
    

    /** Index of the assigned period */
    public int getPeriodIndex() {
        return getPeriod().getIndex();
    }
    
    /** Tabu element of this placement (variable id and period index -- room assignment is ignored). */
    public Object tabuElement() {
        return variable().getId()+":"+getPeriodIndex();
    }

    /** Compare two placements for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExPlacement)) return false;
        ExPlacement p = (ExPlacement)o;
        return 
            variable().getId()==p.variable().getId() && 
            getRoom().getId()==p.getRoom().getId() && 
            getPeriod().getIndex()==p.getPeriod().getIndex();
    }
    
    /** Hash code */
    public int hashCode() {
        return iHashCode;
    }
}
