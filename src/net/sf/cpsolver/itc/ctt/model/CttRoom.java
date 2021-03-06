package net.sf.cpsolver.itc.ctt.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;

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
    public class RoomConstraint extends Constraint<CttLecture, CttPlacement> {
        public CttPlacement[][] iPlacement;
        
        /** Constructor */
        public RoomConstraint() {
            super();
            iAssignedVariables = null;
            iPlacement = new CttPlacement[iModel.getNrDays()][iModel.getNrSlotsPerDay()];
            for (int d=0;d<iModel.getNrDays();d++)
                for (int s=0;s<iModel.getNrSlotsPerDay();s++)
                    iPlacement[d][s] = null;
            
        }
        
        /** 
         * Compute conflicts, i.e., placements of another lecture that is using 
         * this room at the time of the given placement (if any).
         */
        public void computeConflicts(CttPlacement p, Set<CttPlacement> conflicts) {
            if (!p.getRoom().equals(CttRoom.this)) return; 
            if (iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(p.variable()))
                conflicts.add(iPlacement[p.getDay()][p.getSlot()]);
        }
        
        /** 
         * Check for conflicts, i.e., true, if there is another lecture that is using 
         * this room at the time of the given placement.
         */
        public boolean inConflict(CttPlacement p) {
            if (!p.getRoom().equals(CttRoom.this)) return false; 
            return iPlacement[p.getDay()][p.getSlot()]!=null && !iPlacement[p.getDay()][p.getSlot()].variable().equals(p.variable());
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
        public void assigned(long iteration, CttPlacement p) {
            //super.assigned(iteration, value);
            if (p.getRoom().equals(CttRoom.this)) {
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
        }
        
        /** Update placement of lectures into this room */
        public void unassigned(long iteration, CttPlacement p) {
            //super.unassigned(iteration, value);
            if (p.getRoom().equals(CttRoom.this))
                iPlacement[p.getDay()][p.getSlot()] = null;
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
        public CttPlacement getPlacement(int day, int slot) {
            return iPlacement[day][slot];
        }
    }
}
