package net.sf.cpsolver.itc.ctt.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Representation of a curricula. A curriculum is a group of courses such that any pair of courses in 
 * the group have students in common. Based on curricula, 
 * we have the conflicts between courses and other soft constraints.
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
public class CttCurricula {
    private CttModel iModel;
    private String iId;
    private Vector iCourses = new Vector();
    private CurriculaConstraint iConstraint;
    
    /**
     * Constructor
     * @param model problem model
     * @param id unique identifier
     */
    public CttCurricula(CttModel model, String id) {
        iModel = model;
        iId = id;
        iConstraint = new CurriculaConstraint();
        model.addConstraint(iConstraint);
    }
    
    /** Return problem model */
    public CttModel getModel() {
        return iModel;
    }
    
    /** Return unique identifier */
    public String getId() {
        return iId;
    }
    
    /**
     * Return list of courses associated with this curricula.
     * @return list of {@link CttCourse}
     */
    public Vector getCourses() {
        return iCourses;
    }
    
    /** Compare two curriculas for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttCurricula)) return false;
        return getId().equals(((CttCurricula)o).getId());
    }
    
    /** Hash code */
    public int hashCode() {
        return getId().hashCode();
    }
    
    /** String representation */
    public String toString() {
        return getId()+" "+getCourses();
    }
    
    /** Curricula constraint */
    public CurriculaConstraint getConstraint() {
        return iConstraint;
    }
    
    /** 
     * Compute curriculum compactness penalty:
     * Lectures belonging to a curriculum should be adjacent to each other (i.e., in consecutive periods). 
     * For a given curriculum we account for a violation every time there is one lecture not adjacent to any 
     * other lecture within the same day. Each isolated lecture in a curriculum counts as 2 points of penalty.
     */
    public int getCompactPenalty() {
        int penalty = 0;
        for (int d=0;d<iModel.getNrDays();d++) {
            for (int s=0;s<iModel.getNrSlotsPerDay();s++) {
                CttPlacement p = iConstraint.iPlacement[d][s];
                if (p==null) continue;
                CttPlacement prev = (s==0?null:iConstraint.iPlacement[d][s-1]);
                CttPlacement next = (s+1==iModel.getNrSlotsPerDay()?null:iConstraint.iPlacement[d][s+1]);
                if (next==null && prev==null) penalty += 2;
            }
        }
        return penalty;
    }
    
    private CttPlacement prev(CttPlacement placement, CttPlacement eqCheck) {
        if (placement==null) return null;
        CttPlacement prev = (placement.getSlot()==0?null:iConstraint.iPlacement[placement.getDay()][placement.getSlot()-1]);
        if (eqCheck!=null && prev!=null && eqCheck.variable().equals(prev.variable())) return null;
        return prev;
    }
    
    private CttPlacement next(CttPlacement placement, CttPlacement eqCheck) {
        if (placement==null) return null;
        CttPlacement next = (placement.getSlot()+1==iModel.getNrSlotsPerDay()?null:iConstraint.iPlacement[placement.getDay()][placement.getSlot()+1]);
        if (eqCheck!=null && next!=null && eqCheck.variable().equals(next.variable())) return null;
        return next;
    }
    
    /**
     * Compute curriculum compactness penalty for given placement
     */
    public int getCompactPenalty(CttPlacement placement) {
        CttPlacement prev = prev(placement, placement);
        CttPlacement next = next(placement, placement);
        if (prev==null && next==null) return 2;
        if (prev!=null && prev(prev, placement)==null) {
            if (next!=null && next(next, placement)==null) return -4;
            return -2;
        }
        if (next!=null && next(next, placement)==null) return -2;
        return 0;
    }
    
    /**
     * Curricula constraint. This hard constraint ensures that no two lectures of courses of the 
     * same curricula are placed at the same time.
     *
     */
    public class CurriculaConstraint extends Constraint {
        public CttPlacement[][] iPlacement;
        
        /** Constructor */
        public CurriculaConstraint() {
            super();
            iAssignedVariables = null;
            iPlacement = new CttPlacement[iModel.getNrDays()][iModel.getNrSlotsPerDay()];
            for (int d=0;d<iModel.getNrDays();d++)
                for (int s=0;s<iModel.getNrSlotsPerDay();s++)
                    iPlacement[d][s] = null;
        }
        
        /** Return placement of a lecture of this curricula on given day and time */
        public CttPlacement getPlacement(int d, int s) {
            return iPlacement[d][s];
        }

        /** Compute conflicts, i.e., placement of another lecture of the this curricula at the day and time of the given placement */
        public void computeConflicts(Value value, Set conflicts) {
            CttPlacement p = (CttPlacement)value;
            if (iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(value.variable()))
                conflicts.add(iPlacement[p.getDay()][p.getSlot()]);
        }
        
        /** Compute conflicts, i.e., placement of another lecture of the this curricula at the day and time of the given placement */
        public boolean inConflict(Value value) {
            CttPlacement p = (CttPlacement)value;
            return iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(value.variable());
        }
        
        /** Two lectures of the same curricula are consistent if placed on different day or time */
        public boolean isConsistent(Value value1, Value value2) {
            CttPlacement p1 = (CttPlacement)value1;
            CttPlacement p2 = (CttPlacement)value2;
            return p1.getDay()!=p2.getDay() || p1.getSlot()!=p2.getSlot();
        }
        
        /** Update placements of lectures of this curricula */
        public void assigned(long iteration, Value value) {
            //super.assigned(iteration, value);
            CttPlacement p = (CttPlacement)value;
            if (iPlacement[p.getDay()][p.getSlot()]!=null) {
                HashSet confs = new HashSet(); confs.add(iPlacement[p.getDay()][p.getSlot()]);
                iPlacement[p.getDay()][p.getSlot()].variable().unassign(iteration);
                iPlacement[p.getDay()][p.getSlot()] = p;
                if (iConstraintListeners!=null)
                    for (Enumeration e=iConstraintListeners.elements();e.hasMoreElements();)
                        ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, confs);
            } else {
                iPlacement[p.getDay()][p.getSlot()] = p;
            }
        }
        
        /** Update placements of lectures of this curricula */
        public void unassigned(long iteration, Value value) {
            //super.unassigned(iteration, value);
            CttPlacement p = (CttPlacement)value;
            iPlacement[p.getDay()][p.getSlot()] = null;
        }
        
        /** String representation */
        public String toString() {
            return CttCurricula.this.toString();
        }

        /** Hash code */
        public int hashCode() {
            return CttCurricula.this.hashCode();
        }
    }
}
