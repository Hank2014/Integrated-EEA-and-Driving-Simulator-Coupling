package ptolemy.actor.lib.rmi;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.FloatToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class TestFloat extends TypedAtomicActor{

	public TypedIOPort input;
	public TypedIOPort output;
	private FloatToken ft; 
	private Parameter p; 
	public TestFloat(CompositeEntity container, String name) 
			throws IllegalActionException, NameDuplicationException {
		super(container,name);	
		input = new TypedIOPort(this,"input",true,false);
		output = new TypedIOPort(this,"output",false,true);
		p = new Parameter();
	}
	@Override
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
		p.setToken(new FloatToken());
		input.setTypeAtLeast(p);
		output.setTypeEquals(BaseType.FLOAT);
	}
	@Override
	public void initialize() throws IllegalActionException{
		ft = new FloatToken(0);
	}
	
	@Override
	public boolean prefire() throws IllegalActionException{
		if(!input.hasToken(0))
			return false;
		return super.prefire();
	}
	
	@Override
	public void fire() throws IllegalActionException{
		ft = new FloatToken(((FloatToken)input.get(0)).floatValue());
		output.send(0, ft);
	}
}
