package Boids2_PKG;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import processing.core.PImage;

public class myBoids3DWin extends myDispWindow {
	
	//idxs - need one per object
	public final static int
		gIDX_TimeStep 		= 0,
		gIDX_NumFlocks		= 1,
		gIDX_FlockToObs		= 2,
		gIDX_BoidToObs		= 3;

	//initial values - need one per object
	public float[] uiVals = new float[]{
			.1f,	
			1.0f,
			0,
			0
	};			//values of 8 ui-controlled quantities

	public final int numGUIObjs = uiVals.length;											//# of gui objects for ui
	
	public float timeStepMult = 1.0f;													//multiplier to modify timestep to make up for lag
	
	//private child-class flags - window specific
	public static final int 
			debugAnimIDX 		= 0,						//debug
			drawBoids			= 1,			//whether to draw boids or draw spheres (renders faster)
			clearPath			= 2,			//whether to clear each drawn boid, or to show path by keeping past drawn boids
			showVel			 	= 3,			//display vel values
			showFlkMbrs 		= 4,			//whether or not to show actual subflock members (i.e. neigbhors,colliders, preds, etc) when debugging
			attractMode 		= 5,			// whether we are in mouse attractor mode or repel mode
			//must stay within first 32 positions(single int) to make flocking control flag int easier (just sent first int)
			//flocking control flags
			flkCenter 			= 6,			// on/off : flock-centering
			flkVelMatch 		= 7,			// on/off : flock velocity matching
			flkAvoidCol 		= 8,			// on/off : flock collision avoidance	
			flkWander 			= 9,			// on/off : flock wandering		
			flkAvoidPred		= 10,			//turn on/off avoiding predators force and chasing prey force
			flkHunt				= 11,			//whether hunting is enabled
			flkHunger			= 12,			//can get hungry	
			flkSpawn			= 13,			//allow breeding
			useOrigDistFuncs 	= 14,
			useTorroid			= 15;
			//end must stay within first 32
	public static final int numPrivFlags = 16;

	public final int MaxNumBoids = 15000;		//max # of boids per flock
	public final int initNumBoids = 500;		//initial # of boids per flock
	
//	// structure holding boid flocks and the rendered versions of them - move to myRenderObj?
	//only 5 different flocks will display nicely on side menu
	public String[] flkNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters"};
	public float[] flkRadMults = {1.0f, 0.5f, 0.25f, 0.75f, 0.66f, 0.33f};
	public PImage[] flkSails;						//image sigils for sails
	public PImage blankSail;
	public final static int 
			privateer 	= 0,		
			pirate 		= 1,				
			corsair 	= 2,			
			marauder 	= 3,
			freebooter 	= 4;
	public final int MaxNumFlocks = flkNames.length;			//max # of flocks we'll support 
	//array of template objects to render
	//need individual array for each type of object, sphere (simplified) render object
	public myRenderObj[] rndrTmpl,//set depending on UI choice for complex rndr obj 
		boatRndrTmpl,
		//add more rendr obj arrays here
		sphrRndrTmpl;//simplified rndr obj (sphere)
	
	//current values
	public int numFlocks = 1;						
	public myBoidFlock[] flocks;
	public int curFlock = 0;
	//To print out flock vars values
	public int[] clrList;
	public ArrayList<Float[]> flkVarClkRes;
	//idxs of flock and boid to assign camera to if we are watching from "on deck"
	public int flockToWatch, boidToWatch;
	private float y45Off = 4.5f*yOff, flkMenuOffset;
	
	//idx of zone in currently modified flkVars value during drag - set to -1 on click release
	private int flkVarIDX, flkVarObjIDX;
	
	public myBoids3DWin(Boids_2 _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;
		trajFillClrCnst = Boids_2.gui_DarkCyan;		//override this in the ctor of the instancing window class
		trajStrkClrCnst = Boids_2.gui_Cyan;
		super.initThisWin(_canDrawTraj, true, false);
	}
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of privModFlgIdxs
				"Debugging", "Drawing Boids", "Showing Boid Path", "Showing Vel Vectors", "DBG : List Flk Mmbrs",
				"Mouse Click Attracts", 
				"Ctr Force ON", "Vel Match ON", "Col Avoid ON", "Wander ON",
				"Pred Avoid ON", "Hunting ON", "Hunger ON","Spawning ON",
				"Orig Funcs ON", "Tor Bnds ON"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Enable Debug","Drawing Spheres", "Hiding Boid Path", "Hiding Vel Vectors", "DBG : Hide Flk Mmbrs", 
				"Mouse Click Repels",
				"Ctr Force OFF", "Vel Match OFF", "Col Avoid OFF", "Wander OFF",
				"Pred Avoid OFF", "Hunting OFF", "Hunger OFF","Spawning OFF",
				"Orig Funcs OFF", "Tor Bnds OFF"
		};
		privModFlgIdxs = new int[]{
				debugAnimIDX, drawBoids, clearPath, showVel, showFlkMbrs, 
				attractMode,
				flkCenter, flkVelMatch, flkAvoidCol,  flkWander,  
				flkAvoidPred, flkHunt, flkHunger, flkSpawn, 
				useOrigDistFuncs, useTorroid
		};
		numClickBools = privModFlgIdxs.length;	
		initPrivBtnRects(0,numClickBools);
	}//initAllPrivBtns
	
	@Override
	protected void initMe() {
		//called once
		blankSail = pa.loadImage("BlankSail.jpg");

		clrList = new int[]{pa.gui_DarkGreen, pa.gui_DarkCyan, pa.gui_DarkRed, pa.gui_DarkBlue, pa.gui_DarkMagenta};
		initPrivFlags(numPrivFlags);
		//TODO set this to be determined by UI input (?)
		initSimpleBoids();
		initBoatBoids();
	
		setPrivFlags(drawBoids, true);
		setPrivFlags(attractMode, true);
		setPrivFlags(useTorroid, true);
		//this window is runnable
		setFlags(isRunnable, true);
		
		setFlockingOn();
		
		initFlocks();	
		flkMenuOffset = uiClkCoords[1] + uiClkCoords[3] - y45Off;	//495
	}//initMe
	
	//simple render objects - spheres
	private void initSimpleBoids(){
		sphrRndrTmpl = new mySphereRndrObj[MaxNumFlocks];
		for(int i=0; i<MaxNumFlocks; ++i){		sphrRndrTmpl[i] = new mySphereRndrObj(pa, this, i);	}
	}
	
	//initialize all instances of boat boid models - called 1 time
	private void initBoatBoids(){
		flkSails = new PImage[MaxNumFlocks];
		boatRndrTmpl = new myBoatRndrObj[MaxNumFlocks];
		for(int i=0; i<MaxNumFlocks; ++i){	
			flkSails[i] = pa.loadImage(flkNames[i]+".jpg");
			//build boat render object for each individual flock type
			boatRndrTmpl[i] = new myBoatRndrObj(pa, this, i);
		}
	}
	
	//turn on/off all flocking control boolean variables
	public void setFlockingOn(){setFlocking(true);}
	public void setFlockingOff(){setFlocking(false);}
	
	private void setFlocking(boolean val){
		setPrivFlags(flkCenter, val);
		setPrivFlags(flkVelMatch, val);
		setPrivFlags(flkAvoidCol, val);
		setPrivFlags(flkWander, val);
	}
	
	public void setHuntingOn(){setHunting(true);}
	public void setHuntingOff(){setHunting(false);}
	private void setHunting(boolean val){
		//should generally only be enabled if multiple flocks present
		//TODO set up to enable single flock to cannibalize
		setPrivFlags(flkAvoidPred, val);
		setPrivFlags(flkHunt, val);
		setPrivFlags(flkHunger, val);
		setPrivFlags(flkSpawn, val);		
	}//setHunting
	
	//TODO return appropriate complex render object array based on UI input input
	private myRenderObj[] getCurRndrObjAra(){
		return  boatRndrTmpl;		
	}
	
	//set up current flock configuration, based on ui selections
	private void initFlocks(){
		setHunting(numFlocks > 1);
		flockToWatch = 0;
		boidToWatch = 0;
		setMaxUIFlockToWatch();
		flocks = new myBoidFlock[numFlocks];
		flkVarClkRes = new ArrayList<Float[]>();
		for(int i =0; i<flocks.length; ++i){
			flocks[i] = (new myBoidFlock(pa,this,flkNames[i],initNumBoids,i));flocks[i].initFlock();
		}

		rndrTmpl = getCurRndrObjAra();
		for(int i =0; i<flocks.length; ++i){flocks[i].setPredPreyTmpl((((i+flocks.length)+1)%flocks.length), (((i+flocks.length)-1)%flocks.length), rndrTmpl[i], sphrRndrTmpl[i]);}	
	}

	public int getFlkFlagsInt(){		return privFlags[0];} //get first 32 flag settings
	
	public void drawCustMenuObjs(){
		pa.pushMatrix();				pa.pushStyle();		
		//all flock menu drawing within push mat call
		pa.translate(5,flkMenuOffset+yOff);
		for(int i =0; i<flocks.length; ++i){
			flocks[i].drawFlockMenu(i);
		}		
		pa.popStyle();					pa.popMatrix();		
	}
	
	//set camera to be on a boid in one of the flocks
	public void setBoidCam(float rx, float ry, float dz){
		flocks[flockToWatch].boidFlock.get(boidToWatch).setBoatCam(rx,ry,dz);
	}
	
	@Override
	//set flag values and execute special functionality for this sequencer
	public void setPrivFlags(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		privFlags[flIDX] = (val ?  privFlags[flIDX] | mask : privFlags[flIDX] & ~mask);
		switch(idx){
			case debugAnimIDX 			: {break;}//pa.outStr2Scr("debugAnimIDX " + val + " : " + getPrivFlags(idx) + "|"+ mask);  break;}		
			case drawBoids			    : {break;}//pa.outStr2Scr("drawBoids		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case clearPath			    : {
				pa.setFlags(pa.clearBKG, !val);//turn on or off background clearing in main window
				break;}
			case showVel			    : {break;}//pa.outStr2Scr("showVel		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case attractMode			: {break;}//pa.outStr2Scr("attractMode	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case showFlkMbrs 		    : {break;}//pa.outStr2Scr("showFlkMbrs 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkCenter 			    : {break;}//pa.outStr2Scr("flkCenter 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkVelMatch 		    : {break;}//pa.outStr2Scr("flkVelMatch 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkAvoidCol 		    : {break;}//pa.outStr2Scr("flkAvoidCol 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkWander 			    : {break;}//pa.outStr2Scr("flkWander 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkAvoidPred		    : {break;}//pa.outStr2Scr("flkAvoidPred	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkHunt			    : {break;}//pa.outStr2Scr("flkHunt		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkHunger			    : {break;}//pa.outStr2Scr("flkHunger		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkSpawn			    : {break;}//pa.outStr2Scr("flkSpawn		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case useOrigDistFuncs 	    : {//pa.outStr2Scr("useOrigDistFuncs " + val + " : " + getPrivFlags(idx) + "|"+ mask);
				if(flocks == null){break;}
				for(int i =0; i<flocks.length; ++i){
					flocks[i].flv.setDefaultWtVals(val);}
				break;
			}
			case useTorroid			    : { break;}		
		}		
	}//setPrivFlags		
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		guiMinMaxModVals = new double [][]{
			{0,1.0f,.0001f},					//timestep           		gIDX_TimeStep 	
			{1,MaxNumFlocks,1.0f},
			{0,numFlocks-1,1.0f},
			{0,initNumBoids-1,1.0f}
		};		//min max mod values for each modifiable UI comp	

		guiStVals = new double[]{
			uiVals[gIDX_TimeStep],		//timestep           		gIDX_TimeStep 	
			uiVals[gIDX_NumFlocks],
			uiVals[gIDX_FlockToObs],	
			uiVals[gIDX_BoidToObs]	
		};								//starting value
		
		guiObjNames = new String[]{
				"Time Step",
				"# of Flocks",
				"Flock To Watch",
				"Boid To Board"
		};								//name/label of component	
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{false, false, true},	//timestep           		gIDX_TimeStep 	
			{true, false, true},
			{true, true, true},
			{true, false, true}
		};						//per-object  list of boolean flags
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
	}
	//when flockToWatch changes, reset maxBoidToWatch value
	private void setMaxUIBoidToWatch(int flkIdx){guiObjs[gIDX_BoidToObs].setNewMax(flocks[flkIdx].boidFlock.size()-1);setUIWinVals(gIDX_BoidToObs);}	
	private void setMaxUIFlockToWatch(){guiObjs[gIDX_FlockToObs].setNewMax(numFlocks - 1);	setUIWinVals(gIDX_FlockToObs);}		
	@Override
	protected void setUIWinVals(int UIidx) {
		float val = (float)guiObjs[UIidx].getVal();
		//int ival = (int)val;
		switch(UIidx){		
		case gIDX_TimeStep 			:{
			if(val != uiVals[UIidx]){uiVals[UIidx] = val;}
			break;}
		case gIDX_NumFlocks			:{
			if(val != uiVals[UIidx]){uiVals[UIidx] = val; numFlocks = (int)val; initFlocks(); }
			break;}
		case gIDX_FlockToObs 			:{
			if(val != uiVals[UIidx]){uiVals[UIidx] = val; flockToWatch = (int)val; setMaxUIBoidToWatch(flockToWatch);}
			break;}
		case gIDX_BoidToObs 			:{
			if(val != uiVals[UIidx]){uiVals[UIidx] = val; boidToWatch = (int)val;}
			break;}

		default : {break;}
		}
	}

	//if any ui values have a string behind them for display
	@Override
	protected String getUIListValStr(int UIidx, int validx) {
		switch(UIidx){
			case gIDX_FlockToObs : {return flkNames[(validx % flkNames.length)]; }
			default : {break;}
		}
		return "";
	}
	
	public float getTimeStep(){
		return uiVals[gIDX_TimeStep] * timeStepMult;
	}

	@Override
	public void initDrwnTrajIndiv(){}
	
	public void setLights(){
		pa.ambientLight(102, 102, 102);
		pa.lightSpecular(204, 204, 204);
		pa.directionalLight(111, 111, 111, 0, 1, -1);	
	}
	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){
		
	}//drawTraj3D
	@Override
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//need to be here
//		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
		//pa.outStr2Scr("Current mouse loc in 3D : " + curMseLoc3D.toStrBrf() + "| scenectrvals : " + pa.sceneCtrVals[pa.sceneIDX].toStrBrf() +"| current look-at vector from mouse point : " + curMseLookVec.toStrBrf());
		pa.pushMatrix();pa.pushStyle();
		pa.translate(-pa.gridHalfDim.x, -pa.gridHalfDim.y, -pa.gridHalfDim.z);
		for(int i =0; i<flocks.length; ++i){flocks[i].drawBoids();}
		pa.popStyle();pa.popMatrix();
	}//drawMe
	
	@Override
	protected void simMe(float modAmtSec) {//run simulation
		//scale timestep to account for lag of rendering
		timeStepMult = modAmtSec * 30.0f;
		for(int i =0; i<flocks.length; ++i){flocks[i].clearOutBoids();}			//clear boid accumulators of neighbors, preds and prey  initAllMaps
		for(int i =0; i<flocks.length; ++i){flocks[i].initAllMaps();}
		if(getFlags(useOrigDistFuncs)){for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsOrigMultTH();}} 
		else {					for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsLinMultTH();}}
		for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidMovement();setMaxUIBoidToWatch(i);}	
	}
	@Override
	protected void stopMe() {	}	
	
	//debug function
	public void dbgFunc0(){
	}	
	public void dbgFunc1(){	
	}	
	public void dbgFunc2(){	
	}	
	public void dbgFunc3(){	
	}	
	public void dbgFunc4(){	
	}	
	@Override
	public void clickDebug(int btnNum){
		pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {	dbgFunc0();	break;}
			case 1 : {	dbgFunc1();	break;}
			case 2 : {	dbgFunc2();	break;}
			case 3 : {	dbgFunc3();	break;}
			default : {break;}
		}		
	}
	
	@Override
	public void hndlFileLoadIndiv(String[] vals, int[] stIdx) {
		
	}

	@Override
	public List<String> hndlFileSaveIndiv() {
		List<String> res = new ArrayList<String>();

		return res;
	}
	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	}
	@Override
	protected myPoint getMouseLoc3D(int mouseX, int mouseY){return pa.P(mouseX,mouseY,0);}
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	//alt key pressed handles trajectory
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean res = checkUIButtons(mouseX, mouseY);
		if(!res){//not in ui buttons, check if in flk vars region
			if((mouseX < uiClkCoords[2]) && (mouseY >= flkMenuOffset)){
				float relY = mouseY - flkMenuOffset;
				flkVarIDX = Math.round(relY) / 100;
				//pa.outStr2Scr("ui drag in UI coords : [" + mouseX + "," + mouseY + "; rel Y : " +relY + " ] flkIDX : " + flkIDX);
				if(flkVarIDX < numFlocks){	
					flkVarObjIDX = flocks[flkVarIDX].handleFlkMenuClick(mouseX, Math.round(relY) % 100);
					res = (flkVarIDX != -1);	
				} else {			flkVarIDX = -1;			}
			}			
		} 
		return res;
	}//hndlMouseClickIndiv

	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean res = false;
		if(!res){//not in ui buttons, check if in flk vars region
			if ((flkVarIDX != -1 ) && (flkVarObjIDX != -1)) {	res = flocks[flkVarIDX].handleFlkMenuDrag(flkVarObjIDX, mouseX, mouseY, pmouseX, pmouseY, mseBtn);		}
		}					 
		//pa.outStr2Scr("hndlMouseDragIndiv sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
//		if((privFlags[sphereSelIDX]) && (curSelSphere!="")){//pass drag through to selected sphere
//			//pa.outStr2Scr("sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
//			res = sphereCntls.get(curSelSphere).hndlMouseDragIndiv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D,curMseLookVec, mseDragInWorld);
//		}
		return res;
	}
	
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	

	@Override
	protected void hndlMouseRelIndiv() {
		//release always clears mod variable
		flkVarIDX = -1;
		flkVarObjIDX = -1;
		
	}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) {}
	@Override
	protected void closeMe() {}
	@Override
	protected void showMe() {}
}

