package net.sf.cpsolver.itc.exam.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.BinaryConstraint;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.LazySwap;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.util.ToolBox;

import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Representation of an exam (variable).
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
public class ExExam extends Variable<ExExam, ExPlacement> implements Swapable<ExExam, ExPlacement> {
    private static Logger sLog = Logger.getLogger(ExExam.class);
    private List<ExStudent> iStudents = new ArrayList<ExStudent>();
    private int iLength = 0;
    private boolean iRoomExclusive = false;
    private boolean iIsLargest = false;
    private Set<ExExam> iSamePeriodExams = new HashSet<ExExam>();
    private List<BinaryConstraint<ExExam, ExPlacement>> iBinaryConstraints = new ArrayList<BinaryConstraint<ExExam,ExPlacement>>();
    
    /**
     * Constructor
     * @param id unique identifier
     * @param length length of an exam in minutes 
     */
    public ExExam(int id, int length) {
        super();
        iId = id;
        iLength = length;
    }
    
    /** Return length of an exam in minutes */
    public int getLength() {
        return iLength;
    }
    
    /** Students attending this exam */
    public List<ExStudent> getStudents() {
        return iStudents;
    }
    
    /** Exam initialization -- compute exam's domain */
    public void init() {
        ExModel m = (ExModel)getModel();
        List<ExRoom> rooms = new ArrayList<ExRoom>(m.getRooms());
        Collections.shuffle(rooms, ToolBox.getRandom());
        List<ExPlacement> values = new ArrayList<ExPlacement>();
        for (ExRoom room: rooms) {
            if (room.getSize()<getStudents().size()) continue;
            room.addVariable(this);
            for (ExPeriod p=m.firstPeriod(); p!=null; p=p.next()) {
                if (p.getLength()<getLength()) continue;
                values.add(new ExPlacement(this, p, room));
            }
        }
        
        if (sLog.isDebugEnabled())
            sLog.debug("Values for "+this+" are "+ToolBox.col2string(values, 2));
        
        setValues(values);
    }
    
    /** A constraint is added -- memorize binary constraints */
    public void addContstraint(Constraint<ExExam, ExPlacement> constraint) {
        if (constraint instanceof BinaryConstraint)
            iBinaryConstraints.add((BinaryConstraint<ExExam, ExPlacement>)constraint);
        super.addContstraint(constraint);
    }
    
    /** A constraint is removed -- memorize binary constraints */
    public void removeContstraint(Constraint<ExExam, ExPlacement> constraint) {
        if (constraint instanceof BinaryConstraint)
            iBinaryConstraints.remove((BinaryConstraint<ExExam, ExPlacement>)constraint);
        super.removeContstraint(constraint);
    }
    
    /** List of binary constraints */
    public List<BinaryConstraint<ExExam, ExPlacement>> binaryConstraints() {
        return iBinaryConstraints;
    }

    /** True if the exam is room exclusive (i.e., it cannot share a room with another exam) */
    public boolean isRoomExclusive() {
        return iRoomExclusive;
    }
    
    /** Set whether the exam is room exclusive (i.e., it cannot share a room with another exam) */
    public void setRoomExclusive(boolean rx) {
        iRoomExclusive = rx;
    }
    
    /** True if the exam is in the given number of largest exams */
    public boolean isLargest() {
        return iIsLargest;
    }
    
    /** Set whether the exam is in the given number of largest exams */
    public void setIsLargest(boolean isLargest) {
        iIsLargest = isLargest;
    }

    /** Exams requiring same period */
    public Set<ExExam> getSamePeriodExams() {
        return iSamePeriodExams;
    }
    
    /** True if the given exam is requiring the same period as this exam*/
    public boolean isSamePeriodExam(ExExam exam) {
        return iSamePeriodExams.contains(exam);
    }
    
    private Set<ExExam> iCorrelatedExams = null;
    /** Number of corelated exams (considering students) */
    public synchronized int nrCorelatedExams() {
        if (iCorrelatedExams!=null) return iCorrelatedExams.size();
        iCorrelatedExams = new HashSet<ExExam>();
        for (ExStudent student: getStudents())
            iCorrelatedExams.addAll(student.variables());
        iCorrelatedExams.remove(this);
        return iCorrelatedExams.size();
    }


    /** Names of exams requiring same period as this exam */
    private String getSamePeriodNames() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<ExExam> i=getSamePeriodExams().iterator();i.hasNext();) {
            ExExam ex = i.next();
            sb.append(ex.getName());
            if (i.hasNext()) sb.append(",");
        }
        return "["+sb.toString()+"]";
    }
    
    /** Exam name (E id) */
    public String getName() {
        return "E"+getId();
    }
    
    /** String representation */
    public String toString() {
        return "E"+getId()+" [len="+getLength()+",#st="+getStudents().size()+(isRoomExclusive()?",ex":"")+(isLargest()?",lg":"")+(iSamePeriodExams.isEmpty()?"":",sp="+getSamePeriodNames())+"]";
    }
    
    /** Compare two exams on the number of corelated exams and on domain size vs. number of constraints ratio */
    public int compareTo(ExExam x) {
        int cmp = -Double.compare(nrCorelatedExams(), x.nrCorelatedExams());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)values().size())/constraints().size(), ((double)x.values().size())/x.constraints().size());
        if (cmp!=0) return cmp;
        
        return super.compareTo(x);
    }
    
    /** Find a non-conflicting placement (room) in the given period */
    public ExPlacement findPlacement(Assignment<ExExam, ExPlacement> assignment, ExPeriod period) {
        ExModel model = (ExModel)getModel();
        ExPlacement bestPlacement = null;
        int rx = ToolBox.random(model.getRooms().size());
        for (int r=0;r<model.getRooms().size();r++) {
            ExRoom room = (ExRoom)model.getRooms().get((r+rx)%model.getRooms().size());
            if (room.getSize()<getStudents().size()) continue;
            ExPlacement placement = new ExPlacement(this, period, room);
            if (room.inConflict(assignment, placement)) continue;
            if (bestPlacement==null || bestPlacement.getRoomCost(assignment)>placement.getRoomCost(assignment))
                bestPlacement = placement;
        }
        return bestPlacement;
    }

    /** Find a swap with another exam */
    public Neighbour<ExExam, ExPlacement> findSwap(Assignment<ExExam, ExPlacement> assignment, ExExam another) {
        ExModel model = (ExModel)getModel();
        ExExam ex1 = this;
        ExExam ex2 = another;
        ExPlacement p1 = assignment.getValue(ex1);
        ExPlacement p2 = assignment.getValue(ex2);
        if (p1==null || p2==null) return null;
        if (ex1.getLength()>p2.getPeriod().getLength() || ex2.getLength()>p1.getPeriod().getLength()) return null;
        if (model.areDirectConflictsAllowed() && p1.getPeriodIndex()!=p2.getPeriodIndex()) {
            ExPlacement np1 = ex1.findPlacement(assignment, p2.getPeriod());
            if (np1==null) return null;
            ExPlacement np2 = ex2.findPlacement(assignment, p1.getPeriod());
            if (np2==null) return null;
            if (!model.areBinaryViolationsAllowed()) {
                for (BinaryConstraint<ExExam, ExPlacement> constraint: ex1.binaryConstraints()) {
                    if (constraint.another(ex1).equals(ex2) && !constraint.isConsistent(np1, np2)) return null;
                    if (constraint.inConflict(assignment, np1)) return null;
                }
                for (BinaryConstraint<ExExam, ExPlacement> constraint: ex2.binaryConstraints()) {
                    if (constraint.another(ex2).equals(ex1)) continue;
                    if (constraint.inConflict(assignment, np2)) return null;
                }
            }
            return new LazySwap<ExExam, ExPlacement>(np1, np2); 
        }
        ExRoom r1 = p1.getRoom();
        ExRoom r2 = p2.getRoom();
        if (r2.getAvailableSpace(assignment, p2.getPeriod())+ex2.getStudents().size()<ex1.getStudents().size()) return null;
        if (r1.getAvailableSpace(assignment, p1.getPeriod())+ex1.getStudents().size()<ex2.getStudents().size()) return null;
        if (ex1.isRoomExclusive() && r2.getExams(assignment, p2.getPeriod()).size()>1) return null;
        if (ex2.isRoomExclusive() && r1.getExams(assignment, p1.getPeriod()).size()>1) return null;
        if (!model.areDirectConflictsAllowed() && p1.getPeriodIndex()!=p2.getPeriodIndex()) {
            for (ExStudent s: ex1.getStudents()) {
                if (s.getContext(assignment).hasExam(p2.getPeriodIndex(), ex2)) return null;
            }
            for (ExStudent s: ex2.getStudents()) {
                if (s.getContext(assignment).hasExam(p1.getPeriodIndex(), ex1)) return null;
            }
        }
        ExPlacement np1 = new ExPlacement(ex1, p2.getPeriod(), p2.getRoom());
        ExPlacement np2 = new ExPlacement(ex2, p1.getPeriod(), p1.getRoom());
        if (!model.areBinaryViolationsAllowed()) {
            for (BinaryConstraint<ExExam, ExPlacement> constraint: ex1.binaryConstraints()) {
                if (constraint.another(ex1).equals(ex2) && !constraint.isConsistent(np1, np2)) return null;
                if (constraint.inConflict(assignment, np1)) return null;
            }
            for (BinaryConstraint<ExExam, ExPlacement> constraint: ex2.binaryConstraints()) {
                if (constraint.another(ex2).equals(ex1)) continue;
                if (constraint.inConflict(assignment, np2)) return null;
            }
        }
        return new LazySwap<ExExam, ExPlacement>(np1, np2); 
    }
    
    /** Compare two exams for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExExam)) return false;
        ExExam exam = (ExExam)o;
        return getId()==exam.getId();
    }

}
