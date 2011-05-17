package net.sf.cpsolver.itc.tim.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;

/**
 * Representation of a room constraint. It is not allowed to assign two 
 * events a the same time and room. Also, a room must be of enough size 
 * and has to have all the features required by the event, this is 
 * handled by computing the set of allowed rooms of an event (see 
 * {@link TimEvent#rooms()}.
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
public class TimRoom extends Constraint<TimEvent, TimLocation> {
    private TimLocation[] iTable = new TimLocation[45];
	private int iSize;
	
	/**
	 * Constructor
	 * @param id unique room identifier
	 * @param size room size
	 */
	public TimRoom(long id, int size) {
		super();
		iAssignedVariables=null;
		iId = id;
		iSize = size;
        for (int i=0;i<45;i++) {
            iTable[i]=null;
        }
	}
	
	/** Room size */
	public int size() {
		return iSize;
	}
	
	/** Compute conflicts: check whether another event is assigned in this room in the given time */
	public void computeConflicts(TimLocation value, Set<TimLocation> conflicts) {
		TimLocation location = (TimLocation)value;
		if (sameRoom(location.room())) {
			if (iTable[location.time()]!=null && !iTable[location.time()].variable().equals(location.variable())) {
				conflicts.add(iTable[location.time()]);
			}
		}
	}
	
	/** Return location that is assigned in this room at given time (if any) */
	public TimLocation getLocation(int time) {
	    return iTable[time];
	}
	
	/** Compute conflicts: check whether another event is assigned in this room in the given time */
	public boolean inConflict(TimLocation value) {
        TimLocation location = (TimLocation)value;
        return (sameRoom(location.room()) && iTable[location.time()]!=null && !iTable[location.time()].variable().equals(location.variable()));
	}
	
	/** Two events are consistent if they are using this room at different times */
	public boolean isConsistent(TimLocation loc1, TimLocation loc2) {
	    return (sameRoom(loc1.room()) && sameRoom(loc2.room()) && loc1.time()==loc2.time()); 
	}
	
	/** String representation */
	public String toString() {
		return "r"+getId();
	}
	
	/** Compare two rooms for equality */
	public boolean sameRoom(TimRoom room) {
	    return (room==null?false:getId()==room.getId());
	}

	/** Update room assignment table */
	public void assigned(long iteration, TimLocation loc) {
	    //super.assigned(iteration, value);
	    if (sameRoom(loc.room())) {
	        if (iTable[loc.time()]!=null) {
	            Set<TimLocation> confs = new HashSet<TimLocation>(); confs.add(iTable[loc.time()]);
	            iTable[loc.time()].variable().unassign(iteration);
	            iTable[loc.time()]=loc;
                if (iConstraintListeners!=null)
                    for (ConstraintListener<TimLocation> listener: iConstraintListeners)
                        listener.constraintAfterAssigned(iteration, this, loc, confs);
	        } else {
	            iTable[loc.time()]=loc;
	        }
	    }
    }
		
    /** Update room assignment table */
    public void unassigned(long iteration, TimLocation value) {
        //super.unassigned(iteration, value);
        TimLocation loc = (TimLocation)value;
        if (sameRoom(loc.room())) iTable[loc.time()]=null;
    }
}
