package net.sf.cpsolver.itc.exam.model;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Representation of an exam (variable).
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
public class ExExam extends Variable implements Swapable {
    private static Logger sLog = Logger.getLogger(ExExam.class);
    private Vector iStudents = new Vector();
    private int iLength = 0;
    private boolean iRoomExclusive = false;
    private boolean iIsLargest = false;
    private HashSet iSamePeriodExams = new HashSet();
    private Vector iBinaryConstraints = new Vector();
    
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
    public Vector getStudents() {
        return iStudents;
    }
    
    /** Exam initialization -- compute exam's domain */
    public void init() {
        ExModel m = (ExModel)getModel();
        Vector rooms = new Vector(m.getRooms());
        Collections.shuffle(rooms, ToolBox.getRandom());
        Vector values = new Vector();
        for (Enumeration e=rooms.elements();e.hasMoreElements();) {
            ExRoom room = (ExRoom)e.nextElement();
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
    public void addContstraint(Constraint constraint) {
        if (constraint instanceof BinaryConstraint)
            iBinaryConstraints.add(constraint);
        super.addContstraint(constraint);
    }
    
    /** A constraint is removed -- memorize binary constraints */
    public void removeContstraint(Constraint constraint) {
        if (constraint instanceof BinaryConstraint)
            iBinaryConstraints.remove(constraint);
        super.removeContstraint(constraint);
    }
    
    /** List of binary constraints */
    public Vector binaryConstraints() {
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
    public Set getSamePeriodExams() {
        return iSamePeriodExams;
    }
    
    /** True if the given exam is requiring the same period as this exam*/
    public boolean isSamePeriodExam(ExExam exam) {
        return iSamePeriodExams.contains(exam);
    }
    
    private HashSet iCorrelatedExams = null;
    /** Number of corelated exams (considering students) */
    public int nrCorelatedExams() {
        if (iCorrelatedExams!=null) return iCorrelatedExams.size();
        iCorrelatedExams = new HashSet();
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExStudent student = (ExStudent)e.nextElement();
            iCorrelatedExams.addAll(student.variables());
        }
        iCorrelatedExams.remove(this);
        return iCorrelatedExams.size();
    }


    /** Names of exams requiring same period as this exam */
    private String getSamePeriodNames() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=getSamePeriodExams().iterator();i.hasNext();) {
            ExExam ex = (ExExam)i.next();
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
    public int compareTo(Object o) {
        ExExam x = (ExExam)o;
        int cmp;
        
        cmp = -Double.compare(nrCorelatedExams(), x.nrCorelatedExams());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)values().size())/constraints().size(), ((double)x.values().size())/x.constraints().size());
        if (cmp!=0) return cmp;
        
        return super.compareTo(o);
    }
    
    /** Find a non-conflicting placement (room) in the given period */
    public ExPlacement findPlacement(ExPeriod period) {
        ExModel model = (ExModel)getModel();
        ExPlacement bestPlacement = null;
        int rx = ToolBox.random(model.getRooms().size());
        for (int r=0;r<model.getRooms().size();r++) {
            ExRoom room = (ExRoom)model.getRooms().elementAt((r+rx)%model.getRooms().size());
            if (room.getSize()<getStudents().size()) continue;
            ExPlacement placement = new ExPlacement(this, period, room);
            HashSet confs = new HashSet();
            if (room.inConflict(placement)) continue;
            if (bestPlacement==null || bestPlacement.getRoomCost()>placement.getRoomCost())
                bestPlacement = placement;
        }
        return bestPlacement;
    }

    /** Find a swap with another exam */
    public Neighbour findSwap(Variable another) {
        ExModel model = (ExModel)getModel();
        ExExam ex1 = this;
        ExExam ex2 = (ExExam)another;
        ExPlacement p1 = (ExPlacement)ex1.getAssignment();
        ExPlacement p2 = (ExPlacement)ex2.getAssignment();
        if (p1==null || p2==null) return null;
        if (ex1.getLength()>p2.getPeriod().getLength() || ex2.getLength()>p1.getPeriod().getLength()) return null;
        if (model.areDirectConflictsAllowed() && p1.getPeriodIndex()!=p2.getPeriodIndex()) {
            ExPlacement np1 = ex1.findPlacement(p2.getPeriod());
            if (np1==null) return null;
            ExPlacement np2 = ex2.findPlacement(p1.getPeriod());
            if (np2==null) return null;
            if (!model.areBinaryViolationsAllowed()) {
                for (Enumeration e=ex1.binaryConstraints().elements();e.hasMoreElements();) {
                    BinaryConstraint constraint = (BinaryConstraint)e.nextElement();
                    if (constraint.another(ex1).equals(ex2) && !constraint.isConsistent(np1, np2)) return null;
                    if (constraint.inConflict(np1)) return null;
                }
                for (Enumeration e=ex2.binaryConstraints().elements();e.hasMoreElements();) {
                    BinaryConstraint constraint = (BinaryConstraint)e.nextElement();
                    if (constraint.another(ex2).equals(ex1)) continue;
                    if (constraint.inConflict(np2)) return null;
                }
            }
            return new ItcLazySwap(np1, np2); 
        }
        ExRoom r1 = p1.getRoom();
        ExRoom r2 = p2.getRoom();
        if (r2.getAvailableSpace(p2.getPeriod())+ex2.getStudents().size()<ex1.getStudents().size()) return null;
        if (r1.getAvailableSpace(p1.getPeriod())+ex1.getStudents().size()<ex2.getStudents().size()) return null;
        if (ex1.isRoomExclusive() && r2.getExams(p2.getPeriod()).size()>1) return null;
        if (ex2.isRoomExclusive() && r1.getExams(p1.getPeriod()).size()>1) return null;
        if (!model.areDirectConflictsAllowed() && p1.getPeriodIndex()!=p2.getPeriodIndex()) {
            for (Enumeration e=ex1.getStudents().elements();e.hasMoreElements();) {
                ExStudent s = (ExStudent)e.nextElement();
                if (s.hasExam(p2.getPeriod(),ex2)) return null;
            }
            for (Enumeration e=ex2.getStudents().elements();e.hasMoreElements();) {
                ExStudent s = (ExStudent)e.nextElement();
                if (s.hasExam(p1.getPeriod(),ex1)) return null;
            }
        }
        ExPlacement np1 = new ExPlacement(ex1, p2.getPeriod(), p2.getRoom());
        ExPlacement np2 = new ExPlacement(ex2, p1.getPeriod(), p1.getRoom());
        if (!model.areBinaryViolationsAllowed()) {
            for (Enumeration e=ex1.binaryConstraints().elements();e.hasMoreElements();) {
                BinaryConstraint constraint = (BinaryConstraint)e.nextElement();
                if (constraint.another(ex1).equals(ex2) && !constraint.isConsistent(np1, np2)) return null;
                if (constraint.inConflict(np1)) return null;
            }
            for (Enumeration e=ex2.binaryConstraints().elements();e.hasMoreElements();) {
                BinaryConstraint constraint = (BinaryConstraint)e.nextElement();
                if (constraint.another(ex2).equals(ex1)) continue;
                if (constraint.inConflict(np2)) return null;
            }
        }
        return new ItcLazySwap(np1, np2); 
    }
    
    /** Compare two exams for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExExam)) return false;
        ExExam exam = (ExExam)o;
        return getId()==exam.getId();
    }
    
    /** A placement was assigned to this exam -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void assign(long iteration, Value value) {
        getModel().beforeAssigned(iteration,value);
        if (iValue!=null) unassign(iteration);
        if (value==null) return;
        iValue = value;
        ExPlacement placement = (ExPlacement)iValue;
        placement.getRoom().assigned(iteration, value);
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExStudent student = (ExStudent)e.nextElement();
            student.assigned(iteration, value);
        }
        if (!((ExModel)getModel()).areBinaryViolationsAllowed()) {
            for (Enumeration e=iBinaryConstraints.elements();e.hasMoreElements();) {
                BinaryConstraint bc = (BinaryConstraint)e.nextElement();
                bc.assigned(iteration, value);
            }
        }
        value.assigned(iteration);
        getModel().afterAssigned(iteration,value);
    }
    
    /** A placement was unassigned from this exam -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void unassign(long iteration) {
        if (iValue==null) return;
        getModel().beforeUnassigned(iteration,iValue);
        Value oldValue = iValue;
        iValue = null;
        ExPlacement placement = (ExPlacement)oldValue;
        placement.getRoom().unassigned(iteration, oldValue);
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            ExStudent student = (ExStudent)e.nextElement();
            student.unassigned(iteration, oldValue);
        }
        if (!((ExModel)getModel()).areBinaryViolationsAllowed()) {
            for (Enumeration e=iBinaryConstraints.elements();e.hasMoreElements();) {
                BinaryConstraint bc = (BinaryConstraint)e.nextElement();
                bc.unassigned(iteration, oldValue);
            }
        }
        getModel().afterUnassigned(iteration,oldValue);
    }
    
}
