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
 * Representation of a room. Two lectures cannot be placed into the same room 
 * at the same day and time.
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
public class CttRoom {
    private CttModel iModel;
    private String iId;
    private int iSize = 0;
    private RoomConstraint iConstraint = null;
    
    /** 
     * Constructor
     * @param model problem model
     * @param id unique identifier
     * @param size room size
     */
    public CttRoom(CttModel model, String id, int size) {
        iModel = model;
        iId = id;
        iSize = size;
        iConstraint = new RoomConstraint();
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
    
    /** Return room size */
    public int getSize() {
        return iSize;
    }
    
    /** Compare two rooms for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof CttRoom)) return false;
        return getId().equals(((CttRoom)o).getId());
    }
    
    /** Hash code */
    public int hashCode() {
        return getId().hashCode();
    }
    
    /** String representation */
    public String toString() {
        return getId()+" "+getSize();
    }
    
    /** Return appropriate room constraint */
    public RoomConstraint getConstraint() {
        return iConstraint;
    }
    
    /** Room constraint. This hard constraint ensures that no two lectures
     * are placed into this room at the same time. 
     */ 
    public class RoomConstraint extends ConstraintWithContext<CttLecture, CttPlacement, CttRoom.Table> implements CanInheritContext<CttLecture, CttPlacement, CttRoom.Table> {
        
        /** Constructor */
        public RoomConstraint() {
            super();
        }
        
        /** 
         * Compute conflicts, i.e., placements of another lecture that is using 
         * this room at the time of the given placement (if any).
         */
        public void computeConflicts(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p, Set<CttPlacement> conflicts) {
            if (!p.getRoom().equals(CttRoom.this)) return; 
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            if (conflict != null && !conflict.variable().equals(p.variable()))
                conflicts.add(conflict);
        }
        
        /** 
         * Check for conflicts, i.e., true, if there is another lecture that is using 
         * this room at the time of the given placement.
         */
        public boolean inConflict(Assignment<CttLecture, CttPlacement> assignment, CttPlacement p) {
            if (!p.getRoom().equals(CttRoom.this)) return false; 
        	CttPlacement conflict = getPlacement(assignment, p.getDay(), p.getSlot());
            return conflict != null && !conflict.variable().equals(p.variable());
        }
        
        /**
         * Two lectures that are using this room are consistent if placed at 
         * different day or time.
         */
        public boolean isConsistent(CttPlacement p1, CttPlacement p2) {
            if (!p1.getRoom().equals(CttRoom.this)) return true;
            if (!p2.getRoom().equals(CttRoom.this)) return true;
            return p1.getDay()!=p2.getDay() || p1.getSlot()!=p2.getSlot();
        }
        
        /** Update placement of lectures into this room */
        public void assigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
            //super.assigned(iteration, value);
            if (p.getRoom().equals(CttRoom.this)) {
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
        }
        
        /** Update placement of lectures into this room */
        public void unassigned(Assignment<CttLecture, CttPlacement> assignment, long iteration, CttPlacement p) {
            //super.unassigned(iteration, value);
            if (p.getRoom().equals(CttRoom.this))
            	getContext(assignment).unassigned(assignment, p);
        }
        
        /** String representation */
        public String toString() {
            return CttRoom.this.toString();
        }

        /** Hash code */
        public int hashCode() {
            return CttRoom.this.hashCode();
        }
        
        /** Placement of a lecture into this room at given day and time */
        public CttPlacement getPlacement(Assignment<CttLecture, CttPlacement> assignment, int d, int s) {
            return getContext(assignment).getPlacement(d, s);
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
        public CttPlacement[] iPlacement;
        
        public Table(Assignment<CttLecture, CttPlacement> assignment) {
            iPlacement = new CttPlacement[iModel.getNrDays() * iModel.getNrSlotsPerDay()];
            for (int i = 0; i < iPlacement.length; i++)
            	iPlacement[i] = null;
			for (CttPlacement p: assignment.assignedValues())
				if (p.getRoom().equals(CttRoom.this))
					assigned(assignment, p);
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
