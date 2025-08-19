package Boids2_PKG.threadedSolvers.initializers.base;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.ui.Boids_3DWin;
import Boids2_PKG.ui.flkVars.BoidFlockVarsUI;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_UI_Objects.GUI_AppManager;


public abstract class Base_InitPredPreyMaps implements Callable<Boolean> {

    //an overlay for calculations to be used to determine forces acting on a creature
    
    protected GUI_AppManager AppMgr;
    protected BoidFlockVarsUI fv;
    
    protected float tot2MaxRad, totMaxRadSq;
    protected List<Boid> bAra;                                //boid ara being worked on
    public BoidFlock flock, pry, prd;
    protected int flagInt;                        //bitmask of current flags
    protected int nearCount;
    protected boolean[] stFlags; // tor, doHunt, doSpawn;                            //is torroidal
    protected final int         
            doHunt         = 0,
            doSpawn     = 1;
    
    protected final int[] stFlagIDXs = new int[]{Boids_3DWin.flkHunt, Boids_3DWin.flkSpawn};

    protected final float[] gridDims;
    /**
     * 
     * @param _AppMgr
     * @param _f
     * @param _pry
     * @param _prd
     * @param _fv
     * @param _flagInt
     * @param _bAra
     */
    public Base_InitPredPreyMaps(GUI_AppManager _AppMgr, BoidFlock _flk, BoidFlock _pry, BoidFlock _prd, BoidFlockVarsUI _fv, int _flagInt, List<Boid> _bAra) {
        AppMgr = _AppMgr;    flock = _flk; pry=_pry; prd=_prd; bAra=_bAra;
        fv=_fv;
        gridDims = AppMgr.get3dGridDims();
        tot2MaxRad = 2* flock.totMaxRad;
        totMaxRadSq = flock.totMaxRad * flock.totMaxRad;
        //TODO set these via passed int
        flagInt = _flagInt;
        setStFlags();
    }
    
    //TODO set these externally when/if eventually recycling threads
    private void setStFlags(){
        stFlags = new boolean[stFlagIDXs.length];
        for(int i =0;i<stFlagIDXs.length;++i){stFlags[i] = (((flagInt>>stFlagIDXs[i]) & 1) == 1);} 
    } 
    
    
    /**
     * Populate arrays with closest neighbors, sorted by sq distance, so that dist array and neighbor array coincide
     * @param _src
     */
    private final void findMyNeighbors(Boid _src) {
        if(nearCount >= bAra.size() ){        srchForNeighbors(_src, bAra, tot2MaxRad, totMaxRadSq);    }        //if we want to have more than the size of the flock, get the whole flock            
        else{                                srchForNeighbors(_src, bAra, 2 * fv.predRad, fv.nghbrRadSq);    }        
        _src.copySubSetBoidsCol(fv.colRadSq);
    }
    
    /**
     * Look for all neighbors until found neighborhood, expanding distance - keyed by squared distance
     * @param _src
     * @param flock
     * @param min2Dist
     * @param minDistSq
     */
    protected abstract void srchForNeighbors(Boid _src, List<Boid> flock, float min2Dist, float minDistSq );

    /**
     * Search for prey in specified neighborhood vicinity
     * @param _src
     * @param preyflock
     */
    protected abstract void srchForPrey(Boid _src, List<Boid> preyflock);
    
    /**
     * check if src boid map or tar boid map contain passed dist already
     * if so, increase dist a bit to put in unoccupied location in map
     * @param smap
     * @param dmap
     * @param distSq
     * @param _sLoc
     * @param _dLoc
     * @return
     */
    protected Float chkPutDistInMap(ConcurrentSkipListMap<Float, myPointf> smap,ConcurrentSkipListMap<Float, myPointf> dmap, Float distSq, myPointf _sLoc, myPointf _dLoc){
        myPointf chks4d = smap.get(distSq),
                chkd4s = dmap.get(distSq);
        //int iter=0;
        while((chks4d != null) || (chkd4s != null)){
            //replace chk if not null
            distSq *= 1.0000001f;//mod distance some tiny amount
            chks4d = smap.get(distSq);
            chkd4s = dmap.get(distSq);
            //System.out.println("chkPutDistInMap collision : " + distSq + " iter : " + iter++ );
        }
        chks4d = smap.put(distSq, _dLoc);    
        chkd4s = dmap.put(distSq, _sLoc);
        return distSq;
    }//chkDistInMap
    
    /**
     * Finds closest distance between passed values taking into account torroidal bound, 
     * returns square of that distance.
     * @param p1 a location value
     * @param p2 a location value
     * @param dim the torroidal distance along the axis of p1 and p2
     * @param newP1 new val for p1, as seen by owner of p2, (outside torroidal min/max bound)
     *         possibly taking into account torroidal bound if doing so is closest distance
     * @param newP2 new val for p2, as seen by owner of p1, (outside torroidal min/max bound)
     *         possibly taking into account torroidal bound if doing so is closest distance
     * @return closest squared distance between p1 and p2, accounting for torroidal distance
     */
    private float calcMinSqDist1D(float p1, float p2, float dim, float[] newP1, float[] newP2){
        float     d1 = (p1-p2),        d1s = d1*d1,
                d2 = (p1-(p2-dim)),    d2s = d2*d2,
                d3 = ((p1-dim)-p2),    d3s = d3*d3;
        if(d1s <= d2s){
            if(d1s <= d3s){    newP1[0] = p1;newP2[0] = p2;return d1s;}             //d1s is min, for this dim newP1 == p1 and newP2 == p2
            else {            newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}}     //d3s is the min
        else {
            if(d2s <= d3s){    newP1[0] = p1+dim;newP2[0] = p2-dim;return d2s;}    //d2s is min, for this dim newP1 == p1 and newP2 == p2 
            else {            newP1[0] = p1-dim;newP2[0] = p2+dim;return d3s;}}    //d3s is the min
    }// calcMinSqDist1D

    /**
     * Returns the minimum sq length vector from p1 to p2 for torroidal mapping; 
     * puts "virtual location" of torroidal mapped distances in newPt1 and newPt2
     * @param pt1
     * @param pt2
     * @param newPt1 new location for pt1, as seen by owner of pt2, (outside torroidal min/max bound)
     *         possibly taking into account torroidal bound if doing so is closest distance
     * @param new location for pt2, as seen by owner of pt1, (outside torroidal min/max bound)
     *         possibly taking into account torroidal bound if doing so is closest distance
     * @param dimX X dimension of torroidal zone
     * @param dimY Y dimension of torroidal zone
     * @param dimZ Z dimension of torroidal zone
     * @param min2Dist double the closest distance between pt1 and pt2, accounting for torroidal bound in 3D
     * @return min square distance between 2 points
     */
    protected float calcMinDistSq(myPointf pt1, myPointf pt2, myPointf newPt1, myPointf newPt2, float dimX, float dimY, float dimZ, float min2Dist){
        float sqDist = myPointf._SqrDist(pt1, pt2);
        //means points are already closer to each other in regular space so they don't need to be special referenced.
        if(sqDist <= min2Dist){return sqDist;}            
        //we're here because two boids are further from each other than the passed distance - now we have to find the closest they could be to each other given torroidal wrapping
        float[] newP1 = new float[]{0}, newP2 = new float[]{0};
        //Find closest squared distance along all 3 axes
        float dx = calcMinSqDist1D(pt1.x, pt2.x, dimX, newP1, newP2);
        newPt1.x = newP1[0];newPt2.x = newP2[0];
        float dy = calcMinSqDist1D(pt1.y, pt2.y, dimY, newP1, newP2);
        newPt1.y = newP1[0];newPt2.y = newP2[0];
        float dz = calcMinSqDist1D(pt1.z, pt2.z, dimZ, newP1, newP2);
        newPt1.z = newP1[0];newPt2.z = newP2[0];
        return dx+dy+dz;
    }// calcMinDistSq
        
    public void run(){    
        for(Boid b : bAra){    findMyNeighbors(b);}                                        //find neighbors to each boid        
        if(stFlags[doHunt] && (flock!=pry)){                                                //flock==pry means only 1 flock
            for(Boid b : bAra){if(b.isHungry()){srchForPrey(b, pry.boidFlock);}}        //find nearby prey to each boid        
        }
        if (stFlags[doSpawn]) {    for(Boid b : bAra){    if(b.canSpawn()){    b.copySubSetBoidsMate(fv.spawnRadSq);    }}    }
    }// run()
    
    @Override
    public Boolean call() throws Exception {
        run(); return true;
    }
    
    
    public String toString(){
        String res = "";
        return res;
    }

    
}//class myStencil