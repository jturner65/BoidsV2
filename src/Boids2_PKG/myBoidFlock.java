package Boids2_PKG;

//import java.util.SortedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.boids.myBoid;
import Boids2_PKG.renderedObjs.base.myRenderObj;
import Boids2_PKG.threadedSolvers.forceSolvers.myLinForceSolver;
import Boids2_PKG.threadedSolvers.forceSolvers.myOrigForceSolver;
import Boids2_PKG.threadedSolvers.forceSolvers.base.myFwdForceSolver;
import Boids2_PKG.threadedSolvers.initializers.myInitPredPreyMaps;
import Boids2_PKG.threadedSolvers.initializers.myBoidValsResetter;
import Boids2_PKG.threadedSolvers.updaters.myBoidUpdater;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_Utils_Objects.io.MsgCodes;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;

public class myBoidFlock {
	public IRenderInterface p;	
	public myBoids3DWin win;
	public static GUI_AppManager AppMgr;
	public String name;
	public int numBoids;
	public ArrayList<myBoid> boidFlock;
	//private ArrayList<List<myBoid>> boidThrdFrames;			//structure to hold views of boidFlock for each thread operation
	private List<myBoid>[] boidThrdFrames;			//structure to hold views of boidFlock for each thread operation
	
	public float delT;
	
	public myFlkVars flv;						//flock vars per flock	
	
	public final float  distGrwthMod = 1.1f,	//how the radius should grow to look for more creatures for neighbors, if haven't satisfied minimum number
						nearPct = .4f;			//% size of total population to use as neighborhood target, if enough creatures
	
	public float totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	public int curFlagState;					//holds current state of first 32 flags from win/UI
	
	public final int type, mtFrameSize = 100;		//mtFrameSize is # of boids per thread
	public myRenderObj tmpl, sphTmpl;				//template to render boid; simplified sphere template
	public myBoidFlock preyFlock, predFlock;		//direct reference to flock that is my prey and my predator -- set in main program after init is called
	
	public List<Future<Boolean>> callFwdSimFutures, callUpdFutures, callInitFutures, callResetBoidFutures;
	public List<myFwdForceSolver> callFwdBoidCalcs;
	public List<myBoidUpdater> callUbdBoidCalcs;	
	public List<myInitPredPreyMaps> callInitBoidCalcs;
	public List<myBoidValsResetter> callResetBoidCalcs;
	
	private final int numThrds;
	protected ExecutorService th_exec;	//to access multithreading - instance from calling program
	//flock-specific data
	//private int flkMenuClr;//color of menu	
	
	public myBoidFlock(IRenderInterface _p, myBoids3DWin _win, myFlkVars _flv, int _numBoids, int _type){
		p = _p; win=_win;	
		flv = _flv;
		name = flv.typeName; 
		AppMgr = myDispWindow.AppMgr;	
		type = _type;
		th_exec = win.getTh_Exec();
		int numThrdsAvail = AppMgr.getNumThreadsAvailable();
		numThrds = (numThrdsAvail - 2);
		
		//Boids_2 _p, myBoids3DWin _win, myBoidFlock _flock, int _bodyClr, int numSpc, float _nRadMult
		delT = (float) win.getTimeStep();
		setNumBoids(_numBoids);
		totMaxRad = AppMgr.gridDimX + AppMgr.gridDimY + AppMgr.gridDimZ;
		
		callFwdBoidCalcs= new ArrayList<myFwdForceSolver>();
		callFwdSimFutures = new ArrayList<Future<Boolean>>(); 

		callUbdBoidCalcs = new ArrayList<myBoidUpdater>();
		callUpdFutures = new ArrayList<Future<Boolean>>(); 
		
		callInitBoidCalcs = new ArrayList<myInitPredPreyMaps>();
		callInitFutures = new ArrayList<Future<Boolean>>(); 
		
		callResetBoidCalcs = new ArrayList<myBoidValsResetter>();
		callResetBoidFutures = new ArrayList<Future<Boolean>>(); 	
		
		curFlagState = win.getFlkFlagsInt();

	}//myBoidFlock constructor
	//init bflk_flags state machine
	
	//public void initbflk_flags(boolean initVal){bflk_flags = new boolean[numbflk_flags];for(int i=0;i<numbflk_flags;++i){bflk_flags[i]=initVal;}}
	public void initFlock(){
		boidFlock = new ArrayList<myBoid>(numBoids);
		for(int c = 0; c < numBoids; ++c){
			boidFlock.add( new myBoid(p, win,this,randBoidStLoc(), type));
		}
		setNumBoids(boidFlock.size());
		buildThreadFrames();
	}//initFlock - run after each flock has been constructed
	
	public void setPredPreyTmpl(int predIDX, int preyIDX, myRenderObj _tmpl, myRenderObj _sphrTmpl){
		predFlock = win.flocks[predIDX];//flock 0 preys on flock 2, is preyed on by flock 1
		preyFlock = win.flocks[preyIDX];	
		tmpl = _tmpl;
		sphTmpl = _sphrTmpl;
	}//set after init - all flocks should be made
	
	//finds valid coordinates if torroidal walls 
	public myPointf findValidWrapCoordsForDraw(myPointf _coords){return new myPointf(((_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX),((_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY),((_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ));	}//findValidWrapCoords	
	public void setValidWrapCoordsForDraw(myPointf _coords){_coords.set(((_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX),((_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY),((_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ));	}//findValidWrapCoords	
	public float calcRandLocation(float randNum1, float randNum2, float sqDim, float mathCalc, float mult){return ((sqDim/2.0f) + (randNum2 * (sqDim/3.0f) * mathCalc * mult));}
	public myPointf randBoidStLoc(){		return new myPointf(ThreadLocalRandom.current().nextFloat()*AppMgr.gridDimX,ThreadLocalRandom.current().nextFloat()*AppMgr.gridDimY,ThreadLocalRandom.current().nextFloat()*AppMgr.gridDimZ);	}
	
	public void setNumBoids(int _numBoids){
		numBoids = _numBoids;
		nearCount = (int) Math.max(Math.min(nearMinCnt,numBoids), numBoids*nearPct); 		
	}
	
	//adjust boid population by m
	public void modBoidPop(int m){
		if(m>0){if(boidFlock.size() >= win.MaxNumBoids) {return;}for(int i=0;i<m;++i){ addBoid();}} 
		else { int n=-1*m; n = (n>numBoids-1 ? numBoids-1 : n);for(int i=0;i<n;++i){removeBoid();}}
	}//modBoidPop
	
	public myBoid addBoid(){	return addBoid(randBoidStLoc());	}	
	public myBoid addBoid(myPointf stLoc){
		myBoid tmp = new myBoid(p, win, this, stLoc, type); 
		boidFlock.add(tmp);
		setNumBoids(boidFlock.size());
		return tmp;
	}//addBoid	
	
	public void removeBoid(){removeBoid(boidFlock.size()-1);}
	public void removeBoid(int idx){
		if(idx<0){return;}	
		boidFlock.remove(idx);
		setNumBoids(boidFlock.size());
	}//removeBoid		
	
	/**
	 * This will build the individual per-thread views of the boid flock.
	 */
	@SuppressWarnings("unchecked")
	private void buildThreadFrames() {
		int numFrames = (numBoids > numThrds ? numThrds : numBoids); //TODO : have threads equally spread among all flocks
		boidThrdFrames = new List[numFrames];		
		if (numFrames == 0) {return;}
		int frSize = numBoids/numFrames;
		// # of frames to add 1 to, to equally disperse remainder after integer div
		int framesToOverload = numBoids % numFrames;
		int stIdx = 0, endIdx;
		//for these frames add an extra 1
		for (int i = 0;i<framesToOverload;++i) {
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
	
	//move creatures to random start positions
	public void scatterBoids() {for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).coords.set(randBoidStLoc());}}//	randInit
	public void drawBoids(){
		boolean debugAnim = win.getPrivFlags(myBoids3DWin.debugAnimIDX), 
				showVel = win.getPrivFlags(myBoids3DWin.showVel);

		if(win.getPrivFlags(myBoids3DWin.drawBoids)){//broken apart to minimize if checks - only potentially 2 per flock per frame instead of thousands
			if (win.getPrivFlags(myBoids3DWin.drawScaledBoids)) {
				if(debugAnim){		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeDbgFrameScaled();}}
				else if (showVel){	for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeAndVelScaled();}}
				else {				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeScaled();}}				
			} else {
				if(debugAnim){		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeDbgFrame();}}
				else if (showVel){	for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeAndVel();}}
				else {				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMe();}}
			}
		} else {
			for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeBall(debugAnim,showVel);  }
			if(win.getPrivFlags(myBoids3DWin.showFlkMbrs)){
				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPrey();  }
				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPredator();  }
			}
		}
	}//drawBoids

	//clear out all data for each boid
	public void clearOutBoids(){
		curFlagState = win.getFlkFlagsInt();
		//sets current time step from UI
		delT = (float) win.getTimeStep();
		callResetBoidCalcs.clear();
						//find next turn's motion for every creature by finding total force to act on creature
		for(List<myBoid> subL : boidThrdFrames){
			callResetBoidCalcs.add(new myBoidValsResetter(this, preyFlock, curFlagState, subL));
		}
		try {
			callResetBoidFutures = th_exec.invokeAll(callResetBoidCalcs);
			for(Future<Boolean> f: callResetBoidFutures) { 
				f.get(); 
			}
		} catch (Exception e) {
			//win.getMsgObj().dispMessage("myBoidFlock","buildThreadFrames ("+name+")","Error : "+e.toString()+"\n",MsgCodes.error1); 
			e.printStackTrace(); 
		}			
	}
	//build all data structures holding neighbors, pred, prey
	public void initAllMaps(){
		callInitBoidCalcs.clear();
		for(List<myBoid> subL : boidThrdFrames){
			callInitBoidCalcs.add(new myInitPredPreyMaps(AppMgr, this, preyFlock, predFlock, flv, curFlagState, subL));
		}
		try {callInitFutures = th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	//TODO get this from myDispWindow instead of AppMgr
	private boolean ckAddFrc(){return (AppMgr.mouseIsClicked()) && (!AppMgr.shiftIsPressed());}
	//build forces using linear distance functions
	public void moveBoidsLinMultTH(){
		callFwdBoidCalcs.clear();
		boolean addFrc = ckAddFrc();
		for(List<myBoid> subL : boidThrdFrames){
			callFwdBoidCalcs.add(new myLinForceSolver(AppMgr,  this, curFlagState, addFrc, subL));
		}
		try {callFwdSimFutures = th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	
	//build forces using original boids-style distance functions
	public void moveBoidsOrigMultTH(){
		callFwdBoidCalcs.clear();
		boolean addFrc = ckAddFrc();
		for(List<myBoid> subL : boidThrdFrames){
			callFwdBoidCalcs.add(new myOrigForceSolver(AppMgr,  this, curFlagState, addFrc, subL));
		}
		try {callFwdSimFutures = th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	public void updateBoidMovement(){
		callUbdBoidCalcs.clear();
		for(List<myBoid> subL : boidThrdFrames){
			callUbdBoidCalcs.add(new myBoidUpdater(AppMgr, this, curFlagState, subL));
		}
		try {callUpdFutures = th_exec.invokeAll(callUbdBoidCalcs);for(Future<Boolean> f: callUpdFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		    	
	}//updateBoids	
	
	public void finalizeBoids(){
    	//update - remove dead, add babies
        myPointf[] bl = new myPointf[]{new myPointf()};
        myVectorf[] bVelFrc  = new myVectorf[]{new myVectorf(),new myVectorf()};
        myBoid b;
        for(int c = 0; c < boidFlock.size(); ++c){
        	b = boidFlock.get(c);
        	if(b==null){
        		win.getMsgObj().dispMessage("myBoidFlock","finalizeBoids","boid is null", MsgCodes.info4);
        		continue;}
        	if(b.bd_flags[myBoid.isDead]){       		removeBoid(c);       	}
        	else if(b.hadAChild(bl,bVelFrc)){  		myBoid tmpBby = addBoid(bl[0]); tmpBby.initNewborn(bVelFrc);   	}
        } 	
		buildThreadFrames();
	}

	public String[] getInfoString(){return this.toString().split("\n",-1);}
	
	public String toString(){
		String res = "Flock Size " + boidFlock.size() + "\n";
		for(myBoid bd : boidFlock){			res+="\t     "+bd.toString(); res+="\n";	}
		return res;
	}
		
}//myBoidFlock class
