package net.sf.cpsolver.itc.ctt.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a teacher. Two lectures that are associated with the same 
 * teacher cannot be placed at the same day and time.
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
public class CttTeacher {
    private CttModel iModel;
    private String iId;
    private TeacherConstraint iConstraint;
    
    /** 
     * Constructor
     * @param model problem model
     * @param id unique identifier
     */
    public CttTeacher(CttModel model, String id) {
        iModel = model;
        iId = id;
        iConstraint = new TeacherConstraint();
        iModel.addConstraint(iConstraint);
    }
    
    /** Return problem model */
    public CttModel getModel() {
        return iModel;
    }
    
    /** Return unique identifier */
    public String getId() {
        return iId;
    }
    
    /** Compare two teachers for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttTeacher)) return false;
        return getId().equals(((CttTeacher)o).getId());
    }
    
    /** Hash code */
    public int hashCode() {
        return getId().hashCode();
    }
    
    /** String representation */
    public String toString() {
        return getId();
    }
    
    /** Return appropriate teacher constraint */
    public TeacherConstraint getConstraint() {
        return iConstraint;
    }
    
    /**
     * Teacher constraint. This hard constraint ensures that two
     * lectures that are taught by the same teacher are not placed
     * at the same day and time.
     */
    public class TeacherConstraint extends ConstraintWithContext<CttLecture, CttPlacement, CttTeacher.Table> implements CanInheritContext<CttLecture, CttPlacement, CttTeacher.Table> {

    	/** Constructor */
        public TeacherConstraint() {
            super();
            
        }
        
        /** Return placement of a lecture that is taught by this teacher at the given
         * day and time.
         */
        public CttPlacement getPlacement(Assignment<CttLecture, CttPlacement> assignment, int d, int s) {
            return getContext(assignment).getPlacement(d, s);
        }
        
        /** Compute conflict, i.e., another lecture that is taught by this teacher and
         * is placed at the day and time of the given placement.
         */
        public void computeConflicts(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p, Set<CttPlacement> conflicts) {
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            if (conflict != null && !conflict.variable().equals(p.variable()))
                conflicts.add(conflict);
        }
        
        /** Compute conflict, i.e., another lecture that is taught by this teacher and
         * is placed at the day and time of the given placement.
         */
        public boolean inConflict(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p) {
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            return conflict != null && !conflict.variable().equals(p.variable());
        }
        
        /** Two lectures that are taught by the same teacher are consistent
         * only when placed at different day or time.
         */
        public boolean isConsistent(CttPlacement p1, CttPlacement p2) {
            return p1.getDay()!=p2.getDay() || p1.getSlot()!=p2.getSlot();
        }
        
        /**
         * Update information about placement of lectures of this teacher.
         */
        public void assigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
            //super.assigned(iteration, value);
        	Table table = getContext(assignment);
        	CttPlacement conflict = table.getPlacement(p.getDay(), p.getSlot());
            if (conflict != null) {
                assignment.unassign(iteration, conflict.variable());
                if (iConstraintListeners != null && !iConstraintListeners.isEmpty()) {
                    Set<CttPlacement> confs = new HashSet<CttPlacement>(); confs.add(conflict);
                    for (ConstraintListener<CttLecture, CttPlacement> listener: iConstraintListeners)
                        listener.constraintAfterAssigned(assignment, iteration, this, p, confs);
                }
            }
            table.assigned(assignment, p);
        }
        
        /**
         * Update information about placement of lectures of this teacher.
         */
        public void unassigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
            //super.unassigned(iteration, value);
        	getContext(assignment).unassigned(assignment, p);
        }
        
        /** String representation */
        public String toString() {
            return CttTeacher.this.toString();
        }
        
        /** Hash code */
        public int hashCode() {
            return CttTeacher.this.hashCode();
        }

		@Override
		public Table createAssignmentContext(Assignment<CttLecture, CttPlacement> assignment) {
			return new Table(assignment);
		}

		@Override
		public Table inheritAssignmentContext(Assignment<CttLecture, CttPlacement> assignment, Table parentContext) {
			return new Table(assignment, parentContext);
		}
    }
    
    private class Table implements AssignmentConstraintContext<CttLecture, CttPlacement> {
        private CttPlacement[] iPlacement;
        
        public Table(Assignment<CttLecture, CttPlacement> assignment) {
            iPlacement = new CttPlacement[iModel.getNrDays() * iModel.getNrSlotsPerDay()];
            for (int i = 0; i < iPlacement.length; i++)
            	iPlacement[i] = null;
			for (CttLecture lecture: iConstraint.variables()) {
				CttPlacement p = assignment.getValue(lecture);
				if (p != null) assigned(assignment, p);
			}
		}
        
        public Table(Assignment<CttLecture, CttPlacement> assignment, Table parent) {
        	iPlacement = Arrays.copyOf(parent.iPlacement, parent.iPlacement.length);
        }

		@Override
		public void assigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p) {
			iPlacement[p.getDay() * iModel.getNrSlotsPerDay() + p.getSlot()] = p;
		}

		@Override
		public void unassigned(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p) {
			iPlacement[p.getDay() * iModel.getNrSlotsPerDay() + p.getSlot()] = null;
		}
		
		public CttPlacement getPlacement(int day, int slot) {
			return iPlacement[day * iModel.getNrSlotsPerDay() + slot];
		}
    }
}
