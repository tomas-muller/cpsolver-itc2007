package net.sf.cpsolver.itc.ctt.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;

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
    public class TeacherConstraint extends Constraint<CttLecture, CttPlacement> {
        public CttPlacement[][] iPlacement;
        
        /** Constructor */
        public TeacherConstraint() {
            super();
            iAssignedVariables = null;
            iPlacement = new CttPlacement[iModel.getNrDays()][iModel.getNrSlotsPerDay()];
            for (int d=0;d<iModel.getNrDays();d++)
                for (int s=0;s<iModel.getNrSlotsPerDay();s++)
                    iPlacement[d][s] = null;
            
        }
        
        /** Return placement of a lecture that is taught by this teacher at the given
         * day and time.
         */
        public CttPlacement getPlacement(int d, int s) {
            return iPlacement[d][s];
        }
        
        /** Compute conflict, i.e., another lecture that is taught by this teacher and
         * is placed at the day and time of the given placement.
         */
        public void computeConflicts(CttPlacement p, Set<CttPlacement> conflicts) {
            if (iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(p.variable()))
                conflicts.add(iPlacement[p.getDay()][p.getSlot()]);
        }
        
        /** Compute conflict, i.e., another lecture that is taught by this teacher and
         * is placed at the day and time of the given placement.
         */
        public boolean inConflict(CttPlacement p) {
            return iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(p.variable());
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
        public void assigned(long iteration, CttPlacement p) {
            //super.assigned(iteration, value);
            if (iPlacement[p.getDay()][p.getSlot()]!=null) {
                Set<CttPlacement> confs = new HashSet<CttPlacement>(); confs.add(iPlacement[p.getDay()][p.getSlot()]);
                iPlacement[p.getDay()][p.getSlot()].variable().unassign(iteration);
                iPlacement[p.getDay()][p.getSlot()] = p;
                if (iConstraintListeners!=null)
                    for (ConstraintListener<CttPlacement> listener: iConstraintListeners)
                        listener.constraintAfterAssigned(iteration, this, p, confs);
            } else {
                iPlacement[p.getDay()][p.getSlot()] = p;
            }
        }
        
        /**
         * Update information about placement of lectures of this teacher.
         */
        public void unassigned(long iteration, CttPlacement p) {
            //super.unassigned(iteration, value);
            iPlacement[p.getDay()][p.getSlot()] = null;
        }
        
        /** String representation */
        public String toString() {
            return CttTeacher.this.toString();
        }
        
        /** Hash code */
        public int hashCode() {
            return CttTeacher.this.hashCode();
        }
    }
}
