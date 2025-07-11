package Boids2_PKG.threadedSolvers.initializers;

import java.util.List;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.threadedSolvers.initializers.base.Base_InitPredPreyMaps;
import Boids2_PKG.ui.flkVars.Boids_UIFlkVars;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_UI_Objects.GUI_AppManager;

/**
 * Initialize pred/prey/neighbor maps for torroidal bounds
 */
public class InitPredPreyMapsTor extends Base_InitPredPreyMaps {
    
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
    public InitPredPreyMapsTor(GUI_AppManager _AppMgr, BoidFlock _f, BoidFlock _pry, BoidFlock _prd, Boids_UIFlkVars _fv,
            int _flagInt, List<Boid> _bAra) {
        super(_AppMgr, _f, _pry, _prd, _fv, _flagInt, _bAra);
    }
    
    /**
     * Look for all neighbors until found neighborhood, expanding distance - keyed by squared distance
     * @param _src
     * @param flock
     * @param min2Dist
     * @param minDistSq
     */
    @Override
    protected void srchForNeighbors(Boid _src, List<Boid> flock, float min2Dist, float minDistSq) {
        Float distSq;
        myPointf tarLoc, srcLoc;
        for(Boid chk : flock){
            if(chk.ID == _src.ID){continue;}
            tarLoc = new myPointf(chk.getCoords()); srcLoc = new myPointf(_src.getCoords());//resetting because may be changed in calcMinSqDist
            distSq = calcMinDistSq(_src.getCoords(), chk.getCoords(), srcLoc, tarLoc, gridDims[0], gridDims[1], gridDims[2], min2Dist);
            if(distSq>minDistSq){continue;}
            //what if same dist as another? nudge distance a tiny amount. Introduces bias, FIFO        
            distSq = chkPutDistInMap(_src.neighLoc,chk.neighLoc,distSq,srcLoc, tarLoc);
            _src.neighbors.put(distSq, chk);
            chk.neighbors.put(distSq, _src);        
        }

    }//srchForNeighbors

    /**
     * Search for prey in specified neighborhood vicinity, accounting for torroidal bounds
     * @param _src
     * @param preyflock
     */
    @Override
    protected void srchForPrey(Boid _src, List<Boid> preyflock) {
        Float distSq, min2dist = (2 * fv.predRad);
        myPointf preyLoc, srcLoc;
        if(_src == null){return;}//_src boid might have been eaten
        for(Boid prey : preyflock){
            preyLoc = new myPointf(prey.getCoords()); srcLoc = new myPointf(_src.getCoords());//resetting because may be changed in calcMinSqDist
            distSq = calcMinDistSq(_src.getCoords(), prey.getCoords(), srcLoc, preyLoc, gridDims[0], gridDims[1], gridDims[2], min2dist);
            if(distSq>fv.predRadSq){continue;}
            //what if same dist as another? nudge distance a tiny amount. Introduces bias, FIFO        
            distSq = chkPutDistInMap(_src.preyFlkLoc, prey.predFlkLoc, distSq, srcLoc, preyLoc);
            _src.preyFlk.put(distSq, prey);    
        }    
    }//srchForPrey

}//class InitPredPreyMapsTor
