package statisticEvaluation;

import org.apache.log4j.PropertyConfigurator;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class RandomVariable extends TypedAtomicActor{
	
	private double oldMean;
	private int size;
	private double oldVariance;
	
	public RandomVariable(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.DOUBLE);
	    
		output = new TypedIOPort(this, "mean", false, true);
	    output.setTypeEquals(BaseType.DOUBLE);  
	    
	    output2 = new TypedIOPort(this, "variance", false, true);
	    output2.setTypeEquals(BaseType.DOUBLE);  
	}
	
	
	public void initialize() throws IllegalActionException{
		super.initialize();
		oldMean = 0.0;
		size = 0;
		oldVariance = 0.0;
	}
	
	public boolean prefire() throws IllegalActionException{
		if(!input.hasToken(0)) {
			return false;
		}

		return super.prefire(); 
	}
	
	public void fire() throws IllegalActionException{
		double data = ((DoubleToken)(input.get(0))).doubleValue();
		oldMean = getMean(data);
		oldVariance = getVariance(data);
		
		output.send(0, new DoubleToken(oldMean));
		output2.send(0, new DoubleToken(oldVariance));
		
		size++;
	}
	
	private double getMean(double newValue) {
		double sum = oldMean * size;
		sum += newValue;
		return sum / (size+1);
	}
	
	private double getVariance(double newValue) {
		double mean = getMean(newValue);
		double temp = ((double)size/(size+1))*(oldVariance+Math.pow(newValue-oldMean,2)/(size+1));
		return temp;
	}
	
	
	TypedIOPort input;
	TypedIOPort output;
	TypedIOPort output2;
}
