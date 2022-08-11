package Boids2_PKG;

import java.util.concurrent.ThreadLocalRandom;

import base_UI_Objects.windowUI.base.myDispWindow;
import base_Math_Objects.MyMathUtils;


//struct-type class to hold flocking variables for a specific flock
public class myFlkVars {
	public myBoids3DWin win;
	
	private final float neighborMult = .5f;							//multiplier for neighborhood consideration against zone size - all rads built off this
	
	public float dampConst = .01f;				//multiplier for damping force, to slow boats down if nothing else acting on them
	
	public float nghbrRad,									//radius of the creatures considered to be neighbors
					colRad,										//radius of creatures to be considered for colision avoidance
					velRad,										//radius of creatures to be considered for velocity matching
					predRad,									//radius for creature to be considered for pred/prey
					spawnPct,									//% chance to reproduce given the boids breech the required radius
					spawnRad,									//distance to spawn * mass
					killPct,									//% chance to kill prey creature
					killRad;									//radius to kill * mass (distance required to make kill attempt)
	
	public int spawnFreq, 									//# of cycles that must pass before can spawn again
				eatFreq;								 		//# cycles w/out food until starve to death
	
	float nghbrRadMax;							//max allowed neighborhood - min dim of cube
	public float totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	public float[] massForType,		//
					maxFrcs,									//max forces for this flock, for each force type
					wts;										//weights for flock calculations for this flock
	//idx's of vars in wts arrays
	public final int wFrcCtr = 0,								//idx in wt array for multiplier of centering force
            		wFrcAvd = 1,								//idx in wt array for multiplier of col avoidance force
            		wFrcVel = 2,								//idx in wt array for multiplier of velocity matching force
            		wFrcWnd = 3,								//idx in wt array for multiplier of wandering force
            		wFrcAvdPred = 4,							//idx in wts array for predator avoidance
            		wFrcChsPrey = 5;							//idx in wts array for prey chasing
	
	public float maxVelMag = 180,		//max velocity for flock member 
				minVelMag;										//min velocity for flock member

	protected static final float[] 
			defWtAra = new float[]{.5f, .75f, .5f, .5f, .5f, .1f},									//default array of weights for different forces
			defOrigWtAra = new float[]{5.0f, 12.0f, 7.0f, 3.5f, .5f, .1f},							//default array of weights for different forces
			MaxWtAra = new float[]{15, 15, 15, 15, 15, 15},								
			MinWtAra = new float[]{.01f, .01f, .01f, .01f, .001f, .001f},			
			MaxSpAra = new float[]{1,10000,100000},								
			MinSpAra = new float[]{.001f, 100, 100},			
			MaxHuntAra = new float[]{.1f,10000,100000},							
			MinHuntAra = new float[]{.00001f, 10, 100};	
	
	public final String typeName;
	
	public myFlkVars(myBoids3DWin _win, String _flockName, float _nRadMult) {
		win = _win;
		typeName = _flockName;
		initFlockVals(_nRadMult, .05f);
	}//ctor
	
	public float getInitMass(){return (float)(massForType[0] + (massForType[1] - massForType[0])*ThreadLocalRandom.current().nextFloat());}
	
	//set initial values
	public void initFlockVals(float nRadMult, float _spnPct){
		predRad = MyMathUtils.min(MyMathUtils.min(myDispWindow.AppMgr.gridDimY, myDispWindow.AppMgr.gridDimZ), myDispWindow.AppMgr.gridDimX);					//radius to avoid pred/find prey	
		nghbrRadMax = predRad*neighborMult;
		nghbrRad = nghbrRadMax*nRadMult;
		colRad  = nghbrRad*.1f;
		velRad  = nghbrRad*.5f; 			
		//weight multiplier for forces - centering, avoidance, velocity matching and wander
		spawnPct = _spnPct;		//% chance to reproduce given the boids breech the required radius
		spawnRad = colRad;			//distance to spawn 
		spawnFreq = 500; 		//# of cycles that must pass before can spawn again
		//required meal time
		eatFreq = 500; 			//# cycles w/out food until starve to death
		killRad = 1;						//radius to kill * mass
		killPct = .01f;				//% chance to kill prey creature

		setDefaultWtVals(true);//init to true
		massForType = new float[]{2.0f*nRadMult,4.0f*nRadMult}; //nRadMult should vary from 1.0 to .25
		maxFrcs = new float[]{100,200,100,10,400,20};		//maybe scale forces?
		minVelMag = maxVelMag*.0025f;
	}
	

	//if set default weight mults based on whether using force calcs based on original inverted distance functions or linear distance functions
	public void setDefaultWtVals(boolean useOrig){	
		float[] srcAra = (useOrig ? defOrigWtAra: defWtAra);
		wts = new float[srcAra.length]; 
		System.arraycopy( srcAra, 0, wts, 0, srcAra.length );				
	}
	

	//handles all modification of flock values from ui - wIdx is manufactured based on location in ui click area
	public void modFlkVal(int wIdx, float mod){
		//win.getMsgObj().dispInfoMessage("myFlkVars","modFlkVal","Attempt to modify flock : " + flock.name + " value : " + wIdx + " by " + mod);
		if(wIdx==-1){return;}
		switch(wIdx){
		//hierarchy - if neighbor then col and vel, if col then 
			case 0  : {
				//win.getMsgObj().dispInfoMessage("myFlkVars","modFlkVal","nghbrRad : " + nghbrRad + " max : " + nghbrRadMax + " mod : " + mod);
				nghbrRad = modVal(nghbrRad, .1f*nghbrRadMax, nghbrRadMax, mod);
				fixNCVRads(true, true);				
				break;}			//flck radius
			case 1  : {colRad = modVal(colRad, .05f*nghbrRad, .9f*nghbrRad, mod);fixNCVRads(false, true);break;}	//avoid radius
			case 2  : {velRad = modVal(velRad, colRad, .9f*nghbrRad, mod);break;}			//vel match radius
			
			case 3  : 						//3-8 are the 6 force weights
			case 4  : 
			case 5  : 
			case 6  : 
			case 7  : 
			case 8  : {modFlkWt(wIdx-3,mod*.01f);break;}						//3-9 are the 6 force weights
			
			case 9  : {spawnPct = modVal(spawnPct, MinSpAra[0], MaxSpAra[0], mod*.001f); break;}
			case 10 : {spawnRad = modVal(spawnRad, MinSpAra[1], MaxSpAra[1], mod);break;}
			case 11 : {spawnFreq = modVal(spawnFreq, MinSpAra[2], MaxSpAra[2], (int)(mod*10));break;}
			case 12 : {killPct  = modVal(killPct, MinHuntAra[0], MaxHuntAra[0], mod*.0001f); break;}
			case 13 : {predRad  = modVal(predRad, MinHuntAra[1], MaxHuntAra[1], mod);break;}
			case 14 : {eatFreq  = modVal(eatFreq, MinHuntAra[2], MaxHuntAra[2], (int)(mod*10));break;}
			default : break;
		}//switch
		
	}//modFlckVal
	
	//call after neighborhood, collision or avoidance radii have been modified
	private void fixNCVRads(boolean modC, boolean modV){
		if(modC){colRad = Math.min(Math.max(colRad,.05f*nghbrRad),.9f*nghbrRad);}//when neighbor rad modded	
		if(modV){velRad = Math.min(Math.max(colRad,velRad),.9f*nghbrRad);}//when col or neighbor rad modded
	}
	
	private int modVal(int val, float min, float max, int mod){	int oldVal = val;val += mod;if(!(MyMathUtils.inRange(val, min, max))){val = oldVal;} return val;}	
	private float modVal(float val, float min, float max, float mod){float oldVal = val;val += mod;	if(!(MyMathUtils.inRange(val, min, max))){val = oldVal;}return val;}
	
	
	//modify a particular flock force weight for a particular flock
	private void modFlkWt(int wIdx, float mod){
		float oldVal = this.wts[wIdx];
		this.wts[wIdx] += mod;
		if(!(MyMathUtils.inRange(wts[wIdx], MinWtAra[wIdx], MaxWtAra[wIdx]))){this.wts[wIdx] = oldVal;}		
	}

	//used to display flock-specific values
	public String[] getData(int numBoids){
		String res[] = new String[]{
		 "" + numBoids + " " + typeName + " Lim: V: ["+String.format("%.2f", (minVelMag))+"," + String.format("%.2f", (maxVelMag))+"] M ["+ String.format("%.2f", (massForType[0])) + "," + String.format("%.2f", (massForType[1]))+"]" ,
		 "Radius : Fl : "+String.format("%.2f",nghbrRad)+ " |  Avd : "+String.format("%.2f",colRad)+" |  VelMatch : "+ String.format("%.2f",velRad),
		// "           "+(nghbrRad > 10 ?(nghbrRad > 100 ? "":" "):"  ")+String.format("%.2f",nghbrRad)+" | "+(colRad > 10 ?(colRad > 100 ? "":" "):"  ")+String.format("%.2f",colRad)+" | "+(velRad > 10 ?(velRad > 100 ? "":" "):"  ")+ String.format("%.2f",velRad),
		 "Wts: Ctr |  Avoid | VelM | Wndr | AvPrd | Chase" ,
		 "     "+String.format("%.2f", wts[wFrcCtr])
				+"  |  "+String.format("%.2f", wts[wFrcAvd])
				+"  |  "+String.format("%.2f", wts[wFrcVel])
				+"  |  "+String.format("%.2f", wts[wFrcWnd])
				+"  |  "+String.format("%.2f", wts[wFrcAvdPred])
				+"  |  "+String.format("%.2f", wts[wFrcChsPrey]),
		"          		% success |  radius  |  # cycles.",
		"Spawning : "+(spawnPct > .1f ? "" : " ")+String.format("%.2f", (spawnPct*100))+" | "+(spawnRad > 10 ?(spawnRad > 100 ? "":" "):"  ")+String.format("%.2f", spawnRad)+" | "+spawnFreq,
		"Hunting   :  "+(killPct > .1f ? "" : " ")+String.format("%.2f", (killPct*100))+" | "+(predRad > 10 ?(predRad > 100 ? "":" "):"  ")+String.format("%.2f", predRad)+" | "+eatFreq,	
		" "};	
		return res;
	}
	
	public String toString(){
		String res = "Flock Vars for " + typeName + " \n";
//		for(int f=0;f<this.numFlocks;++f){
//			String[] flkStrs = getData(f);
//			for(int s=0; s<flkStrs.length; ++s){res+=flkStrs+"\n";}
//			res+="\tFlock info :\n";
//			//res+="\t"+flocks[f];			
//		}		
		return res;
	}
}
