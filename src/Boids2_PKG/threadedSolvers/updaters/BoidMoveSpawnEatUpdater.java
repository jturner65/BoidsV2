package Boids2_PKG.threadedSolvers.updaters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.myBoidFlock;
import Boids2_PKG.flocks.boids.myBoid;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;

public class BoidMoveSpawnEatUpdater implements Callable<Boolean> {
	private GUI_AppManager AppMgr;
	private List<myBoid> bAra;
	private myBoidFlock f;
	private final float rt2 = MyMathUtils.INV_SQRT_2_F;//1/sqrt2; 
	private final int O_FWD = myBoid.O_FWD;
	private final int O_RHT = myBoid.O_RHT;
	private final int O_UP = myBoid.O_UP;  
	private final float epsValCalcSq =  MyMathUtils.EPS_F * MyMathUtils.EPS_F;
	private final float spawnPct;
	
	/**
	 * Type of update to do
	 */
	private BoidUpdate_Type updateToDo = BoidUpdate_Type.Move;
	
	public BoidMoveSpawnEatUpdater(GUI_AppManager _AppMgr, myBoidFlock _f, List<myBoid> _bAra){
		f=_f; bAra=_bAra;AppMgr=_AppMgr;
		spawnPct = f.flv.spawnPct;
	}	

	private void reproduce(myBoid b){
		float chance;
		for(myBoid ptWife : b.ptnWife.values()){
			chance = ThreadLocalRandom.current().nextFloat();
			if(chance < spawnPct){
				b.haveChild(new myPointf(ptWife.coords,.5f,b.coords), new myVectorf(ptWife.velocity,.5f,b.velocity), new myVectorf(ptWife.forces,.5f,b.forces));
				ptWife.hasSpawned();	
				b.hasSpawned();	return;
			}
		}
	}
	
	/**
	 * build axis angle orientation from passed orientation matrix
	 * @param orientation array of 3 vectors corresponding to orientation vectors
	 * 	- O_FWD idx of forward orientation
	 * 	- O_RHT idx of right orientation
	 * 	- O_UP idx of up orientation
	 * @return axis-angle representation of orientation
	 */	
	private float[] toAxisAngle(myVectorf[] orientation) {
		float angle,x=rt2,y=rt2,z=rt2,s;
		float fyrx = -orientation[O_FWD].y+orientation[O_RHT].x,
			uxfz = -orientation[O_UP].x+orientation[O_FWD].z,
			rzuy = -orientation[O_RHT].z+orientation[O_UP].y;
			
		if (((fyrx*fyrx) < epsValCalcSq) && ((uxfz*uxfz) < epsValCalcSq) && ((rzuy*rzuy) < epsValCalcSq)) {			//checking for rotational singularity
			// angle == 0
			float fyrx2 = orientation[O_FWD].y+orientation[O_RHT].x,
				fzux2 = orientation[O_FWD].z+orientation[O_UP].x,
				rzuy2 = orientation[O_RHT].z+orientation[O_UP].y,
				fxryuz3 = orientation[O_FWD].x+orientation[O_RHT].y+orientation[O_UP].z-3;
			if (((fyrx2*fyrx2) < 1)	&& (fzux2*fzux2 < 1) && ((rzuy2*rzuy2) < 1) && ((fxryuz3*fxryuz3) < 1)) {	return new float[]{0,1,0,0}; }
			// angle == pi
			angle = MyMathUtils.PI_F;
			float fwd2x = (orientation[O_FWD].x+1)/2.0f,rht2y = (orientation[O_RHT].y+1)/2.0f,up2z = (orientation[O_UP].z+1)/2.0f,
				fwd2y = fyrx2/4.0f, fwd2z = fzux2/4.0f, rht2z = rzuy2/4.0f;
			if ((fwd2x > rht2y) && (fwd2x > up2z)) { // orientation[O_FWD].x is the largest diagonal term
				if (fwd2x< MyMathUtils.EPS_F) {	x = 0;} else {			x = (float) Math.sqrt(fwd2x);y = fwd2y/x;z = fwd2z/x;} 
			} else if (rht2y > up2z) { 		// orientation[O_RHT].y is the largest diagonal term
				if (rht2y< MyMathUtils.EPS_F) {	y = 0;} else {			y = (float) Math.sqrt(rht2y);x = fwd2y/y;z = rht2z/y;}
			} else { // orientation[O_UP].z is the largest diagonal term so base result on this
				if (up2z< MyMathUtils.EPS_F) {	z = 0;} else {			z = (float) Math.sqrt(up2z);	x = fwd2z/z;y = rht2z/z;}
			}
			return new float[]{angle,x,y,z}; // return 180 deg rotation
		}
		//no singularities - handle normally
		myVectorf tmp = new myVectorf(rzuy, uxfz, fyrx);
		s = tmp.magn;
		if (s < MyMathUtils.EPS_F){ s=1; }
		tmp._scale(s);//changes mag to s
			// prevent divide by zero, should not happen if matrix is orthogonal -- should be caught by singularity test above
		angle = (float) -Math.acos(( orientation[O_FWD].x + orientation[O_RHT].y + orientation[O_UP].z - 1)/2.0);
	   return new float[]{angle,tmp.x,tmp.y,tmp.z};
	}//toAxisAngle
	
	private myVectorf getFwdVec(myBoid b, float delT_f){
		if(b.velocity.magn < MyMathUtils.EPS_F){			return b.orientation[O_FWD]._normalize();		}
		else {		
			myVectorf tmp = b.velocity.cloneMe()._normalize();			
			return new myVectorf(b.orientation[O_FWD], delT_f, tmp);		
		}
	}

	private myVectorf getUpVec(myBoid b){	
		float fwdUpDotm1 = 1 - b.orientation[O_FWD].z;//b.orientation[O_FWD]._dot(myVectorf.UP);
		if ((fwdUpDotm1 * fwdUpDotm1) < epsValCalcSq){return myVectorf._cross(b.orientation[O_RHT], b.orientation[O_FWD]);	}
		return myVectorf.UP.cloneMe();
	}	
	
	private void setOrientation(myBoid b, double delT){
		//find new orientation at new coords - creature is oriented in local axes as forward being positive z and up being positive y vectors correspond to columns, x/y/z elements correspond to rows
		b.orientation[O_FWD].set(getFwdVec(b, (float) delT));
		b.orientation[O_UP].set(getUpVec(b));	
		b.orientation[O_RHT] = b.orientation[O_UP]._cross(b.orientation[O_FWD]); //sideways is cross of up and forward - backwards(righthanded)
		//b.orientation[O_RHT] = b.orientation[O_FWD]._cross(b.orientation[O_UP]); //sideways is cross of up and forward - backwards(righthanded)
		b.orientation[O_RHT]._normalize();
		//b.orientation[O_RHT].set(b.orientation[O_RHT]._normalize());
		//need to recalc up?  may not be perp to normal
		if(Math.abs(b.orientation[O_FWD]._dot(b.orientation[O_UP])) > MyMathUtils.EPS_F){
			b.orientation[O_UP] = b.orientation[O_FWD]._cross(b.orientation[O_RHT]); //sideways is cross of up and forward
			b.orientation[O_UP]._normalize();
		}
		//can't use MyMathUtiles static version
		b.O_axisAngle = toAxisAngle(b.orientation);
	}
	
	private void moveBoids() {
		double delT = f.getDeltaT();
		for(myBoid b : bAra){
			if (b.forces.magn > epsValCalcSq) {
				b.velocity.set(integrate(myVectorf._mult(b.forces, (1.0f/b.mass)), b.velocity, delT));			//myVectorf._add(velocity[0], myVectorf._mult(forces[1], p.delT/(1.0f * mass)));	divide by  mass, multiply by delta t
				if(b.velocity.magn < f.flv.minVelMag){b.velocity._mult(f.flv.minVelMag/b.velocity.magn);}
				else if(b.velocity.magn > f.flv.maxVelMag){b.velocity._mult(f.flv.maxVelMag/b.velocity.magn);}
			}
			b.coords.set(integrate(b.velocity, b.coords, delT));												// myVectorf._add(coords[0], myVectorf._mult(velocity[1], p.delT));	
			setValWrapCoordsForDraw(b.coords);
			setOrientation(b, delT);
		}			
	}
	
	private void spawn() {
		for(myBoid b : bAra){reproduce(b);}  			//check every boid to reproduce
		for(myBoid b : bAra){b.updateSpawnCntr();}		//update spawn counter
	}
	
	private void updateHunger() {
		//dead boids may exist only after hunt
		for(myBoid b : bAra){if(!b.isDead()){				b.updateHungerCntr();}}		
	}
	/**
	 * Determine which update procedure to perform
	 * @param _updToDo
	 */
	public void setCurrFunction(BoidUpdate_Type _updToDo) {	updateToDo = _updToDo;}
	
	private void run(){	
		switch (updateToDo) {
			case Move : {	moveBoids(); break;}
			case Spawn : {	spawn(); break;}
			case Hunger : {	updateHunger(); break;}
			default:	{break;}
		}
	}
	
	//integrator
	private myPointf integrate(myVectorf stateDot, myPointf state, double delT){		return myPointf._add(state, myVectorf._mult(stateDot, delT));}
	private myVectorf integrate(myVectorf stateDot, myVectorf state, double delT){	return myVectorf._add(state, myVectorf._mult(stateDot, delT));}
	
	/**
	 * Restrict a value to lie within passed bounds, treating the bounds torroidally
	 * 	(i.e. wrapping from maxVal + x to minVal + x or minVal - y to maxVal - y)
	 * @param val the value to potentially wrap
	 * @param minVal the minimum allowed value for val
	 * @param maxVal the maximum allowed value for val
	 * @return val constrained between minVal and maxVal torroidally 
	 */
	private float wrapVal(float val, final float minVal, final float maxVal) {
		float distVal = maxVal - minVal;
		if (val > maxVal) {			return (val % distVal) + minVal;		}
		while (val < minVal) {val += distVal;}		
		return val;
	}
	
	private void setValWrapCoordsForDraw(myPointf _coords){
		_coords.x = wrapVal(_coords.x, 0.0f, AppMgr.gridDimX);
		_coords.y = wrapVal(_coords.y, 0.0f, AppMgr.gridDimY);
		_coords.z = wrapVal(_coords.z, 0.0f, AppMgr.gridDimZ);

		
//		if((_coords.x > AppMgr.gridDimX) || (_coords.x < 0)){	_coords.x = (_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX;}
//		if((_coords.y > AppMgr.gridDimY) || (_coords.y < 0)){	_coords.y = (_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY;}
//		if((_coords.z > AppMgr.gridDimZ) || (_coords.z < 0)){	_coords.z = (_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ;}
		//_coords.set(((_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX),((_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY),((_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ));	
	}//findValidWrapCoords	

	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}
}
