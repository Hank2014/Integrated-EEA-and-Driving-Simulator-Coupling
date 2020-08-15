package eu.opends.basics;

public class SyncManager {
	private MyBulletAppState myBulletAppState;
	
	public SyncManager(MyBulletAppState bulletAppState) {
		this.myBulletAppState = bulletAppState;
	}
	
	public boolean shouldUpdate(float timeLastUpdate, float Period) {
		boolean update = false;
		int scale = (int) Math.pow(10, Math.log10(1/Period));
		
		float elapsedBulletTime = myBulletAppState.getElapsedSecondsSinceStart();
		float bulletTimeDiff = ((float)Math.round(scale*(elapsedBulletTime - timeLastUpdate)))/scale;

		//System.out.println(			"			elapsed:"+elapsedBulletTime+" bulletTimeDiff"+bulletTimeDiff);
		
		if(bulletTimeDiff >= Period) {
			update = true;
		}
		return update;
	}
}
