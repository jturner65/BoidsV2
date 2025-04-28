package Boids2_PKG.flocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.threadedSolvers.forceSolvers.LinearForceSolver;
import Boids2_PKG.threadedSolvers.forceSolvers.OriginalForceSolver;
import Boids2_PKG.threadedSolvers.initializers.BoidValsResetter;
import Boids2_PKG.threadedSolvers.initializers.InitPredPreyMaps;
import Boids2_PKG.threadedSolvers.initializers.InitPredPreyMapsTor;
import Boids2_PKG.threadedSolvers.updaters.BoidUpdate_Type;
import Boids2_PKG.threadedSolvers.updaters.BoidMoveSpawnEatUpdater;
import Boids2_PKG.ui.Boid_UIFlkVars;
import Boids2_PKG.ui.base.Base_BoidsWindow;
import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.renderedObjs.base.Base_RenderObj;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Utils_Objects.io.messaging.MsgCodes;

public class BoidFlock {
	/**
	 * Owning window
	 */
	protected Base_BoidsWindow win;
	/**
	 * Owning application manager
	 */
	public static GUI_AppManager AppMgr;
	/** 
	 * Flock name
	 */
	public String name;
	/**
	 * Current number of members in this flock
	 */
	public int numBoids;
	/**
	 * Collection of flock members
	 */
	public ArrayList<Boid> boidFlock;
	/**
	 * structure to hold views of boidFlock for each thread operation
	 */
	private List<Boid>[] boidThrdFrames;
	/**
	 * timestep
	 */
	private double delT;
	/**
	 * Flock variables for this flock
	 */
	public Boid_UIFlkVars flv;
	
	/**
	 * How the radius should grow to look for more creatures for neighbors, if haven't satisfied minimum number
	 */
	public final float  distGrwthMod = 1.1f;	
	/**
	 * % size of total population to use as neighborhood target, if enough creatures
	 */
	public final float  nearPct = .4f;
	
	public float totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	public int curFlagState;					//holds current state of first 32 flags from win/UI
	
	public final int type, mtFrameSize = 100;		//mtFrameSize is # of boids per thread
	private Base_RenderObj tmpl, sphTmpl;				//template to render boid; simplified sphere template
	public BoidFlock preyFlock, predFlock;		//direct reference to flock that is my prey and my predator -- set in main program after init is called
	
	private List<Future<Boolean>> callFwdSimFutures, callUpdFutures, callInitFutures, callResetBoidFutures;
	private List<Callable<Boolean>> callFwdBoidCalcs, callUbdBoidCalcs, callInitBoidCalcs, callResetBoidCalcs;
	
	/**
	 * Set via UI to add or remove boids on next cycle
	 */
	private int numBoidsToChange = 0;
	
	private final int numThrdsToUse;
	protected ExecutorService th_exec;	//to access multithreading - instance from calling program
	//flock-specific data
	//private int flkMenuClr;//color of menu
	
	protected final float[] grid3dDims;
	
	public BoidFlock(Base_BoidsWindow _win, Boid_UIFlkVars _flv, int _numBoids, int _type, int _numThrdsToUse){
		win=_win;	
		flv = _flv;
		name = flv.typeName; 
		AppMgr = Base_DispWindow.AppMgr;	
		type = _type;
		th_exec = win.getTh_Exec();
		numThrdsToUse = _numThrdsToUse;
		
		//Boids_2 _p, myBoids3DWin _win, myBoidFlock _flock, int _bodyClr, int numSpc, float _nRadMult
		delT = (float) win.getTimeStep();
		setNumBoids(_numBoids);		
		
		grid3dDims = AppMgr.get3dGridDims();
		totMaxRad = grid3dDims[0] + grid3dDims[1] + grid3dDims[2];
		
		callFwdBoidCalcs= new ArrayList<Callable<Boolean>>();
		callFwdSimFutures = new ArrayList<Future<Boolean>>(); 

		callUbdBoidCalcs = new ArrayList<Callable<Boolean>>();
		callUpdFutures = new ArrayList<Future<Boolean>>(); 
		
		callInitBoidCalcs = new ArrayList<Callable<Boolean>>();
		callInitFutures = new ArrayList<Future<Boolean>>(); 
		
		callResetBoidCalcs = new ArrayList<Callable<Boolean>>();
		callResetBoidFutures = new ArrayList<Future<Boolean>>(); 	
		
		curFlagState = win.getFlkFlagsInt();

	}//myBoidFlock constructor
	//init bflk_flags state machine
	
	/**
	 * Initialize flock by creating boids
	 */
	public void initFlock(){
		boidFlock = new ArrayList<Boid>(numBoids);
		for(int c = 0; c < numBoids; ++c){
			boidFlock.add( new Boid(this, randBoidStLoc(), type));
		}
		setNumBoids(boidFlock.size());
		//initial build of per-thread boid population
		buildThreadFrames();
	}//initFlock - run after each flock has been constructed
	
	/**
	 * Set the render template to use for this flock
	 * @param _tmpl
	 */
	public void setCurrTemplate(Base_RenderObj _tmpl) {tmpl = _tmpl;}
	
	/**
	 * Retrieve the current template used for boids
	 * @return
	 */
	public Base_RenderObj getCurrTemplate(){return tmpl;}
	
	/**
	 * Retrieve current template used for spherical rep of boids
	 * @return
	 */
	public Base_RenderObj getSphereTemplate() {return sphTmpl;}
	
	
	public void setPredPreySphereTmpl(BoidFlock _predFlock, BoidFlock _preyFlock, Base_RenderObj _tmpl, Base_RenderObj _sphrTmpl){
		predFlock = _predFlock;	//flock 0 preys on flock last, is preyed on by flock 1
		preyFlock = _preyFlock;	
		tmpl = _tmpl;
		sphTmpl = _sphrTmpl;
	}//set after init - all flocks should be made
	
	//finds valid coordinates if torroidal walls 
	public myPointf findValidWrapCoordsForDraw(myPointf _coords){return new myPointf(((_coords.x+grid3dDims[0]) % grid3dDims[0]),((_coords.y+grid3dDims[1]) % grid3dDims[1]),((_coords.z+grid3dDims[2]) % grid3dDims[2]));	}//findValidWrapCoords	
	public void setValidWrapCoordsForDraw(myPointf _coords){_coords.set(((_coords.x+grid3dDims[0]) % grid3dDims[0]),((_coords.y+grid3dDims[1]) % grid3dDims[1]),((_coords.z+grid3dDims[2]) % grid3dDims[2]));	}//findValidWrapCoords	
	public float calcRandLocation(float randNum1, float randNum2, float sqDim, float mathCalc, float mult){return ((sqDim/2.0f) + (randNum2 * (sqDim/3.0f) * mathCalc * mult));}
	public myPointf randBoidStLoc(){		return new myPointf(ThreadLocalRandom.current().nextFloat()*grid3dDims[0],ThreadLocalRandom.current().nextFloat()*grid3dDims[1],ThreadLocalRandom.current().nextFloat()*grid3dDims[2]);	}
	
	private void setNumBoids(int _numBoids){
		numBoids = _numBoids;
		nearCount = (int) Math.max(Math.min(nearMinCnt,numBoids), numBoids*nearPct); 		
	}
	
	/**
	 * Add or subtract boids based on UI input
	 * @param modAmt how many boids to add or subtract
	 */
	public void modNumBoids(int modAmt) {numBoidsToChange = modAmt;}
	
	protected Boid addBoid(){	return addBoid(randBoidStLoc());	}	
	protected Boid addBoid(myPointf stLoc){
		Boid tmp = new Boid(this, stLoc, type); 
		boidFlock.add(tmp);
		setNumBoids(boidFlock.size());
		return tmp;
	}//addBoid	
	
	protected void removeBoid(){removeBoid(boidFlock.size()-1);}
	protected void removeBoid(int idx){
		if(idx<0){return;}	
		boidFlock.remove(idx);
		setNumBoids(boidFlock.size());
	}//removeBoid		
	
	/**
	 * move creatures to random start positions
	 */
	public void scatterBoids() {for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).coords.set(randBoidStLoc());}}//	randInit

	public void drawBoids(IRenderInterface ri) {				for(Boid b : boidFlock){b.drawMe(ri);}}
	public void drawBoidsScaled(IRenderInterface ri) {			for(Boid b : boidFlock){b.drawMeScaled(ri);}}
	public void drawBoidsAsBall(IRenderInterface ri) {			for(Boid b : boidFlock){b.drawMeAsBall(ri);}}
	
	public void drawBoidVels(IRenderInterface ri) {			for(Boid b : boidFlock){b.drawMyVel(ri);}}
	public void drawBoidFrames(IRenderInterface ri) {			for(Boid b : boidFlock){b.drawMyFrame(ri);}}
	public void drawBoidsFlkMmbrs(IRenderInterface ri) {		for(Boid b : boidFlock){b.drawClosestPrey(ri);b.drawClosestPredator(ri);}}
	
	/**
	 * If this is being animated and has a template, return that template's max animation counter, otherwise return 1
	 * @return
	 */
	public double getMaxAnimCounter() {
		return tmpl == null ? 1.0 : tmpl.getMaxAnimCounter();
	}
	
	////////////////
	// Simulation step functions for flock

	/**
	 * This will build the individual per-thread views of the boid flock.  All boids will be evenly distributed amongst the available frames
	 */
	@SuppressWarnings("unchecked")
	private void buildThreadFrames() {
		int numFrames = (numBoids > numThrdsToUse ? numThrdsToUse : numBoids); 
		boidThrdFrames = new List[numFrames];		
		if (numFrames == 0) {return;}
		// Base # of boids per frame
		int frSize = numBoids/numFrames;
		// # of frames to add 1 to, to equally disperse remainder after integer div
		int framesToOverload = numBoids % numFrames;
		int stIdx = 0, endIdx;
		//for these frames add an extra 1
		for (int i = 0;i<framesToOverload;++i) {
			//add one to frame size
			endIdx = stIdx+frSize+1;
			boidThrdFrames[i] = boidFlock.subList(stIdx, endIdx);
			stIdx = endIdx;
		}
		//for these frames use calculated frame size
		for (int i=framesToOverload; i<numFrames; ++i) {
			endIdx = stIdx+frSize;
			boidThrdFrames[i] = boidFlock.subList(stIdx, endIdx);
			stIdx = endIdx;	
		}
	}
	
	public List<Boid>[] getBoidThrdFrames(){return boidThrdFrames;}
	
	/**
	 * clear out all data for each boid
	 */
	public void clearOutBoids(){
		curFlagState = win.getFlkFlagsInt();
		//sets current time step from UI
		delT = (float) win.getTimeStep();
		callResetBoidCalcs.clear();
		//find next turn's motion for every creature by finding total force to act on creature
		for(List<Boid> subL : boidThrdFrames){
			callResetBoidCalcs.add(new BoidValsResetter(this, curFlagState, subL));
		}
		try {callResetBoidFutures = th_exec.invokeAll(callResetBoidCalcs);for(Future<Boolean> f: callResetBoidFutures) {f.get();}} catch (Exception e) {e.printStackTrace();}			
	}
	/**
	 * build all data structures holding neighbors, pred, prey
	 */
	public void initAllMaps(){
		callInitBoidCalcs.clear();
		if(win.getIsTorroidal()) {
			for(List<Boid> subL : boidThrdFrames){
				callInitBoidCalcs.add(new InitPredPreyMapsTor(AppMgr, this, preyFlock, predFlock, flv, curFlagState, subL));
			}			
		} else {
			for(List<Boid> subL : boidThrdFrames){
				callInitBoidCalcs.add(new InitPredPreyMaps(AppMgr, this, preyFlock, predFlock, flv, curFlagState, subL));
			}
		}
		try {callInitFutures = th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	
	/**
	 * build forces using linear distance functions
	 * @param addFrc
	 */
	public void moveBoidsLinMultTH(boolean addFrc){
		callFwdBoidCalcs.clear();
		for(List<Boid> subL : boidThrdFrames){
			callFwdBoidCalcs.add(new LinearForceSolver(AppMgr,  this, curFlagState, addFrc, subL));
		}
		try {callFwdSimFutures = th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	
	/**
	 * build forces using original boids-style distance functions
	 * @param addFrc
	 */
	public void moveBoidsOrigMultTH(boolean addFrc){
		callFwdBoidCalcs.clear();
		for(List<Boid> subL : boidThrdFrames){
			callFwdBoidCalcs.add(new OriginalForceSolver(AppMgr,  this, curFlagState, addFrc, subL));
		}
		try {callFwdSimFutures = th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	
	/**
	 * Update boid movement, spawn and hunger state
	 * @param _state
	 */
	public void updateBoidState(BoidUpdate_Type _state){
		switch (_state) {
			case Move : {
				callUbdBoidCalcs.clear();
				//move boids
				for(List<Boid> subL : boidThrdFrames){callUbdBoidCalcs.add(new BoidMoveSpawnEatUpdater(AppMgr, this, subL));}
				break;}
			case Spawn:{
				for (Callable<Boolean> upd : callUbdBoidCalcs) {((BoidMoveSpawnEatUpdater) upd).setCurrFunction(BoidUpdate_Type.Spawn);}
				break;}
			case Hunger:{ 
				for (Callable<Boolean> upd : callUbdBoidCalcs) {((BoidMoveSpawnEatUpdater) upd).setCurrFunction(BoidUpdate_Type.Hunger);}
				break;}
			default:{
				win.getMsgObj().dispErrorMessage("myBoidFlock", "updateBoidState", "Unknown update state :"+_state.toStrBrf()+". Aborting!");
				return;}
		}
		try {callUpdFutures = th_exec.invokeAll(callUbdBoidCalcs);for(Future<Boolean> f: callUpdFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }
	}//updateBoids
	
	public void finalizeBoids(){
    	//update - remove dead, add babies
		//arrays so these can be passed as ptrs
        myPointf[] birthLoc = new myPointf[]{new myPointf()};
        myVectorf[] bVelFrc  = new myVectorf[]{new myVectorf(),new myVectorf()};
        Boid b;
        for(int c = 0; c < boidFlock.size(); ++c){
        	b = boidFlock.get(c);
        	if(b==null){
        		win.getMsgObj().dispMessage("myBoidFlock","finalizeBoids","Null boid found in flock "+ name, MsgCodes.info4);
        		removeBoid(c); 
        		continue;
        	}
        	if(b.bd_flags[Boid.isDead]){       				removeBoid(c);       	}
        	else if(b.hadAChild(birthLoc,bVelFrc)){  		Boid tmpBby = addBoid(birthLoc[0]); tmpBby.initNewborn(bVelFrc);   	}
        } 	
        //Handle adding/removing new boids from UI input
        if (numBoidsToChange != 0) {
        	//will be positive for addition
        	for(int i=0;i<numBoidsToChange; ++i) {        		addBoid();        	}
        	//will be negative for removal
        	for(int i=numBoidsToChange;i<0; ++i) {        		removeBoid();        	}
        	numBoidsToChange = 0;
        	win.clearModNumBoids();
        }
        //Do this after all boids have been added/removed, for next cycle
		buildThreadFrames();
	}
	
	////////////////
	// End simulation step functions for flock

	/**
	 * Whether we're showing the flock members 
	 * @return
	 */
	public final boolean getShowFlkMbrs() {return win.getShowFlkMbrs();}
	
	public String getName() {return name;}
	
	public String getFlkName(int flockIDX) {return win.getFlkName(flockIDX);}
	
	public double getDeltaT() {return delT;}

	public String[] getInfoString(){return this.toString().split("\n",-1);}
	
	public String toString(){
		String res = "Flock Size " + boidFlock.size() + "\n";
		for(Boid bd : boidFlock){			res+="\t     "+bd.toString(); res+="\n";	}
		return res;
	}
		
}//myBoidFlock class
