package Boids2_PKG.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.UIObjectManager;
import base_UI_Objects.windowUI.base.IUIManagerOwner;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;


/**
 * struct-type class to hold flocking variables for a specific flock
 * @author John Turner
 *
 */
public class Boid_UIFlkVars implements IUIManagerOwner {	
	/**
	 * Gui-based application manager
	 */
	public static GUI_AppManager AppMgr;
	/**
	 * class name of instancing class
	 */
	protected final String className;
	
	/**
	 * Manager of all UI objects that make up this construct
	 */
	protected UIObjectManager uiMgr;
	
	/**
	 * Base_GUIObj that was clicked on for modification
	 */
	protected boolean msClickInUIObj;
	
	protected boolean objsModified;
	
	
	/**
	 * ID for this flock variable struct
	 */
	public final int ID;
	//Counter of how many windows are built in the application. Used to specify unique ID for each new window
	private static int objCnt = 0;
	
	
	private final float neighborMult = .5f;							//multiplier for neighborhood consideration against zone size - all rads built off this
	
	public float dampConst = .01f;							//multiplier for damping force, to slow boats down if nothing else acting on them
	
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
			defOrigWtAra = new float[]{5.0f, 12.0f, 7.0f, 3.5f, .5f, .1f},							//default array of weights for different forces using original calcs
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
	
	/**
	 * Idxs for the UI objects this construct holds
	 */
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
	public final static int numFlockUIObjs = 15;
	
	public Boid_UIFlkVars(GUI_AppManager _AppMgr, String _flockName, float _nRadMult, float _predRad) {
		ID = objCnt++;
		AppMgr = _AppMgr;
		className = this.getClass().getSimpleName();
		typeName = _flockName;
		msClickInUIObj = false;
		
		initFlockVals(_nRadMult, .05f, _predRad);
	}//ctor
	
	public float getInitMass(){return (float)(massForType[0] + (massForType[1] - massForType[0])*MyMathUtils.randomFloat());}
	
	/**
	 * 
	 * @param _initRadMult
	 * @param _initSpnPct
	 * @param _initPredRad
	 */
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
		
	/**
	 * UIObjectManager will call this.
	 */
	@Override
	public void initOwnerStateDispFlags() {
		//TODO
	}
	/**
	 * UI Manager access to this function to retrieve appropriate initial uiClkCoords.
	 * @return
	 */
	@Override
	public final float[] getOwnerParentWindowUIClkCoords() {
		//TODO
		return new float[0];
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
	public final int getID() {return ID;}
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
	public UIDataUpdater getUIDataUpdater() {return uiMgr.getUIDataUpdater();}

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
		setupGUIObjsAras(tmpUIObjArray,tmpListObjVals);	
	}

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
	protected final void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	
//		//build list select box values
//		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
//	
		tmpUIObjArray.put(fv_flkRadius, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{0.01f,1000.0f,0.01f}, nghbrRad, UI_Labels[fv_flkRadius]));
		tmpUIObjArray.put(fv_flkFrcWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.5f , UI_Labels[fv_flkFrcWeight]));
		tmpUIObjArray.put(fv_colAvoidRadius, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{0.01f,1000.0f,0.01f}, colRad, UI_Labels[fv_colAvoidRadius]));
		tmpUIObjArray.put(fv_colAvoidWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.75f, UI_Labels[fv_colAvoidWeight]));
		tmpUIObjArray.put(fv_velMatchRadius, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{0.01f,1000.0f,0.01f}, velRad, UI_Labels[fv_velMatchRadius]));
		tmpUIObjArray.put(fv_velMatchWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.5f, UI_Labels[fv_velMatchWeight]));
		tmpUIObjArray.put(fv_wanderFrcWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.5f, UI_Labels[fv_wanderFrcWeight]));
		tmpUIObjArray.put(fv_predAvoidWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.5f, UI_Labels[fv_predAvoidWeight]));
		tmpUIObjArray.put(fv_preyChaseWeight, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.0001f,1.0f,.0001f}, 0.1f, UI_Labels[fv_preyChaseWeight]));
		tmpUIObjArray.put(fv_matingRadius, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{0.01f,1000.0f,0.01f}, colRad, UI_Labels[fv_matingRadius]));
		tmpUIObjArray.put(fv_matingSuccessPct, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.1f,100.0f,.1f}, spawnPct, UI_Labels[fv_matingSuccessPct]));
		tmpUIObjArray.put(fv_matingFrequency, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{100.0f,10000.0f,10.0f}, spawnFreq, UI_Labels[fv_matingFrequency]));
		tmpUIObjArray.put(fv_huntingRadius, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{0.01f,100.0f,0.01f}, killRad, UI_Labels[fv_huntingRadius]));
		tmpUIObjArray.put(fv_huntingSuccessPct, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{.1f,100.0f,.1f}, killPct, UI_Labels[fv_huntingSuccessPct]));
		tmpUIObjArray.put(fv_huntingFrequency, uiMgr.uiObjInitAra_FloatMultiLine(new double[]{100.0f,10000.0f,10.0f}, eatFreq, UI_Labels[fv_huntingFrequency]));	
	}//setupGUIObjsAras
		
	
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
		// tmpBtnNamesArray.add(uiMgr.uiObjInitAra_Btn(new String[] {"<true string>", "<false string>"}, <button index>));
		return tmpBtnNamesArray.size();
	}

	@Override
	public int[] getOwnerFlagIDXsToInitToTrue() {	return new int[0];}

	/**
	 * Called by privFlags bool struct, to update uiUpdateData when boolean flags have changed
	 * @param idx
	 * @param val
	 */
	@Override
	public final void checkSetBoolAndUpdate(int idx, boolean val) {uiMgr.checkSetBoolAndUpdate(idx, val);	}
	
	/**
	 * These are called externally from execution code object to synchronize ui values that might change during execution
	 * @param idx of particular type of object
	 * @param value value to set
	 */
	@Override
	public final void updateBoolValFromExecCode(int idx, boolean value) {uiMgr.updateBoolValFromExecCode(idx, value);}
	/**
	 * These are called externally from execution code object to synchronize ui values that might change during execution
	 * @param idx of particular type of object
	 * @param value value to set
	 */
	@Override
	public final void updateIntValFromExecCode(int idx, int value) {uiMgr.updateIntValFromExecCode(idx, value);}
	/**
	 * These are called externally from execution code object to synchronize ui values that might change during execution
	 * @param idx of particular type of object
	 * @param value value to set
	 */
	@Override
	public final void updateFloatValFromExecCode(int idx, float value) {uiMgr.updateFloatValFromExecCode(idx, value);}
	

	@Override
	public void handleOwnerPrivFlags(int idx, boolean val, boolean oldVal) {}

	@Override
	public void handlePrivFlagsDebugMode(boolean val) {}
	
	///////////////////////////////////////////////////////
	/// Start mouse interaction
	
	/**
	 * Handle mouse interaction via a mouse click
	 * @param mouseX current mouse x on screen
	 * @param mouseY current mouse y on screen
	 * @param mseBtn which button is pressed : 0 is left, 1 is right
	 * @return whether a UI object was clicked in
	 */
	@Override
	public final boolean handleMouseClick(int mouseX, int mouseY, int mseBtn){
		boolean[] retVals = new boolean[] {false,false};
		msClickInUIObj = uiMgr.handleMouseClick(mouseX, mouseY, mseBtn, retVals);
		if (retVals[1]){objsModified = true;}
		if (retVals[0]){return true;}
		return false;
	}//handleMouseClick
		
	/**
	 * Handle mouse interaction via the clicked mouse drag
	 * @param mouseX current mouse x on screen
	 * @param mouseY current mouse y on screen
	 * @param pmouseX previous mouse x on screen
	 * @param pmouseY previous mouse y on screen
	 * @param mseDragInWorld vector of mouse drag in the world, for interacting with trajectories
	 * @param mseBtn what mouse btn is pressed
	 * @return whether a UI object has been modified via a drag action
	 */
	@Override
	public final boolean handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myVector mseDragInWorld, int mseBtn){
		int delX = (mouseX-pmouseX), delY = (mouseY-pmouseY);
		boolean shiftPressed = AppMgr.shiftIsPressed();
		boolean retVals[] = uiMgr.handleMouseDrag(delX, delY, shiftPressed);
		if (retVals[1]){objsModified = true;}
		if (retVals[0]){return true;}
		return false;
	}//handleMouseDrag
	
	/**
	 * Handle mouse interaction via the mouse moving over a UI object
	 * @param mouseX current mouse x on screen
	 * @param mouseY current mouse y on screen
	 * @return whether a UI object has the mouse pointer moved over it
	 */
	@Override
	public boolean handleMouseMove(int mouseX, int mouseY) {
		boolean uiObjMseOver = uiMgr.handleMouseMove(mouseX, mouseY);
		if (uiObjMseOver){return true;}
		return false;
	}
	
	/**
	 * Handle mouse interaction via the mouse wheel
	 * @param ticks
	 * @param mult amount to modify view based on sensitivity and whether shift is pressed or not
	 * @return whether a UI object has been modified via the mouse wheel
	 */
	@Override
	public final boolean handleMouseWheel(int ticks, float mult) {
		if (msClickInUIObj) {
			//modify object that was clicked in by mouse motion
			boolean retVals[] = uiMgr.handleMouseWheel(ticks, mult);
			if (retVals[1]){objsModified = true;}
			if (retVals[0]){return true;}		
		}
		return false;
	}//handleMouseWheel
	
	/**
	 * Handle mouse interactive when the mouse button is released - in general consider this the end of a mouse-driven interaction
	 */
	@Override
	public final void handleMouseRelease(){
		//TODO no buttons currently built to be cleared on future draw
		if(uiMgr.handleMouseRelease(objsModified)) {}
		msClickInUIObj = false;
		objsModified = false;
	}//handleMouseRelease
	
}//class myFlkVars 
