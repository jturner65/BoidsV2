package Boids2_PKG.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Boids2_PKG.flocks.myBoidFlock;
import Boids2_PKG.flocks.myFlkVars;
import Boids2_PKG.renderedObjs.Boat_RenderObj;
import Boids2_PKG.renderedObjs.JFish_RenderObj;
import Boids2_PKG.renderedObjs.Sphere_RenderObj;
import Boids2_PKG.renderedObjs.base.Base_RenderObj;
import Boids2_PKG.ui.myBoidsUIDataUpdater;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Type;
import base_Utils_Objects.io.messaging.MsgCodes;
import base_Utils_Objects.tools.flags.Base_BoolFlags;
import processing.core.PImage;

public abstract class Base_BoidsWindow extends Base_DispWindow {
	
	/**
	 * idxs - need one per ui object
	 */
	public final static int
		gIDX_TimeStep 		= 0,
		gIDX_NumFlocks		= 1,
		gIDX_BoidType		= 2,
		gIDX_FlockToObs		= 3,
		gIDX_ModNumBoids	= 4,
		gIDX_BoidToObs		= 5;

	public static final int numBaseGUIObjs = 6;											//# of gui objects for ui
	
	/**
	 * private child-class flags - window specific
	 */
	public static final int 
			//debug is 0
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
	
	protected static final int numBasePrivFlags = 21;

	public final int MaxNumBoids = 15000;		//max # of boids per flock
	protected final int initNumBoids = 500;		//initial # of boids per flock
	
	/**
	 * All flocks of boids
	 */
	public myBoidFlock[] flocks;
	
	/**
	 * Array of each flock's specific flocking vars, set via UI
	 */
	protected myFlkVars[] flockVars;

	
//	// structure holding boid flocks and the rendered versions of them - move to myRenderObj?
	//only 5 different flocks will display nicely on side menu
	protected String[] flkNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters"};
	protected float[] flkRadMults = {1.0f, 0.5f, 0.25f, 0.75f, 0.66f, 0.33f};
	
	///////////
	//graphical constructs for boids 
	///////////
	/**
	 * image sigils for sails
	 */
	protected PImage[] flkSails;				
	/**
	 * badge size
	 */
	protected final float bdgSizeX_base = 15, bdgSizeY = 15;
	protected float[] bdgSizeX;

	protected myPointf[][] mnBdgBox;
	protected static final myPointf[] mnUVBox = new myPointf[]{new myPointf(0,0,0),new myPointf(1,0,0),new myPointf(1,1,0),new myPointf(0,1,0)};
	
	protected String[] boidTypeNames = new String[]{"Pirate Boats", "Jellyfish"};
	//whether this boid exhibits cyclic motion
	protected boolean[] boidCyclesFrc = new boolean[]{false, true};
	
	protected final int MaxNumFlocks = flkNames.length;			//max # of flocks we'll support
	//array of template objects to render
	//need individual array for each type of object, sphere (simplified) render object
	protected Base_RenderObj[] rndrTmpl,//set depending on UI choice for complex rndr obj 
		boatRndrTmpl,
		jellyFishRndrTmpl,
		//add more rendr obj arrays here
		sphrRndrTmpl;//simplified rndr obj (sphere)
	
	protected ConcurrentSkipListMap<String, Base_RenderObj[]> cmplxRndrTmpls;
	
	/**
	 * multiplier to modify timestep to make up for lag
	 */
	protected float timeStepMult = 1.0f;
	
	//current/initial values
	protected double curTimeStep = .1;
	protected int numFlocks = 1;
	
	
	//idxs of flock and boid to assign camera to if we are watching from "on deck"
	protected int flockToWatch, boidToWatch;
	//offset to bottom of custom window menu 
	protected float custMenuOffset;
	
	//idx of zone in currently modified flkVars value during drag - set to -1 on click release
	protected int flkVarIDX, flkVarObjIDX;
	
	//threading constructions - allow map manager to own its own threading executor
	protected ExecutorService th_exec;	//to access multithreading - instance from calling program
	protected int numUsableThreads;		//# of threads usable by the application
	
	public Base_BoidsWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx, int _flagIdx) {
		super(_p, _AppMgr, _winIdx, _flagIdx);	
	}
	
	public ExecutorService getTh_Exec() {return th_exec;}
	
	/**
	 * initialize all private-flag based UI buttons here - called by base class
	 */
	@Override
	public int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray){
										//needs to be in order of privModFlgIdxs
		tmpBtnNamesArray.add(new Object[] {"Debugging", "Enable Debug", Base_BoolFlags.debugIDX});
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
		
		return initAllPrivBtns_Indiv(tmpBtnNamesArray);
	
	}//initAllPrivBtns
	
	/**
	 * Instance-specific button init
	 * @param tmpBtnNamesArray
	 * @return size of tmpBtnNamesArray
	 */
	protected abstract int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	
	/**
	 * Initialize any UI control flags appropriate for all boids window applications
	 */
	@Override
	protected final void initDispFlags() {
		//this window is runnable
		dispFlags.setIsRunnable(true);
		//this window uses a customizable camera
		dispFlags.setUseCustCam(true);		
		initDispFlags_Indiv();
	}
	/**
	 * Initialize any UI control flags appropriate for specific instanced boids window
	 */
	protected abstract void initDispFlags_Indiv();
	
	@Override
	protected void initMe() {
		//called once
		//TODO set this to be determined by UI input (?)
		initSimpleBoids();
		initBoidRndrObjs();
		//want # of usable background threads.  Leave 2 for primary process (and potential draw loop)
		numUsableThreads = Runtime.getRuntime().availableProcessors() - 2;
		//set if this is multi-threaded capable - need more than 1 outside of 2 primary threads (i.e. only perform multithreaded calculations if 4 or more threads are available on host)
		privFlags.setFlag(isMTCapableIDX, numUsableThreads>1);
		
		//th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
		if(privFlags.getFlag(isMTCapableIDX)) {
			//th_exec = Executors.newFixedThreadPool(numUsableThreads+1);//fixed is better in that it will not block on the draw - this seems really slow on the prospect mapping
			th_exec = Executors.newCachedThreadPool();// this is performing much better even though it is using all available threads
		} else {//setting this just so that it doesn't fail somewhere - won't actually be exec'ed
			th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
		}
			
		initFlocks();	
		//flkMenuOffset = uiClkCoords[1] + uiClkCoords[3] - y45Off;	//495
		custMenuOffset = uiClkCoords[3];	//495
		initMe_IndivPost();
	}//initMe
	
	/**
	 * Instance specific init after base class init is finished
	 */
	protected abstract void initMe_IndivPost();
	

	@Override
	protected int[] getFlagIDXsToInitToTrue() {
		return getFlagIDXsToInitToTrue_Indiv(new int[] {drawBoids, attractMode, useTorroid});
	}
	
	/**
	 * Add instance-specific private flags to init to true to those from base class
	 * @param baseFlags base class flags to init to true - add to this array
	 * @return array of base and instance class flags to init to true
	 */
	protected abstract int[] getFlagIDXsToInitToTrue_Indiv(int[] baseFlags);
	
	/**
	 * This function would provide an instance of the override class for base_UpdateFromUIData, which would
	 * be used to communicate changes in UI settings directly to the value consumers.
	 */
	@Override
	protected UIDataUpdater buildUIDataUpdateObject() {
		return new myBoidsUIDataUpdater(this);
	}
	/**
	 * This function is called on ui value update, to pass new ui values on to window-owned consumers
	 */
	protected final void updateCalcObjUIVals() {}
	
	/**
	 * simple render object templates - spheres
	 */
	protected void initSimpleBoids(){
		sphrRndrTmpl = new Sphere_RenderObj[MaxNumFlocks];
		for(int i=0; i<MaxNumFlocks; ++i){		sphrRndrTmpl[i] = new Sphere_RenderObj((my_procApplet) pa, this, i);	}
	}
	
	/**
	 * initialize all instances of boat boid models/templates - called 1 time
	 */
	protected void initBoidRndrObjs(){
		cmplxRndrTmpls = new ConcurrentSkipListMap<String, Base_RenderObj[]> (); 
		flkSails = new PImage[MaxNumFlocks];
		boatRndrTmpl = new Boat_RenderObj[MaxNumFlocks];
		jellyFishRndrTmpl = new JFish_RenderObj[MaxNumFlocks];
		bdgSizeX = new float[MaxNumFlocks];
		mnBdgBox = new myPointf[MaxNumFlocks][];
		for(int i=0; i<MaxNumFlocks; ++i){	
			flkSails[i] = ((my_procApplet) pa).loadImage(flkNames[i]+".jpg");

			float scale = flkSails[i].width / (1.0f*flkSails[i].height);
			bdgSizeX[i] = bdgSizeX_base * scale; 

			mnBdgBox[i] = new myPointf[]{new myPointf(0,0,0),new myPointf(0,bdgSizeY,0),new myPointf(bdgSizeX[i],bdgSizeY,0),new myPointf(bdgSizeX[i],0,0)};
			
			//build boat render object for each individual flock type
			boatRndrTmpl[i] = new Boat_RenderObj(pa, this, i);			
			jellyFishRndrTmpl[i] = new JFish_RenderObj((my_procApplet) pa, this, i);
		}
		cmplxRndrTmpls.put(boidTypeNames[0], boatRndrTmpl);
		cmplxRndrTmpls.put(boidTypeNames[1], jellyFishRndrTmpl);
		rndrTmpl = cmplxRndrTmpls.get(boidTypeNames[0]);//start by rendering boats
	}
	
	/**
	 * turn on/off all flocking control boolean variables
	 * @param val
	 */
	public final void setFlocking(boolean val){
		privFlags.setFlag(flkCenter, val);
		privFlags.setFlag(flkVelMatch, val);
		privFlags.setFlag(flkAvoidCol, val);
		privFlags.setFlag(flkWander, val);
	}
	/**
	 * turn on/off all hunting control boolean variables
	 * @param val
	 */
	public final void setHunting(boolean val){
		//should generally only be enabled if multiple flocks present
		//TODO set up to enable single flock to cannibalize
		privFlags.setFlag(flkAvoidPred, val);
		privFlags.setFlag(flkHunt, val);
		privFlags.setFlag(flkHunger, val);
		privFlags.setFlag(flkSpawn, val);		
	}//setHunting

	
	/**
	 * set up current flock configuration, based on ui selections
	 */
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
		for(int i =0; i<flocks.length; ++i){
			// ??? 
			// flockVars[i] = new myFlkVars(this, flkNames[i],(float)ThreadLocalRandom.current().nextDouble(0.65, 1.0));
			flockVars[i] = new myFlkVars(flkNames[i], flkRadMults[i]);
			flocks[i] = new myBoidFlock(pa,this,flockVars[i],initNumBoids,i);
			flocks[i].initFlock();
		}

		int predIDX, preyIDX;
		for(int i =0; i<flocks.length; ++i){
			predIDX = ((i+1)%flocks.length);
			preyIDX = (((i+flocks.length)-1)%flocks.length);
			flocks[i].setPredPreyTmpl(flocks[predIDX], flocks[preyIDX], rndrTmpl[i], sphrRndrTmpl[i]);
		}	
	}//initFlocks
	
	/**
	 * Retrieve the first 32 flag bits from the privFlags structure, used to hold all the flocking menu flags
	 * @return
	 */
	public int getFlkFlagsInt(){		return privFlags.getFlagsAsInt(0);} //get first 32 flag settings
	
	public void drawMenuBadge(myPointf[] ara, myPointf[] uvAra, int type) {
		pa.gl_beginShape(); 
		((my_procApplet)pa).texture(flkSails[type]);
		for(int i=0;i<ara.length;++i){	((my_procApplet)pa).vTextured(ara[i], uvAra[i].y, uvAra[i].x);} 
		pa.gl_endShape(true);
	}//
	
	private static final float fvDataTxtStY = -Base_DispWindow.yOff*.5f;
	private static final float fvDataNewLineY = Base_DispWindow.yOff*.75f;
	
	public void drawFlockMenu(int i, int numBoids){
		pa.translate(0,-bdgSizeY-6);
		drawMenuBadge(mnBdgBox[i],mnUVBox,i);
		pa.translate(bdgSizeX[i]+3,bdgSizeY+6);
		//p.setColorValFill(flkMenuClr);
		rndrTmpl[i].setMenuColor();
		String fvData[] = flockVars[i].getData(numBoids);
		pa.showText(fvData[0],0, fvDataTxtStY);pa.translate(0,fvDataNewLineY);
		pa.translate(-bdgSizeX[i]-3,0);
		for(int j=1;j<fvData.length; ++j){pa.showText(fvData[j],0,fvDataTxtStY);pa.translate(0,fvDataNewLineY);}
	}//drawFlockMenu
	
	@Override
	public void drawCustMenuObjs(float animTimeMod){
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
	
	/**
	 * Handle application-specific flag setting
	 */
	@Override
	public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
		switch(idx){
			case drawBoids			    : {break;}
			case drawScaledBoids		: {break;}		
			case clearPath			    : {
				//TODO this needs to change how it works so that initialization doesn't call my_procApplet before it is ready
				//pa.setClearBackgroundEveryStep( !val);//turn on or off background clearing in main window
				break;}
			case showVel			    : {break;}
			case attractMode			: {break;}
			case showFlkMbrs 		    : {break;}
			case flkCenter 			    : {break;}
			case flkVelMatch 		    : {break;}
			case flkAvoidCol 		    : {break;}
			case flkWander 			    : {break;}
			case flkAvoidPred		    : {break;}
			case flkHunt			    : {break;}
			case flkHunger			    : {break;}
			case flkSpawn			    : {break;}
			case modDelT	 			: {break;}
			case flkCyclesFrc			: {break;}
			case viewFromBoid		    : {
				dispFlags.setDrawMseEdge(!val);//if viewing from boid, then don't show mse edge, and vice versa
				break;}	//whether viewpoint is from a boid's perspective or global
			case useOrigDistFuncs 	    : {
				if(flocks == null){break;}
				for(int i =0; i<flocks.length; ++i){
					flockVars[i].setDefaultWtVals(val);}
				break;
			}
			case useTorroid			    : { break;}		
			case isMTCapableIDX			: {break;}
			default : {
				if (!handlePrivBoidFlags_Indiv(idx, val, oldVal)){
					msgObj.dispErrorMessage(className, "handlePrivFlags_Indiv", "Unknown/unhandled flag idx :"+idx+" attempting to be set to "+val+" from "+oldVal+". Aborting.");
				}
			}
		}		
	}//setPrivFlags
	
	/**
	 * Instance-specific boolean flags to handle
	 * @param idx
	 * @param val
	 * @param oldVal
	 * @return
	 */
	protected abstract boolean handlePrivBoidFlags_Indiv(int idx, boolean val, boolean oldVal);
	
	/**
	 * Return the current flock vars for the flock specified by flockIDX
	 * @param flockIDX
	 * @return
	 */	
	public myFlkVars getFlkVars(int flockIDX) {return flockVars[flockIDX];}
	public String getFlkName(int flockIDX) {return flkNames[flockIDX];}
	
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
	@Override
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){	
		//build list select box values
		//keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
		
		tmpListObjVals.put(gIDX_BoidType, boidTypeNames);
		tmpListObjVals.put(gIDX_FlockToObs, flkNames);
			
		tmpUIObjArray.put(gIDX_TimeStep,  new Object[]{new double[]{0,1.0f,.0001f}, .1, "Time Step", GUIObj_Type.FloatVal, new boolean[]{true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_NumFlocks, new Object[]{new double[]{1,MaxNumFlocks,1.0f}, 1.0, "# of Flocks", GUIObj_Type.IntVal, new boolean[]{true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_BoidType,  new Object[]{new double[]{0,boidTypeNames.length-1,1.1f}, 0.0, "Flock Species", GUIObj_Type.ListVal, new boolean[]{true}} );   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_FlockToObs,new Object[]{new double[]{0,flkNames.length-1,1.1f}, 0.0, "Flock To Watch", GUIObj_Type.ListVal, new boolean[]{true}} );   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_ModNumBoids, new Object[]{new double[]{-50,50,1.0f}, 0.0, "Modify Flock Pop", GUIObj_Type.IntVal, new boolean[]{true}});   				//uiTrainDataFrmtIDX                                                                        
		tmpUIObjArray.put(gIDX_BoidToObs, new Object[]{new double[]{0,initNumBoids-1,1.0f}, 0.0, "Boid To Board", GUIObj_Type.IntVal, new boolean[]{true}} );   				//uiTrainDataFrmtIDX
		setupGUIObjsAras_Indiv(tmpUIObjArray, tmpListObjVals);
	}
	/**
	 * Set up ui objects specific to instancing class.
	 * @param tmpUIObjArray
	 * @param tmpListObjVals
	 */
	protected abstract void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals);
	
	//when flockToWatch changes, reset maxBoidToWatch value
	private void setMaxUIBoidToWatch(int flkIdx){guiObjs[gIDX_BoidToObs].setNewMax(flocks[flkIdx].boidFlock.size()-1);setUIWinVals(gIDX_BoidToObs);}	
	private void setMaxUIFlockToWatch(){guiObjs[gIDX_FlockToObs].setNewMax(numFlocks - 1);	setUIWinVals(gIDX_FlockToObs);}		
	
	/**
	 * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
		switch(UIidx){	
			case gIDX_NumFlocks		:{
				numFlocks = ival; 
				initFlocks(); 
				break;}
			case gIDX_BoidType		:{
				rndrTmpl = cmplxRndrTmpls.get(boidTypeNames[ival]);
				privFlags.setFlag( flkCyclesFrc, boidCyclesFrc[ival]);//set whether this flock cycles animation/force output
				initFlocks(); 
				break;}
			case gIDX_FlockToObs 	:{
				flockToWatch = ival; 
				setMaxUIBoidToWatch(flockToWatch);
				break;}
			case gIDX_ModNumBoids  	:{	
				flocks[flockToWatch].modNumBoids(ival);
				break;}
			case gIDX_BoidToObs 	:{
				boidToWatch = ival;
				break;}		
			default : {
				if (!setUI_IntValsCustom_Indiv(UIidx, ival, oldVal)) {
					msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
				}
				break;
			}				
		}
	}//setUI_IntValsCustom
	
	/**
	 * Handles Instance-specific UI objects
	 * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 * @return whether the UIidx was found or not
	 */
	protected abstract boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal);
	

	/**
	 * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 */
	@Override
	protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
		switch(UIidx){		
		case gIDX_TimeStep 			:{curTimeStep = val;break;}
		default : {
			if (!setUI_FloatValsCustom_Indiv(UIidx, val, oldVal)) {
				msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No float-defined gui object mapped to idx :"+UIidx);
			}
			break;}
		}				
	}//setUI_FloatValsCustom
	/**
	 * Handles Instance-specific UI objects
	 * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed! 
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 * @return whether the UIidx was found or not
	 */
	protected abstract boolean setUI_FloatValsCustom_Indiv(int UIidx, float val, float oldVal);
	
	public double getTimeStep(){
		return curTimeStep * timeStepMult;
	}
	/**
	 * Once boid count has been modified, reset mod UI obj
	 */
	public void clearModNumBoids() {	resetUIObj(gIDX_ModNumBoids);}
	
	@Override
	public void initDrwnTrajIndiv(){}	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){}//drawTraj3D	
	//set camera to either be global or from pov of one of the boids
	@Override
	protected void setCameraIndiv(float[] camVals){
		if (privFlags.getFlag(viewFromBoid)){	setBoidCam(rx,ry,dz);		}
		else {	
			pa.setCameraWinVals(camVals);//(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);      
			// puts origin of all drawn objects at screen center and moves forward/away by dz
			pa.translate(camVals[0],camVals[1],(float)dz); 
		    setCamOrient();	
		}
	}
	
	@Override
	protected void drawMe(float animTimeMod) {
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
		timeStepMult = privFlags.getFlag(modDelT) ?  modAmtSec * 30.0f : 1.0f;
		for(int i =0; i<flocks.length; ++i){flocks[i].clearOutBoids();}			//clear boid accumulators of neighbors, preds and prey 
		for(int i =0; i<flocks.length; ++i){flocks[i].initAllMaps();}
		boolean checkForce = (AppMgr.mouseIsClicked()) && (!AppMgr.shiftIsPressed());
		if(privFlags.getFlag(useOrigDistFuncs)){for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsOrigMultTH(checkForce);}} 
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

	/**
	 * handle click in menu region - return idx of mod obj or -1
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
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
		//msgObj.dispDebugMessage(className,"handleFlkMenuClick","Flock vars click : [" + mouseX + "," + mouseY + "] row : " +clkRow + " obj idx : " + vIdx);	
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
	}//handleFlkMenuDrag
	
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

	/**
	 * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
	 * @param funcRow idx for button row
	 * @param btn idx for button within row (column)
	 * @param label label for this button (for display purposes)
	 */
	@Override
	protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
		switch(funcRow) {
		case 0 : {
			msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 1 | Btn : " + btn, MsgCodes.info4);
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
			msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn, MsgCodes.info4);
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
			msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 3 | Btn : " + btn, MsgCodes.info4);
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
			msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 4 | Btn : " + btn, MsgCodes.info4);
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
		default : {
			msgObj.dispWarningMessage(className,"launchMenuBtnHndlr","Clicked Unknown Btn row : " + funcRow +" | Btn : " + btn);
			break;
		}
		}			
	}
	
	@Override
	public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {}
	
	@Override
	protected final void handleSideMenuDebugSelEnable(int btn) {
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
	}
	
	@Override
	protected final void handleSideMenuDebugSelDisable(int btn) {
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
	protected void setCustMenuBtnLabels() {}

	@Override
	public void hndlFileLoad(File file, String[] vals, int[] stIdx) {}

	@Override
	public ArrayList<String> hndlFileSave(File file) {		
		return null;
	}

	@Override
	public void processTrajIndiv(DrawnSimpleTraj drawnTraj) {	}
}//class Base_BoidsWindow