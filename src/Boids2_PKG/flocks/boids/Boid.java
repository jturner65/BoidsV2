package Boids2_PKG.flocks.boids;

import java.util.concurrent.ConcurrentSkipListMap;

import Boids2_PKG.flocks.BoidFlock;
import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;

/**
 * class defining a creature object for flocking
 * @author John
 */
public class Boid {
	public static GUI_AppManager AppMgr;
	/**
	 * Owning flock for this boid
	 */
	public BoidFlock flk;
	/**
	 * Unique ID for this boid
	 */
	public int ID;
	private static int IDcount = 0;
	/**
	 * How long until this boid starves
	 */
	public int starveCntr;
	/**
	 * Counter for how long until ready to spawn
	 */
	private int spawnCntr;
	/**
	 * Axis angle orientation of this boid
	 */
	public float[] O_axisAngle;															
	/**
	 * axis angle orientation of this boid's spawn
	 */
	public float[] baby_O_axisAngle;
	/**
	 * Mass of this boid. Reflected in scale of rendering if enabled
	 */
	public float mass;
	/**
	 * Last frame's rotation angle, used to orient the boid 
	 */
	private float oldRotAngle;	
	public final float sizeMult = .15f;	
	/**
	 * Multiplier for scale based on mass
	 */
	private final myVectorf scMult = new myVectorf(.5f,.5f,.5f);				
	/**
	 * Scale of boat - reflects mass
	 */
	private myVectorf scaleBt;
	/**
	 * Rotation Vector
	 */
	private myVectorf rotVec;
	/**
	 * Velocity applied to boid at birth - hit the ground running
	 */
	private myVectorf birthVel;
	/**
	 * Force applied to boid at birth - hit the ground running
	 */
	private myVectorf birthForce;
	/**
	 * COM Location of boid in space
	 */
	private myPointf coords;
	/**
	 * Linear Velocity of boid COM in space
	 */
	public myVectorf velocity;
	/**
	 * Aggregate forces acting on boid's COM
	 */
	public myVectorf forces;												//force accumulator
	/**
	 * Rot matrix - 3x3 orthonormal basis matrix
	 * cols are bases for body frame orientation in world frame
	 */
	public myVectorf[] orientation;	
		
	public boolean[] boidFlags;	
	public final static int canSpawn 		= 0,							//whether enough time has passed that this boid can spawn
						 	isDead			= 1,							//whether this boid is dead
						 	isHungry		= 2,							//whether this boid is hungry
						 	hadChild		= 3;							//had a child this cycle, needs to "deliver"
	private final static int NumBoidFlags 	= 4;
	
	/**
	 * location to put new child
	 */
	private myPointf birthLoc;	
	
	/**
	 * animation controlling variables
	 */	
	private double animCntr;
	
	/**
	 * Fraction of animation cycle currently at
	 */
	public double animPhase;
	/**
	 * Current render object type's max anim counter
	 */
	private double maxAnimCntr;	
	/**
	 * for spawning gender = 0 == female, 1 == male;
	 */
	public final int gender;												
	public static final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
	
	/**
	 * sorted by distance map of neighbors to this boid
	 */
	public ConcurrentSkipListMap<Float, Boid> neighbors;
	/**
	 * sorted by distance map of prey near this boid
	 */
	public ConcurrentSkipListMap<Float, Boid> preyFlk;
	/**
	 * sorted by distance map of potential mates near this boid
	 */
	public ConcurrentSkipListMap<Float, Boid> posMate;			
	/**
	 * Neighbor boid location mapped to location used for distance calc
	 */
	public ConcurrentSkipListMap<Float, myPointf> neighLoc;	
	/**
	 * Neighbor boids within collision radius location mapped to location used for distance calc
	 */
	public ConcurrentSkipListMap<Float, myPointf> colliderLoc;
	/**
	 * Potential predator boid location mapped to location used for distance calc
	 */
	public ConcurrentSkipListMap<Float, myPointf> predFlkLoc;
	/**
	 * Potential prey boid location mapped to location used for distance calc
	 */
	public ConcurrentSkipListMap<Float, myPointf> preyFlkLoc;
	
	public Boid(BoidFlock _f,  myPointf _coords){
		ID = IDcount++;	 flk = _f;
		AppMgr = Base_DispWindow.AppMgr;
		initboidFlags();
		rotVec = myVectorf.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVectorf[3];
		orientation[O_FWD] = myVectorf.FORWARD.cloneMe();
		orientation[O_RHT] = myVectorf.RIGHT.cloneMe();
		orientation[O_UP] = myVectorf.UP.cloneMe();
		//keep initial phase between .25 and .75 so that cyclic-force boids start moving right away
		animPhase = MyMathUtils.randomFloat(.25f, .75f);
		maxAnimCntr = flk.getMaxAnimCounter();
		animCntr = animPhase * maxAnimCntr;
		
		coords = new myPointf(_coords);	//new myPointf[2]; 
		velocity = new myVectorf();
		forces  = new myVectorf();//= new myVectorf[2];
		setInitState();
		O_axisAngle=new float[]{0,1,0,0};
		oldRotAngle = 0;
		gender =  MyMathUtils.randomInt(1000)%2;												//0 or 1
		neighbors 	= new ConcurrentSkipListMap<Float, Boid>();
		preyFlk 	= new ConcurrentSkipListMap<Float, Boid>();
		posMate 	= new ConcurrentSkipListMap<Float, Boid>();
		
		neighLoc 	= new ConcurrentSkipListMap<Float, myPointf>();
		colliderLoc = new ConcurrentSkipListMap<Float, myPointf>();
		predFlkLoc	= new ConcurrentSkipListMap<Float, myPointf>();
		preyFlkLoc	= new ConcurrentSkipListMap<Float, myPointf>();
	}//constructor
	
	public void setInitState(){
		setMassAndScale(flk.flv.getInitMass());
		//init starve counter with own mass
		eat(mass);
		spawnCntr = 0;		
		boidFlags[canSpawn] = true;
	}
	
	/**
	 * Set the boid's mass
	 * @param _mass
	 */
	public void setMassAndScale(float _mass) {
		mass=_mass;
		scaleBt = new myVectorf(scMult);					//for rendering different sized boids
		scaleBt._mult(mass);		
	}
	
	public void clearNeighborMaps(){	
		neighbors.clear(); 
		neighLoc.clear(); 
		colliderLoc.clear();	 
	}
	public void clearHuntMaps(){
		predFlkLoc.clear();
		preyFlk.clear(); 
		preyFlkLoc.clear();		
	}
	
	public void copySubSetBoidsCol(Float colRadSq){		
		colliderLoc.putAll(neighLoc.subMap(0.0f, colRadSq));	
	}
	public void copySubSetBoidsMate(Float spawnRadSq){
		if((!boidFlags[canSpawn]) || (gender==0)){return;}//need "males" who can mate
		for(Float dist : neighbors.keySet()){
			if (dist > spawnRadSq){return;}//returns in increasing order - can return once we're past spawn Rad Threshold
			Boid b = neighbors.get(dist);
			if((b.gender==0)&&(b.canSpawn())){	posMate.put(dist, b);}
		}
	}//copySubSetBoidsMate
	
	/**
	 * Set this boid to have a child at a specific location, with specific initial velocity and forces
	 * @param _bl
	 * @param _bVel
	 * @param _bFrc
	 */
	public void haveChild(myPointf _bl, myVectorf _bVel, myVectorf _bFrc){
		boidFlags[hadChild]=true; 
		birthLoc=_bl;
		birthVel=_bVel;
		birthForce=_bFrc;
	}
	/**
	 * This boid
	 * @param _bl
	 * @param _bVelFrc
	 * @return
	 */
	public boolean hadAChild(myPointf[] _bl, myVectorf[] _bVelFrc){//if baby is born then set values of arrays and return
		if(boidFlags[hadChild]){
			boidFlags[hadChild]=false;
			_bl[0].set(birthLoc);
			_bVelFrc[0].set(birthVel);
			_bVelFrc[1].set(birthForce);
			return true;
		} 
		return false;
	}	
	private int resetCntrs(int cntrBseVal, float mod){return (int)(cntrBseVal*(1+mod));}
	/**
	 * Only reset spawn counters once boid has spawned
	 */
	public void hasSpawned(){
		spawnCntr = resetCntrs(flk.flv.spawnFreq, MyMathUtils.randomFloat()); 
		boidFlags[canSpawn] = false;
	}
	public boolean canSpawn(){return boidFlags[canSpawn];}
	/**
	 * update spawn counters
	 */
	public void updateSpawnCntr(){
		--spawnCntr;
		boidFlags[canSpawn]=(spawnCntr<=0);
	}//updateBoidCounters	
	
	/**
	 * update hunger counters
	 */
	public void updateHungerCntr(){
		--starveCntr;
		if (starveCntr<=0){killMe("Starvation"); return;}//if can get hungry then can starve to death
		//once boid is hungry he stays hungry unless he eats (hungry set to false elsewhere)
		boidFlags[isHungry] = (boidFlags[isHungry] || ( MyMathUtils.randomInt(flk.flv.eatFreq)>=starveCntr)); 
	}	
	public void eat(float tarMass){	
		starveCntr = resetCntrs(flk.flv.eatFreq,tarMass);
		boidFlags[isHungry]=false;
	}
	public boolean canSprint(){return (starveCntr > flk.flv.canSprintCycles);}
	
	/**
	 * Whether or not we're hungry
	 * @return
	 */
	public boolean isHungry(){return boidFlags[isHungry];}
	/**
	 * Whether or not we're dead
	 * @return
	 */
	public boolean isDead() {return boidFlags[isDead];}
	/**
	 * init boidFlags state machine
	 */
	public void initboidFlags(){boidFlags = new boolean[NumBoidFlags];for(int i=0;i<NumBoidFlags;++i){boidFlags[i]=false;}}
	
	/**
	 * initialize newborn velocity, forces, and orientation
	 * @param bVelFrc
	 */
	public void initNewborn(myVectorf[] bVelFrc){
		velocity.set(bVelFrc[0]);
		forces.set(bVelFrc[1]);
	}	
	/**
	 * align the boid along the current orientation matrix
	 * @param ri
	 */
	private void alignBoid(IRenderInterface ri){
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		float rotAngle = (float) (oldRotAngle + ((O_axisAngle[0]-oldRotAngle) * flk.getDeltaT()));
		ri.rotate(rotAngle, rotVec.x, rotVec.y, rotVec.z);
		oldRotAngle = rotAngle;
	}//alignBoid	
	/**
	 * kill this boid
	 * @param cause
	 */
	public void killMe(String cause){
		if(AppMgr.isDebugMode()){AppMgr.msgObj.dispConsoleDebugMessage("myBoid", "killMe", "Boid : " +ID+" killed by : " + cause);}
		boidFlags[isDead]=true;
	}	
	
	/**
	 * set this boat's COM to be the camera location
	 * @param ri
	 * @param dThet
	 * @param dPhi
	 * @param dz
	 */
	public void setBoatCam(IRenderInterface ri,float dThet, float dPhi, float dz, myPointf winOrigin){
		//set eye to initially be at coords of boid, modified for world being displaced by half grid dims
		myPointf eyeTmp = myPointf._add(coords,winOrigin);
		myPointf tmpEyeMod = new myPointf(orientation[O_FWD]);
		tmpEyeMod._mult(2.0f);
		tmpEyeMod._add(orientation[O_UP]);
		tmpEyeMod._mult(4.0f);
		//move eye to be on deck
		eyeTmp._add(tmpEyeMod);
		myVectorf rotdir = myVectorf._rotAroundAxis( orientation[O_FWD], orientation[O_UP], dPhi);
		myVectorf eyeLookVec = myVectorf._rotAroundAxis( rotdir, rotdir._cross(orientation[O_UP]), dThet);
		myPointf eye = myPointf._add(eyeTmp, .1f*dz, eyeLookVec);
		myPointf dir =  myPointf._add(eyeTmp, eyeLookVec);
		ri.setCameraWinVals(new float[] {eye.x, eye.y, eye.z, dir.x, dir.y, dir.z, -orientation[O_UP].x, -orientation[O_UP].y, -orientation[O_UP].z});
	}//setBoatCam
	/**
	 * Set coordinates from passed point
	 * @param _coords
	 */
	public void setCoords(myPointf _coords) {coords.set(_coords);}
	/**
	 * Set coordinates from passed x,y,z values
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setCoords(float x, float y, float z) {coords.set(x,y,z);}
	/**
	 * Get this boid's coordinates
	 * @return
	 */
	public myPointf getCoords() {return coords;}
	
	/**
	 * Actual implementation of drawing the boid's shape
	 * @param ri
	 */
	private void _drawTmpl(IRenderInterface ri) {
		ri.pushMatState();
		flk.getCurrTemplate().drawMe(animPhase, ID);
		ri.popMatState();
	}
	
	/**
	 * Only called when animated object is drawn
	 * @param vel
	 */
	private void _animIncr(float vel){
		animCntr += (1.0f + vel);	
		maxAnimCntr = flk.getMaxAnimCounter();
		animCntr %= maxAnimCntr;
		animPhase = (animCntr/maxAnimCntr);									//phase of animation cycle
	}//_animIncr
	
	/**
	 * Draw this boid
	 * @param ri
	 */
	public void drawMe(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);
			alignBoid(ri);
			_drawTmpl(ri);		
		ri.popMatState();
		_animIncr(velocity.magn*.1f);
	}//drawme	
	
	/**
	 * Draw this boid scaled to a size reflecting its mass
	 * @param ri
	 */
	public void drawMeScaled(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);
			alignBoid(ri);
			ri.scale(scaleBt.x,scaleBt.y,scaleBt.z);				
			_drawTmpl(ri);
		ri.popMatState();
		_animIncr(velocity.magn*.1f);
	}//drawMeScaled
	
	/**
	 * Draw this boids past locations 
	 * @param ri
	 */
	public void drawMyTrajectory(IRenderInterface ri){
		ri.pushMatState();
		
		
		ri.popMatState();	
	}//drawMyTrajectory
	
	/**
	 * draw this boid as a ball - replace with sphere render obj 
	 * @param debugAnim
	 * @param showVel
	 */
	public void drawMeAsBall(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location			
			flk.getSphereTemplate().drawMe(animPhase, ID);
		ri.popMatState();
	}//drawMeAsBall
	
	/**
	 * Draw a line to closest predator and prey
	 * @param ri
	 */
	public void drawClosestPredAndPrey(IRenderInterface ri){
		if(preyFlkLoc.size() == 0){return;}
		myPointf tmp = preyFlkLoc.firstEntry().getValue();
		_drawClosestOther(ri, tmp, IRenderInterface.gui_LightRed, IRenderInterface.gui_Red);
		if(predFlkLoc.size() == 0){return;}
		tmp = predFlkLoc.firstEntry().getValue();
		_drawClosestOther(ri, tmp, IRenderInterface.gui_LightCyan, IRenderInterface.gui_Cyan);
	}//drawClosestPredAndPrey
	
	/**
	 * 
	 * @param ri
	 * @param tmp
	 * @param stClr
	 * @param endClr
	 */
	private void _drawClosestOther(IRenderInterface ri, myPointf tmp, int stClr, int endClr){
		ri.pushMatState();
			ri.setStrokeWt(3.0f);
			ri.drawLine(coords, tmp,stClr,endClr );
			ri.translate(tmp.x,tmp.y,tmp.z);		//move to location
			ri.setColorValFill(endClr, 255);
			ri.noStroke();
			ri.drawSphere(10);
		ri.popMatState();		
	}		
	//public double calcBobbing(){		return 2*(p.cos(.01f*animCntr));	}		//bobbing motion

	/**
	 * Draw a vector starting at the location of this boid
	 * @param ri
	 * @param vec
	 * @param clr
	 * @param sw
	 */
	private void _drawMyVec(IRenderInterface ri, myVectorf vec, int clr, float sw){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			ri.setColorValStroke(clr, 255);
			ri.setStrokeWt(sw);
			ri.drawLine(myPointf.ZEROPT,vec);
		ri.popMatState();	
	}//_drawMyVec
	
	/**
	 * Draw velocity vector
	 * @param ri
	 */
	public void drawMyVel(IRenderInterface ri) {	_drawMyVec(ri, velocity,IRenderInterface.gui_Magenta, 0.5f);}
	/**
	 * Draw accelerations/forces
	 * @param ri
	 */
	public void drawMyForces(IRenderInterface ri) {	_drawMyVec(ri, forces, IRenderInterface.gui_Green, 0.5f);}
	/**
	 * Draw rotation vector and orientation frame
	 * @param ri
	 */
	public void drawMyFrame(IRenderInterface ri) {
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			ri.setColorValStroke(IRenderInterface.gui_Cyan, 255);
			ri.setStrokeWt(2.0f);
			ri.drawLine(myPointf.ZEROPT,myPointf._mult(rotVec, 100f));
			AppMgr.drawAxes(100, 2.0f, myPoint.ZEROPT, orientation, 255);
		ri.popMatState();	
	}//drawMyFrame
	
	@Override
	public String toString(){
		String result = "ID : " + ID + " Type : "+flk.getName()+" | Mass : " + mass + " | Spawn CD "+spawnCntr + " | Starve CD " + starveCntr+"\n";
		result+=" | location : " + coords + " | velocity : " + velocity + " | forces : " + forces +"\n" ;
		//if(p.flags[p.debugMode]){result +="\nOrientation : UP : "+orientation[O_UP] + " | FWD : "+orientation[O_FWD] + " | RIGHT : "+orientation[O_RHT] + "\n";}
		int num =neighbors.size();
		result += "# neighbors : "+ num + (num==0 ? "\n" : " | Neighbor IDs : \n");
		if(flk.getShowFlkMbrs()){	for(Float bd_K : neighbors.keySet()){result+="\tNeigh ID : "+neighbors.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = colliderLoc.size();
		result += "# too-close neighbors : "+ num + (num==0 ? "\n" : " | Colliders IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : colliderLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		result += "# predators : "+ num + (num==0 ? "\n" : " | Predator IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : predFlkLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		num = preyFlk.size();
		result += "# prey : "+ num + (num==0 ? "\n" : " | Prey IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : preyFlk.keySet()){result+="\tPrey ID : "+preyFlk.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		return result;
	}	
}//myBoid class
