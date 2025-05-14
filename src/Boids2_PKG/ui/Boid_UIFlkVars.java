package Boids2_PKG.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import base_Math_Objects.MyMathUtils;
import base_UI_Objects.windowUI.base.IUIManagerOwner;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.base.GUIObj_Type;


/**
 * struct-type class to hold flocking variables for a specific flock
 * @author John Turner
 *
 */
public class Boid_UIFlkVars implements IUIManagerOwner {	
	/**
	 * class name of instancing class
	 */
	protected final String className;
	/**
	 * structure to facilitate communicating UI changes with functional code
	 */
	private UIDataUpdater uiUpdateData;		
	
	private final float neighborMult = .5f;							//multiplier for neighborhood consideration against zone size - all rads built off this
	
	public float dampConst = .01f;				//multiplier for damping force, to slow boats down if nothing else acting on them
	
	public float nghbrRad,									//radius of the creatures considered to be neighbors
				nghbrRadSq,
				colRad,										//radius of creatures to be considered for collision avoidance
				colRadSq,
				velRad,										//radius of creatures to be considered for velocity matching
				velRadSq,
				predRad,									//radius for creature to be considered for pred/prey (to start avoiding or to chase)
				predRadSq,
				spawnPct,									//% chance to reproduce given the boids breech the required radius
				spawnRad,									//distance to spawn * mass
				spawnRadSq,
				killPct,									//% chance to kill prey creature
				killRad,									//distance to kill * mass (distance required to make kill attempt)
				killRadSq;
	
	public int spawnFreq, 									//# of cycles that must pass before can spawn again
				eatFreq,								 	//# cycles w/out food until starve to death				
				canSprintCycles;							//cycles after eating where sufficiently full that boid can sprint
	
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
			MaxHuntAra = new float[]{.1f,10000,100000},					//max values for kill%, predation						
			MinHuntAra = new float[]{.0001f, 10, 100};	
	
	public final String typeName;
	
	public final String[] UI_Labels = new String[] {
		"Flock Rad",
		"Flock Frc Wt",
		"Col Avd Rad",
		"Col Avd Wt",
		"Vel Match Rad",
		"Vel Match Wt",
		"Wander Frc Wt",
		"Pred Avd Wt",
		"Prey Chase Wt",		
		"Spawn Rad",
		"Spawn %",
		"Spawn Freq",
		"Hunt Rad",
		"Hunt %",
		"Hunt Freq"			
	};
	
	public final static int 
		fv_flkRadius = 0,
		fv_flkFrcWeight = 1,
		fv_colAvoidRadius = 2,
		fv_colAvoidWeight = 3,
		fv_velMatchRadius = 4,
		fv_velMatchWeight = 5,
		fv_wanderFrcWeight = 6,
		fv_predAvoidWeight = 7,
		fv_preyChaseWeight = 8,
		fv_matingRadius = 9,
		fv_matingSuccessPct = 10,
		fv_matingFrequency = 11,
		fv_huntingRadius = 12,
		fv_huntingSuccessPct = 13,
		fv_huntingFrequency = 14;
	
	public Boid_UIFlkVars(String _flockName, float _nRadMult, float _predRad) {
		className = this.getClass().getSimpleName();
		typeName = _flockName;
		
		initFlockVals(_nRadMult, .05f, _predRad);
	}//ctor
	
	public float getInitMass(){return (float)(massForType[0] + (massForType[1] - massForType[0])*ThreadLocalRandom.current().nextFloat());}
	
	//set initial values
	private void initFlockVals(float _initRadMult, float _initSpnPct, float _initPredRad){
		//radius to avoid pred/find prey
		predRad = _initPredRad;		
		predRadSq = predRad * predRad;
		nghbrRadMax = predRad*neighborMult;
		nghbrRad = nghbrRadMax*_initRadMult;
		nghbrRadSq = nghbrRad *nghbrRad;
		colRad  = nghbrRad*.1f;
		colRadSq = colRad*colRad;
		velRad  = nghbrRad*.5f; 	
		velRadSq = velRad * velRad;
		//weight multiplier for forces - centering, avoidance, velocity matching and wander
		spawnPct = _initSpnPct;		//% chance to reproduce given the boids breech the required radius
		spawnRad = colRad;			//distance to spawn 
		spawnRadSq = spawnRad * spawnRad;
		spawnFreq = 500; 		//# of cycles that must pass before can spawn again
		//required meal time
		eatFreq = 500; 			//# cycles w/out food until starve to death
		setCanSprintCycles();
		killRad = 1;						//radius to kill * mass
		killRadSq = killRad * killRad;
		killPct = .01f;				//% chance to kill prey creature

		setDefaultWtVals(true);//init to true
		massForType = new float[]{2.0f*_initRadMult,4.0f*_initRadMult}; //_initRadMult should vary from 1.0 to .25
		maxFrcs = new float[]{100,200,100,10,400,20};		//maybe scale forces?
		minVelMag = maxVelMag*.0025f;
	}
	
	private final Object[] uiObjInitAra(double[] minMaxMod, double initVal, int idx) {
		// idx 0: value is sent to owning window,  
		// idx 1: value is sent on any modifications (while being modified, not just on release), 
		// idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
		
		boolean[] cfgFlagsAra = new boolean[] {true, false, false};
		// idx 0: whether multi-line(stacked) or not
		// idx 1: if true, build prefix ornament
		// idx 2: if true and prefix ornament is built, make it the same color as the text fill color. 
		boolean[] formatFlagsAra = new boolean[] {true, true, true};
		
		return new Object[] {minMaxMod, initVal, UI_Labels[idx], GUIObj_Type.FloatVal, cfgFlagsAra, formatFlagsAra};
	}
	
	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	{value is sent to owning window, 
	 *           	value is sent on any modifications (while being modified, not just on release), 
	 *           	changes to value must be explicitly sent to consumer (are not automatically sent)}    
	 * @param tmpListObjVals
	 */
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	
//		//build list select box values
//		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
//	
		tmpUIObjArray.put(fv_flkRadius, uiObjInitAra(new double[]{0.01f,1000.0f,0.01f}, nghbrRad, fv_flkRadius));
		tmpUIObjArray.put(fv_flkFrcWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.5f , fv_flkFrcWeight));
		tmpUIObjArray.put(fv_colAvoidRadius, uiObjInitAra(new double[]{0.01f,1000.0f,0.01f}, colRad, fv_colAvoidRadius));
		tmpUIObjArray.put(fv_colAvoidWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.75f, fv_colAvoidWeight));
		tmpUIObjArray.put(fv_velMatchRadius, uiObjInitAra(new double[]{0.01f,1000.0f,0.01f}, velRad, fv_velMatchRadius));
		tmpUIObjArray.put(fv_velMatchWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.5f, fv_velMatchWeight));
		tmpUIObjArray.put(fv_wanderFrcWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.5f, fv_wanderFrcWeight));
		tmpUIObjArray.put(fv_predAvoidWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.5f, fv_predAvoidWeight));
		tmpUIObjArray.put(fv_preyChaseWeight, uiObjInitAra(new double[]{.0001f,1.0f,.0001f}, 0.1f, fv_preyChaseWeight));
		tmpUIObjArray.put(fv_matingRadius, uiObjInitAra(new double[]{0.01f,1000.0f,0.01f}, colRad, fv_matingRadius));
		tmpUIObjArray.put(fv_matingSuccessPct, uiObjInitAra(new double[]{.1f,100.0f,.1f}, spawnPct, fv_matingSuccessPct));
		tmpUIObjArray.put(fv_matingFrequency, uiObjInitAra(new double[]{100.0f,10000.0f,10.0f}, spawnFreq, fv_matingFrequency));
		tmpUIObjArray.put(fv_huntingRadius, uiObjInitAra(new double[]{0.01f,100.0f,0.01f}, killRad, fv_huntingRadius));
		tmpUIObjArray.put(fv_huntingSuccessPct, uiObjInitAra(new double[]{.1f,100.0f,.1f}, killPct, fv_huntingSuccessPct));
		tmpUIObjArray.put(fv_huntingFrequency, uiObjInitAra(new double[]{100.0f,10000.0f,10.0f}, eatFreq, fv_huntingFrequency));

	
	}

	//if set default weight mults based on whether using force calcs based on original inverted distance functions or linear distance functions
	public void setDefaultWtVals(boolean useOrig){	
		float[] srcAra = (useOrig ? defOrigWtAra: defWtAra);
		wts = new float[srcAra.length]; 
		System.arraycopy( srcAra, 0, wts, 0, srcAra.length );				
	}
	

	/**
	 * handles all modification of flock values from ui - wIdx is manufactured based on location in ui click area
	 * @param wIdx
	 * @param mod
	 */
	public void modFlkVal(int wIdx, float mod){
		//win.getMsgObj().dispInfoMessage("myFlkVars","modFlkVal","Attempt to modify flock : " + flock.name + " value : " + wIdx + " by " + mod);
		if(wIdx==-1){return;}
		switch(wIdx){
		//hierarchy - if neighbor then col and vel, if col then 
			case 0  : {//flock radius
				nghbrRad = modVal(nghbrRad, .1f*nghbrRadMax, nghbrRadMax, mod);
				fixNCVRads(true, true);
				nghbrRadSq = nghbrRad * nghbrRad;			
				break;}			
			case 1  : {
				colRad = modVal(colRad, .05f*nghbrRad, .9f*nghbrRadMax, mod);
				fixNCVRads(false, true);
				colRadSq = colRad * colRad;
				break;}	//avoid friends radius
			case 2  : {
				velRad = modVal(velRad, colRad, .9f*nghbrRadMax, mod);
				velRadSq = velRad * velRad;
				break;}			//vel match radius			
			case 3  : 						//3-8 are the 6 force weights
			case 4  : 
			case 5  : 
			case 6  : 
			case 7  : 
			case 8  : {modFlkWt(wIdx-3,mod*.01f);break;}						//3-8 are the 6 force weights
			
			case 9  : {spawnPct = modVal(spawnPct, MinSpAra[0], MaxSpAra[0], mod*.001f); break;}
			case 10 : {
				spawnRad = modVal(spawnRad, MinSpAra[1], MaxSpAra[1], mod); 
				spawnRadSq = spawnRad*spawnRad; 
				break;}
			case 11 : {spawnFreq = modVal(spawnFreq, MinSpAra[2], MaxSpAra[2], (int)(mod*10));break;}
			case 12 : {killPct = modVal(killPct, MinHuntAra[0], MaxHuntAra[0], mod*.0001f); break;}
			case 13 : {
				predRad = modVal(predRad, MinHuntAra[1], MaxHuntAra[1], mod);
				predRadSq = predRad * predRad;
				break;}
			case 14 : {eatFreq = modVal(eatFreq, MinHuntAra[2], MaxHuntAra[2], (int)(mod*10)); setCanSprintCycles();break;}
			default : break;
		}//switch
		
	}//modFlckVal
	
	//call after neighborhood, collision or avoidance radii have been modified
	private void fixNCVRads(boolean modC, boolean modV){
		if(modC){colRad = MyMathUtils.min(MyMathUtils.max(colRad,.05f*nghbrRad),.9f*nghbrRad);}//when neighbor rad modded	
		if(modV){velRad = MyMathUtils.min(MyMathUtils.max(colRad,velRad),.9f*nghbrRad);}//when col or neighbor rad modded
	}
	
	private int modVal(int val, float min, float max, int mod){	int oldVal = val;val += mod;if(!(MyMathUtils.inRange(val, min, max))){val = oldVal;} return val;}	
	private float modVal(float val, float min, float max, float mod){float oldVal = val;val += mod;	if(!(MyMathUtils.inRange(val, min, max))){val = oldVal;}return val;}
	
	
	/**
	 * modify a particular flock force weight for a particular flock
	 * @param wIdx
	 * @param mod
	 */
	private void modFlkWt(int wIdx, float mod){
		float oldVal = this.wts[wIdx];
		this.wts[wIdx] += mod;
		if(!(MyMathUtils.inRange(wts[wIdx], MinWtAra[wIdx], MaxWtAra[wIdx]))){this.wts[wIdx] = oldVal;}		
	}
	private void setCanSprintCycles() {canSprintCycles = (int) (.25f * eatFreq);}

	private String[] getInfoForFlkVars(String _prefix) {
		String res[] = new String[]{
				 _prefix + typeName + " Lim: V: ["+String.format("%.2f", (minVelMag))+"," + String.format("%.2f", (maxVelMag))+"] M ["+ String.format("%.2f", (massForType[0])) + "," + String.format("%.2f", (massForType[1]))+"]" ,
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
	
	/**
	 * used to display flock-specific values
	 * @param numBoids number of boids of this flock currently existing
	 * @return
	 */
	public String[] getData(int numBoids){
		return getInfoForFlkVars("" + numBoids + " ");
	}
	
	@Override
	public String toString(){
		String[] resAra = getInfoForFlkVars("Flock Vars for ");
		return Arrays.toString(resAra);
	}

	@Override
	public String getClassName() {return className;	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	@Override
	public void updateOwnerCalcObjUIVals() {
		//
		
	}
	/**
	 * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	public void setUI_OwnerIntValsCustom(int UIidx, int ival, int oldVal) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	public void setUI_OwnerFloatValsCustom(int UIidx, float val, float oldVal) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Build flkVars UIDataUpdater instance for application
	 * @return
	 */	
	@Override
	public UIDataUpdater buildOwnerUIDataUpdateObject() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Retrieve the Owner's UIDataUpdater
	 * @return
	 */
	@Override
	public UIDataUpdater getUIDataUpdater() {return uiUpdateData;}

	/**
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *           the 6th element is a boolean array of format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 * @param tmpListObjVals
	 */
	@Override
	public void setupOwnerGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray,
			TreeMap<Integer, String[]> tmpListObjVals) {
		// Build all UI objects in here.
		
		// TODO Auto-generated method stub
		
	}
	/**
	 * Build button descriptive arrays : each object array holds true label, false label, and idx of button in owning child class
	 * this must return count of -all- booleans managed by privFlags, not just those that are interactive buttons (some may be 
	 * hidden to manage booleans that manage or record state)
	 * @param tmpBtnNamesArray ArrayList of Object arrays to be built containing all button definitions. 
	 * @return count of -all- booleans to be managed by privFlags
	 */
	@Override
	public int initAllOwnerUIButtons(ArrayList<Object[]> tmpBtnNamesArray) {
		// Build all UI Buttons in here; return number of buttons
		return 0;
	}

	@Override
	public int[] getOwnerFlagIDXsToInitToTrue() {
		return new int[0];
	}

	@Override
	public void checkSetBoolAndUpdate(int idx, boolean val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleOwnerPrivFlags(int idx, boolean val, boolean oldVal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlePrivFlagsDebugMode(boolean val) {
		// TODO Auto-generated method stub
		
	}
}//class myFlkVars 
