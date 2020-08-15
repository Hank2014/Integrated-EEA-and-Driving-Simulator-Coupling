package eu.opends.main;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import eu.opends.camera.SimulatorCam;
import eu.opends.car.SteeringCar;
import rmiinterface.RMIListener;
import rmiinterface.RMIObjectInterface;



public class RMIControlServer extends UnicastRemoteObject implements RMIObjectInterface{
	/**
	 * auto-generated ID
	 */
	private static final long serialVersionUID = -2972944185845499617L;

	private static Simulator sim;
	private static SimulatorCommandCenter simCom;
	private static List<RMIListener> listeners = new ArrayList<>();
	private String path = System.getProperty("user.dir")+"\\assets\\DrivingTasks\\Projects\\Highway\\Highway.xml";
	
	
	public RMIControlServer() throws RemoteException{
		
	}

	public static void main(String[] args) {
		try {
			RMIObjectInterface stub = new RMIControlServer();
			LocateRegistry.createRegistry(1099);
			System.err.println("Server established");
			Naming.rebind("opendsSim", stub);


		}catch (Exception e) { 
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	@Override
	public boolean startSimulator() throws RemoteException {
		boolean isOpenDSStarted = false;
        String driver = "ITIV_Schumacher";
        try {
            Simulator.main(new String[] {path,driver}); 
            simCom = Simulator.simComCen;
            sim = simCom.getSimulator();
            isOpenDSStarted = true;
        }catch (Exception e) {}
        
        return isOpenDSStarted;
	}
	

	@Override
	public String helloWorld(String name) throws RemoteException {
		return "hello"+name;
	}

	@Override
	public void pauseSimulator() throws RemoteException {
		sim.setPause(true);
		System.err.println("opends paused!");
	}
	
	@Override
	public void resumeSimulator() throws RemoteException {
		sim.setPause(false);
	}
	
	@Override
	public float getCarSpeed() throws RemoteException {
		return simCom.getCarCurrentSpeedKmh(); 
	}

	@Override
	public float getCarHeading() throws RemoteException {
		return simCom.getCarHeadingDegree();
	}

	@Override
	public float getCarAcceleratePedalValue() throws RemoteException {
		return simCom.getCarAcceleratorPedalIntensity();
	}

	@Override
	public void setCarAcceleratePedalValue(float f) throws RemoteException {
		simCom.setAcceleratorPedalIntensity(f);  
	}

	@Override
	public float[] getCarPosition() throws RemoteException {
		return simCom.getCarPosition();
	}

	@Override
	public boolean isSimPaused() throws RemoteException {
		if(sim.getInitializationFinished())
			return sim.isPause();
		return false;
	}

	@Override
	public float getLogicalTime() throws RemoteException {
		return sim.getBulletAppState().getElapsedSecondsSinceStart();
	}

	@Override
	public int getCamCount() throws RemoteException {
		if(null != sim.getSimulatorCam())
			return sim.getSimulatorCam().getCamCount();
		return 0;
	}

	@Override
	public void setMinTimeDiffForUpdate(float f) throws RemoteException {
		sim.setMinTimeDiffForUpdate(f);
	}

	@Override
	public void setCarSteering(float f) throws RemoteException {
		simCom.carSteering(f);
	}

	@Override
	public boolean getInitializationFinished() throws RemoteException {
		if(sim == null)
			return false;
		return sim.getInitializationFinished();
	}

	@Override
	public void setEngineOn() throws RemoteException {
		simCom.setEngineOn();
	}
	
	@Override
	public void addRMIListener(RMIListener a) throws RemoteException {
		System.out.println("Listener Added");
		listeners.add(a);
	}

	@Override
	public void removeRMIListener(RMIListener r) throws RemoteException {
		listeners.remove(r);
	}

	
	
	protected static byte[] getScreenshot() throws IOException {
		byte[] img = null;
		SimulatorCam sCamera = sim.getSimulatorCam();
		if(sCamera != null)
			img = sCamera.takePeriodicRGB();
		return img;
	}
	
	protected static void sendImageInByteArray(byte [] b) throws RemoteException{
		for(RMIListener l: listeners) {
			l.sendImageInByteArray(b);
		}
	}
	
	protected static void sendSteeringAngle(float f) throws RemoteException{
		for(RMIListener l: listeners) {
			System.out.println("sending car steering angle");
			l.sendSteeringAngle(f);	
		}
	}

	@Override
	public void setTestFileName(String str) throws RemoteException {
		path = str ;
	}

	@Override
	public float[] getODInfo() throws RemoteException {
		float [] dst = new float[3];
		dst = sim.getCar().getODRelatedParameters();
		return dst;
	}
	

	
}


