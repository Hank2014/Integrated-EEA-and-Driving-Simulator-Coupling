package rmiinterface;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIObjectInterface extends Remote {
	// For testing
	public String helloWorld(String name) throws RemoteException;
	
	// Flow control
    boolean startSimulator () throws RemoteException ;
    void pauseSimulator() throws RemoteException;
	void resumeSimulator() throws RemoteException;
	boolean isSimPaused() throws RemoteException;
	float getLogicalTime() throws RemoteException;
	boolean getInitializationFinished() throws RemoteException;

	
	//Fine-tune Parameter
	void setMinTimeDiffForUpdate(float f) throws RemoteException;
	void setTestFileName(String s) throws RemoteException;
	
    //Cardynamic-related
    float getCarSpeed() throws RemoteException;
    float getCarAcceleratePedalValue() throws RemoteException;
    float[] getCarPosition() throws RemoteException;
    float getCarHeading() throws RemoteException;
    void setEngineOn() throws RemoteException;
    void setCarAcceleratePedalValue(float f) throws RemoteException;
    void setCarSteering(float f) throws RemoteException;
    float[] getODInfo() throws RemoteException;
    
    //Camera-related
    //boolean getCameraUpdatedFrameNotRead() throws RemoteException;
    //void setCameraUpdatedFrameNotRead(boolean b) throws RemoteException;
    int getCamCount() throws RemoteException;
    
    //RMI callback
    void addRMIListener(RMIListener a) throws RemoteException;
    void removeRMIListener(RMIListener r) throws RemoteException;

    
}
