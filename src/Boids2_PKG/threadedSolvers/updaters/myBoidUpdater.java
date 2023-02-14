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

public class myBoidUpdater implements Callable<Boolean> {
	private GUI_AppManager AppMgr;
	private List<myBoid> bAra;
	private myBoidFlock f;
	private final float rt2;
	private final int O_FWD, O_RHT,  O_UP;
	private final float epsValCalc, epsValCalcSq, spawnPct;
	
	/**
	 * Type of update to do
	 */
	private BoidUpdate_Type updateToDo = BoidUpdate_Type.Move;
	
	public myBoidUpdater(GUI_AppManager _AppMgr, myBoidFlock _f, List<myBoid> _bAra){
		f=_f; bAra=_bAra;AppMgr=_AppMgr;
		O_FWD = myBoid.O_FWD;
		O_RHT = myBoid.O_RHT;  
		O_UP = myBoid.O_UP;  
		rt2 = .5f*MyMathUtils.SQRT_2_F;//p.fsqrt2; 
		epsValCalc = MyMathUtils.EPS_F;
		epsValCalcSq = epsValCalc * epsValCalc;
		spawnPct = f.flv.spawnPct;
	}	

	public void reproduce(myBoid b){
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
		float fwdUpDotm1 = b.orientation[O_FWD]._dot(myVectorf.UP);
		if (1.0 - (fwdUpDotm1 * fwdUpDotm1) < epsValCalcSq){
			return myVectorf._cross(b.orientation[O_RHT], b.orientation[O_FWD]);
		}
		return myVectorf.UP.cloneMe();
	}	
	
	public void setOrientation(myBoid b, double delT){
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
			//b.orientation[O_UP].set(b.orientation[O_UP]._normalize());
			b.orientation[O_RHT]._normalize();
		}
		//can't use MyMathUtiles static version
		b.O_axisAngle = toAxisAngle(b.orientation);
	}
	
	private void moveBoids() {
		double delT = f.getDeltaT();
		for(myBoid b : bAra){
			//if(!b.isDead()){
				//sclMult = (p.sin(a * radAmt) * .25f) +1.0f;
				// 1.0f/(sclMult * sclMult)
				if (b.forces.magn >.00001f) {
					b.velocity.set(integrate(myVectorf._mult(b.forces, (1.0f/b.mass)), b.velocity, delT));			//myVectorf._add(velocity[0], myVectorf._mult(forces[1], p.delT/(1.0f * mass)));	divide by  mass, multiply by delta t
					if(b.velocity.magn > f.flv.maxVelMag){b.velocity._scale(f.flv.maxVelMag);}
					if(b.velocity.magn < f.flv.minVelMag){b.velocity._scale(f.flv.minVelMag);}
				}
				b.coords.set(integrate(b.velocity, b.coords, delT));												// myVectorf._add(coords[0], myVectorf._mult(velocity[1], p.delT));	
				setValWrapCoordsForDraw(b.coords);
				setOrientation(b, delT);
			//}
		}			
	}
	
	private void spawn() {
		for(myBoid b : bAra){//check every boid to reproduce
			reproduce(b);		
		}
		for(myBoid b : bAra){//update spawn counter
			b.updateSpawnCntr();	
		}
	}
	
	private void updateHunger() {
		//dead boids may exist only after hunt
		for(myBoid b : bAra){
			if(!b.isDead()){				b.updateHungerCntr();}
		}		
	}
	/**
	 * Determine which update procedure to perform
	 * @param _updToDo
	 */
	public void setCurrFunction(BoidUpdate_Type _updToDo) {
		updateToDo = _updToDo;
	}
	
	public void run(){	
		switch (updateToDo) {
			case Move : {	moveBoids(); break;}
			case Spawn : {	spawn(); break;}
			case Hunger : {	updateHunger(); break;}
			default:	{break;}
		}
	}
	
	//integrator
	public myPointf integrate(myVectorf stateDot, myPointf state, double delT){		return myPointf._add(state, myVectorf._mult(stateDot, delT));}
	public myVectorf integrate(myVectorf stateDot, myVectorf state, double delT){	return myVectorf._add(state, myVectorf._mult(stateDot, delT));}
	
	public void setValWrapCoordsForDraw(myPointf _coords){
		if((_coords.x > AppMgr.gridDimX) || (_coords.x < 0)){	_coords.x = (_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX;}
		if((_coords.y > AppMgr.gridDimY) || (_coords.y < 0)){	_coords.y = (_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY;}
		if((_coords.z > AppMgr.gridDimZ) || (_coords.z < 0)){	_coords.z = (_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ;}
		//_coords.set(((_coords.x+AppMgr.gridDimX) % AppMgr.gridDimX),((_coords.y+AppMgr.gridDimY) % AppMgr.gridDimY),((_coords.z+AppMgr.gridDimZ) % AppMgr.gridDimZ));	
	}//findValidWrapCoords	

	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}
}
