package net.sf.cpsolver.itc.ctt.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a curricula. A curriculum is a group of courses such that any pair of courses in 
 * the group have students in common. Based on curricula, 
 * we have the conflicts between courses and other soft constraints.
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
public class CttCurricula {
    private CttModel iModel;
    private String iId;
    private List<CttCourse> iCourses = new ArrayList<CttCourse>();
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
    public List<CttCourse> getCourses() {
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
    public int getCompactPenalty(Assignment<CttLecture, CttPlacement> assignment) {
        int penalty = 0;
        Table table = iConstraint.getContext(assignment);
        for (int d=0;d<iModel.getNrDays();d++) {
            for (int s=0;s<iModel.getNrSlotsPerDay();s++) {
                CttPlacement p = table.getPlacement(d, s);
                if (p==null) continue;
                CttPlacement prev = (s==0 ? null : table.getPlacement(d, s - 1));
                CttPlacement next = (s + 1 == iModel.getNrSlotsPerDay() ? null : table.getPlacement(d, s + 1));
                if (next==null && prev==null) penalty += 2;
            }
        }
        return penalty;
    }
    
    private CttPlacement prev(Table table, CttPlacement placement, CttPlacement eqCheck) {
        if (placement==null) return null;
        CttPlacement prev = (placement.getSlot()==0?null:table.getPlacement(placement.getDay(), placement.getSlot()-1));
        if (eqCheck!=null && prev!=null && eqCheck.variable().equals(prev.variable())) return null;
        return prev;
    }
    
    private CttPlacement next(Table table, CttPlacement placement, CttPlacement eqCheck) {
        if (placement==null) return null;
        CttPlacement next = (placement.getSlot()+1==iModel.getNrSlotsPerDay()?null:table.getPlacement(placement.getDay(), placement.getSlot()+1));
        if (eqCheck!=null && next!=null && eqCheck.variable().equals(next.variable())) return null;
        return next;
    }
    
    /**
     * Compute curriculum compactness penalty for given placement
     */
    public int getCompactPenalty(Assignment<CttLecture, CttPlacement> assignment, CttPlacement placement) {
    	Table table = iConstraint.getContext(assignment);
        CttPlacement prev = prev(table, placement, placement);
        CttPlacement next = next(table, placement, placement);
        if (prev==null && next==null) return 2;
        if (prev!=null && prev(table, prev, placement)==null) {
            if (next!=null && next(table, next, placement)==null) return -4;
            return -2;
        }
        if (next!=null && next(table, next, placement)==null) return -2;
        return 0;
    }
    
    /**
     * Curricula constraint. This hard constraint ensures that no two lectures of courses of the 
     * same curricula are placed at the same time.
     *
     */
    public class CurriculaConstraint extends ConstraintWithContext<CttLecture, CttPlacement, CttCurricula.Table> {
        
        /** Constructor */
        public CurriculaConstraint() {
            super();
        }
        
        /** Return placement of a lecture of this curricula on given day and time */
        public CttPlacement getPlacement(Assignment<CttLecture, CttPlacement> assignment, int d, int s) {
            return getContext(assignment).getPlacement(d, s);
        }

        /** Compute conflicts, i.e., placement of another lecture of the this curricula at the day and time of the given placement */
        public void computeConflicts(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p, Set<CttPlacement> conflicts) {
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            if (conflict != null && !conflict.variable().equals(p.variable()))
                conflicts.add(conflict);
        }
        
        /** Compute conflicts, i.e., placement of another lecture of the this curricula at the day and time of the given placement */
        public boolean inConflict(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p) {
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            return conflict != null && !conflict.variable().equals(p.variable());
        }
        
        /** Two lectures of the same curricula are consistent if placed on different day or time */
        public boolean isConsistent(CttPlacement p1, CttPlacement p2) {
            return p1.getDay() != p2.getDay() || p1.getSlot() != p2.getSlot();
        }
        
        /** Update placements of lectures of this curricula */
        public void assigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
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
        
        /** Update placements of lectures of this curricula */
        public void unassigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
        	getContext(assignment).unassigned(assignment, p);
        }
        
        /** String representation */
        public String toString() {
            return CttCurricula.this.toString();
        }

        /** Hash code */
        public int hashCode() {
            return CttCurricula.this.hashCode();
        }

		@Override
		public Table createAssignmentContext(Assignment<CttLecture, CttPlacement> assignment) {
			return new Table(assignment);
		}
    }
    
    private class Table implements AssignmentConstraintContext<CttLecture, CttPlacement> {
        public CttPlacement[] iPlacement;
        
        public Table(Assignment<CttLecture, CttPlacement> assignment) {
            iPlacement = new CttPlacement[iModel.getNrDays() * iModel.getNrSlotsPerDay()];
            for (int i = 0; i < iPlacement.length; i++)
            	iPlacement[i] = null;
			for (CttLecture lecture: iConstraint.variables()) {
				CttPlacement p = assignment.getValue(lecture);
				if (p != null) assigned(assignment, p);
			}
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
