package Boids2_PKG;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Boids2_PKG.renderedObjs.myBoatRndrObj;
import Boids2_PKG.renderedObjs.myJFishRndrObj;
import Boids2_PKG.renderedObjs.mySphereRndrObj;
import Boids2_PKG.renderedObjs.base.myRenderObj;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.windowUI.base.base_UpdateFromUIData;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.my_procApplet;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import processing.core.PImage;

public class myBoids3DWin extends myDispWindow {
	
	//idxs - need one per object
	public final static int
		gIDX_TimeStep 		= 0,
		gIDX_NumFlocks		= 1,
		gIDX_BoidType		= 2,
		gIDX_FlockToObs		= 3,
		gIDX_BoidToObs		= 4;

	public final int numGUIObjs = 5;											//# of gui objects for ui
	
	public float timeStepMult = 1.0f;													//multiplier to modify timestep to make up for lag
	
	//private child-class flags - window specific
	public static final int 
			debugAnimIDX 		= 0,						//debug
			isMTCapableIDX 		= 1,			//whether this machine supports multiple threads
			drawBoids			= 2,			//whether to draw boids or draw spheres (renders faster)
			drawScaledBoids 	= 3,			//whether to draw boids scaled by their mass
			clearPath			= 4,			//whether to clear each drawn boid, or to show path by keeping past drawn boids
			showVel			 	= 5,			//display vel values
			showFlkMbrs 		= 6,			//whether or not to show actual subflock members (i.e. neigbhors,colliders, preds, etc) when debugging
			attractMode 		= 7,			// whether we are in mouse attractor mode or repel mode
			//must stay within first 32 positions(single int) to make flocking control flag int easier (just sent first int)
			//flocking control flags
			flkCenter 			= 8,			// on/off : flock-centering
			flkVelMatch 		= 9,			// on/off : flock velocity matching
			flkAvoidCol 		= 10,			// on/off : flock collision avoidance	
			flkWander 			= 11,			// on/off : flock wandering		
			flkAvoidPred		= 12,			//turn on/off avoiding predators force and chasing prey force
			flkHunt				= 13,			//whether hunting is enabled
			flkHunger			= 14,			//can get hungry	
			flkSpawn			= 15,			//allow breeding
			useOrigDistFuncs 	= 16,
			useTorroid			= 17,	
			flkCyclesFrc		= 18,			//the force these boids exert cycles with motion
			//end must stay within first 32
			modDelT				= 19,			//whether to modify delT based on frame rate or keep it fixed (to fight lag)
			viewFromBoid		= 20;			//whether viewpoint is from a boid's perspective or global
	
	private static final int numPrivFlags = 21;

	public final int MaxNumBoids = 15000;		//max # of boids per flock
	public final int initNumBoids = 500;		//initial # of boids per flock
	
//	// structure holding boid flocks and the rendered versions of them - move to myRenderObj?
	//only 5 different flocks will display nicely on side menu
	public String[] flkNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters"};
	public float[] flkRadMults = {1.0f, 0.5f, 0.25f, 0.75f, 0.66f, 0.33f};
	
	///////////
	//graphical constructs for pirate boids 
	///////////
	public PImage[] flkSails;						//image sigils for sails
	private final float bdgSizeX_base = 15, bdgSizeY = 15;			//badge size
	private float[] bdgSizeX;

	private myPointf[][] mnBdgBox;
	private static final myPointf[] mnUVBox = new myPointf[]{new myPointf(0,0,0),new myPointf(1,0,0),new myPointf(1,1,0),new myPointf(0,1,0)};
	
	public String[] boidTypeNames = new String[]{"Pirate Boats", "Jellyfish"};
	//whether this boid exhibits cyclic motion
	public boolean[] boidCyclesFrc = new boolean[]{false, true};
	
	public final int MaxNumFlocks = flkNames.length, numBoidTypes = boidTypeNames.length;			//max # of flocks we'll support, # of different kinds of boid species
	//array of template objects to render
	//need individual array for each type of object, sphere (simplified) render object
	private myRenderObj[] rndrTmpl,//set depending on UI choice for complex rndr obj 
		boatRndrTmpl,
		jellyFishRndrTmpl,
		//add more rendr obj arrays here
		sphrRndrTmpl;//simplified rndr obj (sphere)
	
	private ConcurrentSkipListMap<String, myRenderObj[]> cmplxRndrTmpls;
	
	//current values
	public int numFlocks = 1;						
	public myBoidFlock[] flocks;
	private myFlkVars[] flockVars;
	public int curFlock = 0;
	public ArrayList<Float[]> flkVarClkRes;
	//idxs of flock and boid to assign camera to if we are watching from "on deck"
	public int flockToWatch, boidToWatch;
	//offset to bottom of custom window menu 
	private float custMenuOffset;
	
	//idx of zone in currently modified flkVars value during drag - set to -1 on click release
	private int flkVarIDX, flkVarObjIDX;
	
	//threading constructions - allow map manager to own its own threading executor
	protected ExecutorService th_exec;	//to access multithreading - instance from calling program
	protected int numUsableThreads;		//# of threads usable by the application
	
	public myBoids3DWin(IRenderInterface _p, GUI_AppManager _AppMgr, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt) {
		super(_p, _AppMgr, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt);		
		super.initThisWin(false);
	}
	
	public ExecutorService getTh_Exec() {return th_exec;}
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray){
										//needs to be in order of privModFlgIdxs
		tmpBtnNamesArray.add(new Object[] {"Debugging", "Enable Debug", debugAnimIDX});
		tmpBtnNamesArray.add(new Object[] {"Drawing Boids", "Drawing Spheres", drawBoids});
		tmpBtnNamesArray.add(new Object[] {"Scale Boids' Sizes", "Boids Same Size", drawScaledBoids});
		tmpBtnNamesArray.add(new Object[] {"Showing Boid Path", "Hiding Boid Path", clearPath});
		tmpBtnNamesArray.add(new Object[] {"Showing Vel Vectors", "Hiding Vel Vectors", showVel});
		tmpBtnNamesArray.add(new Object[] {"DBG : List Flk Mmbrs", "DBG : Hide Flk Mmbrs", showFlkMbrs});
		tmpBtnNamesArray.add(new Object[] {"Mouse Click Attracts", "Mouse Click Repels", attractMode});
		tmpBtnNamesArray.add(new Object[] {"Ctr Force ON", "Ctr Force OFF", flkCenter});
		tmpBtnNamesArray.add(new Object[] {"Vel Match ON", "Vel Match OFF", flkVelMatch});
		tmpBtnNamesArray.add(new Object[] {"Col Avoid ON", "Col Avoid OFF", flkAvoidCol});
		tmpBtnNamesArray.add(new Object[] {"Wander ON", "Wander OFF", flkWander});
		tmpBtnNamesArray.add(new Object[] {"Pred Avoid ON", "Pred Avoid OFF", flkAvoidPred});
		tmpBtnNamesArray.add(new Object[] {"Hunting ON", "Hunting OFF", flkHunt, });
		tmpBtnNamesArray.add(new Object[] {"Hunger ON", "Hunger OFF", flkHunger});
		tmpBtnNamesArray.add(new Object[] {"Spawning ON", "Spawning OFF", flkSpawn, 	} );
		tmpBtnNamesArray.add(new Object[] {"Orig Funcs ON", "Orig Funcs OFF", useOrigDistFuncs});
		tmpBtnNamesArray.add(new Object[] {"Tor Bnds ON", "Tor Bnds OFF", useTorroid, });
		tmpBtnNamesArray.add(new Object[] {"Mod DelT By FRate", "Fixed DelT", modDelT});
		tmpBtnNamesArray.add(new Object[] {"Boid-eye View", "Global View", viewFromBoid});
		
		return numPrivFlags;
	
	}//initAllPrivBtns
	
	@Override
	protected void initMe() {
		//called once
		//initPrivFlags(numPrivFlags);
		//TODO set this to be determined by UI input (?)
		initSimpleBoids();
		initBoidRndrObjs();
		//want # of usable background threads.  Leave 2 for primary process (and potential draw loop)
		numUsableThreads = Runtime.getRuntime().availableProcessors() - 2;
		//set if this is multi-threaded capable - need more than 1 outside of 2 primary threads (i.e. only perform multithreaded calculations if 4 or more threads are available on host)
		setPrivFlags(isMTCapableIDX, numUsableThreads>1);
		
		//th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
		if(getPrivFlags(isMTCapableIDX)) {
			//th_exec = Executors.newFixedThreadPool(numUsableThreads+1);//fixed is better in that it will not block on the draw - this seems really slow on the prospect mapping
			th_exec = Executors.newCachedThreadPool();// this is performing much better even though it is using all available threads
		} else {//setting this just so that it doesn't fail somewhere - won't actually be exec'ed
			th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
		}
			
		initFlocks();	
		//flkMenuOffset = uiClkCoords[1] + uiClkCoords[3] - y45Off;	//495
		custMenuOffset = uiClkCoords[3];	//495
	}//initMe

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		//this window is runnable
		setFlags(isRunnable, true);
		//this window uses a customizable camera
		setFlags(useCustCam, true);
		return new int[] {drawBoids, attractMode, useTorroid};
	}
	@Override
	protected base_UpdateFromUIData buildUIDataUpdateObject() {
		return null;
	}

	@Override
	protected void buildUIUpdateStruct_Indiv(TreeMap<Integer, Integer> intValues, TreeMap<Integer, Float> floatValues,TreeMap<Integer, Boolean> boolValues) {}
	
	//simple render objects - spheres
	private void initSimpleBoids(){
		sphrRndrTmpl = new mySphereRndrObj[MaxNumFlocks];
		for(int i=0; i<MaxNumFlocks; ++i){		sphrRndrTmpl[i] = new mySphereRndrObj((my_procApplet) pa, this, i);	}
	}
	
	//initialize all instances of boat boid models - called 1 time
	private void initBoidRndrObjs(){
		cmplxRndrTmpls = new ConcurrentSkipListMap<String, myRenderObj[]> (); 
		flkSails = new PImage[MaxNumFlocks];
		boatRndrTmpl = new myBoatRndrObj[MaxNumFlocks];
		jellyFishRndrTmpl = new myJFishRndrObj[MaxNumFlocks];
		bdgSizeX = new float[MaxNumFlocks];
		mnBdgBox = new myPointf[MaxNumFlocks][];
		for(int i=0; i<MaxNumFlocks; ++i){	
			flkSails[i] = ((my_procApplet) pa).loadImage(flkNames[i]+".jpg");

			float scale = flkSails[i].width / (1.0f*flkSails[i].height);
			bdgSizeX[i] = bdgSizeX_base * scale; 

			mnBdgBox[i] = new myPointf[]{new myPointf(0,0,0),new myPointf(0,bdgSizeY,0),new myPointf(bdgSizeX[i],bdgSizeY,0),new myPointf(bdgSizeX[i],0,0)};
			
			//build boat render object for each individual flock type
			boatRndrTmpl[i] = new myBoatRndrObj(pa, this, i);			
			jellyFishRndrTmpl[i] = new myJFishRndrObj((my_procApplet) pa, this, i);
		}
		cmplxRndrTmpls.put(boidTypeNames[0], boatRndrTmpl);
		cmplxRndrTmpls.put(boidTypeNames[1], jellyFishRndrTmpl);
		rndrTmpl = cmplxRndrTmpls.get(boidTypeNames[0]);//start by rendering boats
	}
	
	//turn on/off all flocking control boolean variables
	public void setFlocking(boolean val){
		setPrivFlags(flkCenter, val);
		setPrivFlags(flkVelMatch, val);
		setPrivFlags(flkAvoidCol, val);
		setPrivFlags(flkWander, val);
	}
	
	public void setHunting(boolean val){
		//should generally only be enabled if multiple flocks present
		//TODO set up to enable single flock to cannibalize
		setPrivFlags(flkAvoidPred, val);
		setPrivFlags(flkHunt, val);
		setPrivFlags(flkHunger, val);
		setPrivFlags(flkSpawn, val);		
	}//setHunting

	
	//set up current flock configuration, based on ui selections
	private void initFlocks(){
		// Always start with flocking controls enabled
		setFlocking(true);
		// Only enable hunting if more than 1 flock exist
		setHunting(numFlocks > 1);
		flockToWatch = 0;
		boidToWatch = 0;
		setMaxUIFlockToWatch();
		flocks = new myBoidFlock[numFlocks];
		flockVars = new myFlkVars[numFlocks];
		flkVarClkRes = new ArrayList<Float[]>();
		for(int i =0; i<flocks.length; ++i){
			// ??? 
			// flockVars[i] = new myFlkVars(this, flkNames[i],(float)ThreadLocalRandom.current().nextDouble(0.65, 1.0));
			flockVars[i] = new myFlkVars(this, flkNames[i], flkRadMults[i]);
			flocks[i] = new myBoidFlock(pa,this,flockVars[i],initNumBoids,i);
			flocks[i].initFlock();
		}

		//rndrTmpl = getCurRndrObjAra();
		for(int i =0; i<flocks.length; ++i){flocks[i].setPredPreyTmpl((((i+flocks.length)+1)%flocks.length), (((i+flocks.length)-1)%flocks.length), rndrTmpl[i], sphrRndrTmpl[i]);}	
	}

	public int getFlkFlagsInt(){		return privFlags[0];} //get first 32 flag settings
	
	private void drawMenuBadge(myPointf[] ara, myPointf[] uvAra, int type) {
		pa.gl_beginShape(); 
		((my_procApplet)pa).texture(flkSails[type]);
		for(int i=0;i<ara.length;++i){	((my_procApplet)pa).vTextured(ara[i], uvAra[i].y, uvAra[i].x);} 
		pa.gl_endShape(true);
	}//
	
	public void drawFlockMenu(int i, int numBoids){
		String fvData[] = flockVars[i].getData(numBoids);
		//p.pushStyle();
		pa.translate(0,-bdgSizeY-6);
		drawMenuBadge(mnBdgBox[i],mnUVBox,i);
		pa.translate(bdgSizeX[i]+3,bdgSizeY+6);
		//p.setColorValFill(flkMenuClr);
		rndrTmpl[i].setMenuColor();
		pa.showText(fvData[0],0,-myDispWindow.yOff*.5f);pa.translate(0,myDispWindow.yOff*.75f);
		pa.translate(-bdgSizeX[i]-3,0);
		for(int j=1;j<fvData.length; ++j){pa.showText(fvData[j],0,-myDispWindow.yOff*.5f);pa.translate(0,myDispWindow.yOff*.75f);}	
		//p.popStyle();
	}//drawFlockMenu
	
	
	public void drawCustMenuObjs(){
		pa.pushMatState();	
		//all flock menu drawing within push mat call
		pa.translate(5,custMenuOffset+yOff);
		for(int i =0; i<flocks.length; ++i){
			drawFlockMenu(i, flocks[i].numBoids);
		}		
		pa.popMatState();
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
			case debugAnimIDX 			: {break;}//msgObj.dispInfoMessage(className, "xxx","debugAnimIDX " + val + " : " + getPrivFlags(idx) + "|"+ mask);  break;}		
			case drawBoids			    : {break;}//msgObj.dispInfoMessage(className, "xxx","drawBoids		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case drawScaledBoids		: {break;}//msgObj.dispInfoMessage(className, "xxx","drawScaledBoids		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}		
			case clearPath			    : {
				//TODO this needs to change how it works so that initialization doesn't call my_procApplet before it is ready
				//pa.setClearBackgroundEveryStep( !val);//turn on or off background clearing in main window
				break;}
			case showVel			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","showVel		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case attractMode			: {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","attractMode	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case showFlkMbrs 		    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","showFlkMbrs 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkCenter 			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkCenter 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkVelMatch 		    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkVelMatch 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkAvoidCol 		    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkAvoidCol 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkWander 			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkWander 	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkAvoidPred		    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkAvoidPred	 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkHunt			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkHunt		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkHunger			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkHunger		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case flkSpawn			    : {break;}//msgObj.dispInfoMessage(className, "setPrivFlags","flkSpawn		 " + val+ " : " + getPrivFlags(idx) + "|"+ mask );break;}
			case modDelT	 			: {break;}	//whether to keep delT fixed or to modify it based on frame rate (to fight lag)
			case flkCyclesFrc			: {break;}//whether or not current species scales force output cyclically with animation (pumping motion)
			case viewFromBoid		    : {
				super.setFlags(drawMseEdge,!val);//if viewing from boid, then don't show mse edge, and vice versa
				break;}	//whether viewpoint is from a boid's perspective or global
			case useOrigDistFuncs 	    : {//msgObj.dispInfoMessage(className, "setPrivFlags","useOrigDistFuncs " + val + " : " + getPrivFlags(idx) + "|"+ mask);
				if(flocks == null){break;}
				for(int i =0; i<flocks.length; ++i){
					flockVars[i].setDefaultWtVals(val);}
				break;
			}
			case useTorroid			    : { break;}		
			case isMTCapableIDX			: {break;}
		}		
	}//setPrivFlags		
	
	/**
	 * Return the current flock vars for the flock specified by flockIDX
	 * @param flockIDX
	 * @return
	 */	
	public myFlkVars getFlkVars(int flockIDX) {return flockVars[flockIDX];}
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	
		//build list select box values
		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
		
		tmpListObjVals.put(gIDX_BoidType, boidTypeNames);
		tmpListObjVals.put(gIDX_FlockToObs, flkNames);
		
		//tmpBtnNamesArray.add(new Object[]{"Building SOM","Build SOM ",buildSOMExe});
		//object array of elements of following format  : 
		//	the first element double array of min/max/mod values
		//	the 2nd element is starting value
		//	the 3rd elem is label for object
		//	the 4th element is boolean array of {treat as int, has list values, value is sent to owning window}
	
		tmpUIObjArray.put(gIDX_TimeStep,  new Object[] {new double[]{0,1.0f,.0001f}, .1, "Time Step", new boolean[]{false, false, true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_NumFlocks, new Object[] {new double[]{1,MaxNumFlocks,1.0f}, 1.0, "# of Flocks", new boolean[]{true, false, true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_BoidType,  new Object[] {new double[]{0,boidTypeNames.length-1,1.1f}, 0.0, "Flock Species", new boolean[]{true, true, true}} );   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_FlockToObs,new Object[] {new double[]{0,flkNames.length-1,1.1f}, 0.0, "Flock To Watch", new boolean[]{true, true, true}} );   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_BoidToObs, new Object[] {new double[]{0,initNumBoids-1,1.0f}, 0.0, "Boid To Board", new boolean[]{true, false, true}} );   				//uiTrainDataFrmtIDX                                                                        
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
		case gIDX_BoidType:{
			if(val != uiVals[UIidx]){
				uiVals[UIidx] = val; 
				int bIdx = (int)val;
				rndrTmpl = cmplxRndrTmpls.get(boidTypeNames[bIdx]);
				setPrivFlags( flkCyclesFrc, boidCyclesFrc[bIdx]);//set whether this flock cycles animation/force output
				initFlocks(); 
			}
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
	
	public double getTimeStep(){
		return uiVals[gIDX_TimeStep] * timeStepMult;
	}

	@Override
	public void initDrwnTrajIndiv(){}
	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCameraIndiv(float[] camVals){
		if (getPrivFlags(viewFromBoid)){	setBoidCam(rx,ry,dz);		}
		else {	
			pa.setCameraWinVals(camVals);//(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
			// puts origin of all drawn objects at screen center and moves forward/away by dz
			pa.translate(camVals[0],camVals[1],(float)dz); 
		    setCamOrient();	
		}
	}
	
	@Override
	protected void drawMe(float animTimeMod) {
//		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//need to be here
//		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
//		msgObj.dispInfoMessage(className, "drawMe","Current mouse loc in 3D : " + curMseLoc3D.toStrBrf() + "| scenectrvals : " + pa.sceneCtrVals[pa.sceneIDX].toStrBrf() +"| current look-at vector from mouse point : " + curMseLookVec.toStrBrf());
		pa.pushMatState();
		pa.translate(-AppMgr.gridHalfDim.x, -AppMgr.gridHalfDim.y, -AppMgr.gridHalfDim.z);
		for(int i =0; i<flocks.length; ++i){flocks[i].drawBoids();}
		pa.popMatState();
	}//drawMe
	

	@Override
	protected void drawRightSideInfoBarPriv(float modAmtMillis) {}

	@Override
	protected void drawOnScreenStuffPriv(float modAmtMillis) {}
	
	@Override
	protected boolean simMe(float modAmtSec) {//run simulation
		//scale timestep to account for lag of rendering if set in booleans		
		timeStepMult = getPrivFlags(modDelT) ?  modAmtSec * 30.0f : 1.0f;
		for(int i =0; i<flocks.length; ++i){flocks[i].clearOutBoids();}			//clear boid accumulators of neighbors, preds and prey 
		for(int i =0; i<flocks.length; ++i){flocks[i].initAllMaps();}
		boolean checkForce = (AppMgr.mouseIsClicked()) && (!AppMgr.shiftIsPressed());
		if(getFlags(useOrigDistFuncs)){for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsOrigMultTH(checkForce);}} 
		else {					for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsLinMultTH(checkForce);}}
		for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidMovement();}//setMaxUIBoidToWatch(i);}	
		for(int i =0; i<flocks.length; ++i){flocks[i].finalizeBoids();setMaxUIBoidToWatch(i);}	
		return false;
	}
	@Override
	protected void stopMe() {	}		

	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}

	//handle click in menu region - return idx of mod obj or -1
	private int handleFlkMenuClick(int mouseX, int mouseY){
		int vIdx = -1;
		//float mod = 0;
		int clkRow = (mouseY/12);//UI values modifiable in rows 1,3,5 and 6
		switch(clkRow){
		case 1 : {//radii 45 -110 | 110-183 | 183 ->
			if((mouseX >= 45) && (mouseX < 110)) {			vIdx = 0;} 
			else if((mouseX >= 110) && (mouseX < 185)) {	vIdx = 1;} 
			else if (mouseX >= 185) {						vIdx = 2;} 
			break;	}
		case 3 : {//weight vals : ctr : 10-45; av 50-90; velM 95-125; wander 130-165; avPred 170-200   ; chase 205->    ;
			if(10 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 3 + (mouseX - 10)/40;vIdx = (vIdx > 8 ? 8 : vIdx);}
			break;	}
		case 5 : {//spawn vals ( 60-85; 90-125; 130-165 )
			if(60 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 9 +  (mouseX - 60)/30;vIdx = (vIdx > 11 ? 11 : vIdx);}
			break;		}
		case 6 : {//hunt vals (  60-85; 90-135; 140-175 )
			if(60 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 12 + (mouseX - 60)/30;vIdx = (vIdx > 14 ? 14 : vIdx);}
			break;	}		
		default : {break;}
		}//switch			
		msgObj.dispInfoMessage(className,"handleFlkMenuClick","Flock vars click : [" + mouseX + "," + mouseY + "] row : " +clkRow + " obj idx : " + vIdx);	
		return vIdx;
	}//handleFlkMenuClick
	
	
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean res = false;
		if(!res){//not in ui buttons, check if in flk vars region
			if((mouseX < uiClkCoords[2]) && (mouseY >= custMenuOffset)){
				float relY = mouseY - custMenuOffset;
				flkVarIDX = Math.round(relY) / 100;
				//msgObj.dispInfoMessage(className, "hndlMouseClickIndiv","ui drag in UI coords : [" + mouseX + "," + mouseY + "; rel Y : " +relY + " ] flkIDX : " + flkVarIDX);
				if(flkVarIDX < numFlocks){	
					flkVarObjIDX = handleFlkMenuClick(mouseX, Math.round(relY) % 100);
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
			if ((flkVarIDX != -1 ) && (flkVarObjIDX != -1)) {	res = handleFlkMenuDrag(flkVarIDX, flkVarObjIDX, mouseX, mouseY, pmouseX, pmouseY, mseBtn);		}
		}					 
		return res;
	}
	
	//handle click in menu region - abs x, rel to start y
	private boolean handleFlkMenuDrag(int flkIDX, int flkValIDX, int mouseX, int mouseY, int pmx, int pmy, int mseBtn){
		boolean res = true;
		float mod = (mouseX-pmx) + (mouseY-pmy)*-5.0f;		
		flockVars[flkIDX].modFlkVal(flkValIDX, mod);		
		//msgObj.dispInfoMessage("myBoidFlock","handleFlkMenuDrag","Flock : " + name + " flkVar IDX : " + flkVarIDX + " mod amt : " + mod);		
		return res;
	}//handleFlkMenuClick
	
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

	@Override  
	protected void launchMenuBtnHndlr(int funcRow, int btn) {
		msgObj.dispMessage("myBoids3DWin","launchMenuBtnHndlr","Begin requested action", MsgCodes.info4);
		
		switch(funcRow) {
		case 0 : {
			msgObj.dispMessage("myBoids3DWin","launchMenuBtnHndlr","Clicked Btn row : Aux Func 1 | Btn : " + btn, MsgCodes.info4);
			switch(btn){
				case 0 : {						
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				default : {
					break;}
			}	
			break;}//row 1 of menu side bar buttons
		case 1 : {
			msgObj.dispMessage("myBoids3DWin","launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn, MsgCodes.info4);
			switch(btn){
				case 0 : {	
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					//test cosine function
					resetButtonState();
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons
		case 2 : {
			msgObj.dispMessage("myBoids3DWin","launchMenuBtnHndlr","Clicked Btn row : Aux Func 3 | Btn : " + btn, MsgCodes.info4);
			switch(btn){
				case 0 : {	
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					//test cosine function
					resetButtonState();
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons
		case 3 : {
			msgObj.dispMessage("myBoids3DWin","launchMenuBtnHndlr","Clicked Btn row : Aux Func 4 | Btn : " + btn, MsgCodes.info4);
			switch(btn){
				case 0 : {	
					resetButtonState();
					break;}
				case 1 : {	
					resetButtonState();
					break;}
				case 2 : {	
					resetButtonState();
					break;}
				case 3 : {	
					//test cosine function
					resetButtonState();
					break;}
				default : {
					break;}	
			}
			break;}//row 2 of menu side bar buttons
		}			
	}
	
	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {
		
		
	}
	@Override
	public final void handleSideMenuDebugSelEnable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable","Click Debug functionality on in " + name + " : btn : " + btn, MsgCodes.info4);
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
			default: {
				msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
				break;
			}
		}
		msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "End Debug functionality on selection.",MsgCodes.info4);
	}
	
	@Override
	public final void handleSideMenuDebugSelDisable(int btn) {
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable","Click Debug functionality off in " + name + " : btn : " + btn, MsgCodes.info4);
		switch (btn) {
			case 0: {				break;			}
			case 1: {				break;			}
			case 2: {				break;			}
			case 3: {				break;			}
			case 4: {				break;			}
			case 5: {				break;			}
		default: {
			msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
			break;
			}
		}
		msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "End Debug functionality off selection.",MsgCodes.info4);
	}


	@Override
	protected String[] getSaveFileDirNamesPriv() {
		return null;
	}

	@Override
	protected myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,mseLoc.z);}

	@Override
	protected void setVisScreenDimsPriv() {}

	@Override
	protected void setCustMenuBtnNames() {}

	@Override
	public void processTrajIndiv(myDrawnSmplTraj drawnTraj) {}

	@Override
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {}

	@Override
	public ArrayList<String> hndlFileSave(File file) {		
		return null;
	}
}//class myBoids3DWin

