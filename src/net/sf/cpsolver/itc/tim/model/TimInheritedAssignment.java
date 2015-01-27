package net.sf.cpsolver.itc.tim.model;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;

import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.assignment.context.InheritedAssignmentContextHolder;
import org.cpsolver.ifs.solution.Solution;

/**
 * Post Enrollment based Course Timetabling (tim) inherited assignment model.
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2014 Tomas Muller<br>
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
public class TimInheritedAssignment extends TimAssignment implements InheritedAssignment<TimEvent, TimLocation> {
	private long iVersion = -1;

	public TimInheritedAssignment(Solution<TimEvent, TimLocation> solution, int index) {
		super(new InheritedAssignmentContextHolder<TimEvent, TimLocation>(index, solution.getIteration()));
		iIndex = index;
		iVersion = solution.getIteration();
		iModel = (TimModel) solution.getModel();
		iParent = (TimAssignment) solution.getAssignment();
		Lock lock = solution.getLock().readLock();
		lock.lock();
		try {
			iNrAssigned = iParent.nrAssignedVariables();
			iAssignment = Arrays.copyOf(((TimAssignment)iParent).toArray(), iModel.variables().size());
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public long getVersion() {
		return iVersion;
	}
}