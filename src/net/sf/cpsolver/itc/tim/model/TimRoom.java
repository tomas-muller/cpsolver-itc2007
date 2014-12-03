package net.sf.cpsolver.itc.tim.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.ConstraintListener;

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
public class TimRoom extends ConstraintWithContext<TimEvent, TimLocation, TimRoom.Context> implements CanInheritContext<TimEvent, TimLocation, TimRoom.Context> {
	private int iSize;
	
	/**
	 * Constructor
	 * @param id unique room identifier
	 * @param size room size
	 */
	public TimRoom(long id, int size) {
		super();
		iId = id;
		iSize = size;
	}
	
	/** Room size */
	public int size() {
		return iSize;
	}
	
	/** Compute conflicts: check whether another event is assigned in this room in the given time */
	public void computeConflicts(Assignment<TimEvent, TimLocation> assignment, TimLocation location, Set<TimLocation> conflicts) {
		if (sameRoom(location.room())) {
			TimLocation conflict = getLocation(assignment, location.time());
			if (conflict!=null && !conflict.variable().equals(location.variable()))
				conflicts.add(conflict);
		}
	}
	
	/** Return location that is assigned in this room at given time (if any) */
	public TimLocation getLocation(Assignment<TimEvent, TimLocation> assignment, int time) {
	    return getContext(assignment).getLocation(time);
	}
	
	/** Compute conflicts: check whether another event is assigned in this room in the given time */
	public boolean inConflict(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
		if (sameRoom(location.room())) {
			TimLocation conflict = getLocation(assignment, location.time());
			return conflict!=null && !conflict.variable().equals(location.variable());
		} else {
			return false;
		}
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
	public void assigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation location) {
		if (sameRoom(location.room())) {
			Context context = getContext(assignment);
			TimLocation conflict = context.getLocation(location.time());
			if (iConstraintListeners != null && conflict != null) {
				Set<TimLocation> confs = new HashSet<TimLocation>();
				confs.add(conflict);
				assignment.unassign(iteration, conflict.variable());
				context.assigned(assignment, location);
				for (ConstraintListener<TimEvent, TimLocation> listener: iConstraintListeners)
					listener.constraintAfterAssigned(assignment, iteration, this, location, confs);
			} else {
				context.assigned(assignment, location);
			}
        }
    }
		
    /** Update room assignment table */
    public void unassigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation location) {
    	if (sameRoom(location.room()))
    		getContext(assignment).unassigned(assignment, location);
    }
    
    public class Context implements AssignmentConstraintContext<TimEvent, TimLocation> {
        private TimLocation[] iTable = new TimLocation[45];

    	public Context(Assignment<TimEvent, TimLocation> assignment) {
            for (int i = 0; i < 45; i++)
            	iTable[i] = null;
            for (TimEvent event: variables()) {
            	TimLocation location = assignment.getValue(event);
            	if (location != null && sameRoom(location.room()))
            		iTable[location.time()] = location;
            }
    	}
    	
    	public Context(Assignment<TimEvent, TimLocation> assignment, Context parent) {
    		iTable = Arrays.copyOf(parent.iTable, 45);
    	}

		@Override
		public void assigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
			iTable[location.time()] = location;
		}

		@Override
		public void unassigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
			iTable[location.time()] = null;
		}

		/** Return location that is assigned in this room at given time (if any) */
		public TimLocation getLocation(int time) {
		    return iTable[time];
		}
    	
    }

	@Override
	public Context createAssignmentContext(Assignment<TimEvent, TimLocation> assignment) {
		return new Context(assignment);
	}

	@Override
	public Context inheritAssignmentContext(Assignment<TimEvent, TimLocation> assignment, Context parentContext) {
		return new Context(assignment, parentContext);
	}
}
