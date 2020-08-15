package ptolemy.actor.lib.rmi;


import ptolemy.actor.TypedAtomicActor;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public abstract class RMIUpdatableImpl extends TypedAtomicActor implements RMIUpdatable{

	public RMIUpdatableImpl(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
		super(container,name);
	}
	
	///////////////////////////////////////////////////////////////////
	////                         public methods                    ////
	@Override
	public void initialize() throws IllegalActionException{
		super.initialize();
	}
	
	@Override
    public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}
	
	@Override
	public void fire() throws IllegalActionException{	
		super.fire();
	}
	

}
