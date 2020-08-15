package eu.opends.main;

public class SimulatorCommandCenter{ 
    private Simulator sim = null;
	public static boolean stoprequested;

	protected SimulatorCommandCenter(Simulator sim){
		this.sim = sim;
	}
	
    /**
	 * Requests the connection to close after the current loop
	 */
	public synchronized void requestStop() 
	{
		stoprequested = true;
	}
	
	public Simulator getSimulator() {
		Simulator sim = this.sim;
		return sim;
	}
	

	
	//check if initialzation tasks in opends Simulator is done
	public boolean testReady() {
		if(!stoprequested) {
			if(sim.getInitializationFinished()) {
				return true;
			}
		}
		return false;
	}
	
	///////////////////////////////////////////////////////////////////
	////                  cardynamic-related methods               ////
	public float getCarCurrentSpeedKmh() {
		if(testReady()) {
			return sim.getCar().getCurrentSpeedKmh();
		} 
		return 0.0f;
	}
	
	public float getCarHeadingDegree() {
		if(testReady()) {
			return sim.getCar().getHeadingDegree();
		}
		return 0.0f;
	}
	
	// note that car.getAcceleratorPedalIntensity performs Math.abs on the pedal value, so
	// this will only return 1 or 0.
	public float getCarAcceleratorPedalIntensity() {
		if(testReady()) {
			return sim.getCar().getAcceleratorPedalIntensity();
		}
		return 0.0f;
	}
	
	public void setAcceleratorPedalIntensity(float f) {
		if(testReady()) {
			sim.getCar().setAcceleratorPedalIntensity(f);
		}
	}
	
	public void carSteering(float f) {
		if(testReady()) {
			sim.getCar().steer(f);
		}
	}
	
	public float[] getCarPosition() {
		float[] p = new float[] {0.0f,0.0f,0.0f};
		if(testReady()) {
			p[0] = sim.getCar().getPosition().x;
			p[1] = sim.getCar().getPosition().y;
			p[2] = sim.getCar().getPosition().z;
		}
		return p;
	}

	public void setEngineOn() {
		sim.getCar().setEngineOn(true);
	}
	

}
