/**
 * 
 */
package Boids2_PKG.threadedSolvers.initializers;

import java.util.List;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.threadedSolvers.initializers.base.Base_InitPredPreyMaps;
import Boids2_PKG.ui.flkVars.BoidFlockVarsUI;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_UI_Objects.GUI_AppManager;

/**
 * Initialize pred/prey/neighbor maps for non-torroidal bounds
 */
public class InitPredPreyMaps extends Base_InitPredPreyMaps {

    /**
     * @param _AppMgr
     * @param _f
     * @param _pry
     * @param _prd
     * @param _fv
     * @param _flagInt
     * @param _bAra
     */
    public InitPredPreyMaps(GUI_AppManager _AppMgr, BoidFlock _f, BoidFlock _pry, BoidFlock _prd, BoidFlockVarsUI _fv,
            int _flagInt, List<Boid> _bAra) {
        super(_AppMgr, _f, _pry, _prd, _fv, _flagInt, _bAra);
        // TODO Auto-generated constructor stub
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
        //int numAdded = 0;
        for(Boid chk : flock){
            if(chk.ID == _src.ID){continue;}
            distSq = myPointf._SqrDist(_src.getCoords(), chk.getCoords());
            if(distSq>minDistSq){continue;}
            //what if same dist as another? nudge distance a tiny amount. Introduces bias, FIFO    
            distSq = chkPutDistInMap(_src.neighLoc,chk.neighLoc,distSq,_src.getCoords(), chk.getCoords());
            _src.neighbors.put(distSq,chk);
            chk.neighbors.put(distSq, _src);        
        }
    }//srchForNeighbors

    /**
     * Search for prey in specified neighborhood vicinity
     * @param _src
     * @param preyflock
     */
    @Override
    protected void srchForPrey(Boid _src, List<Boid> preyflock) {
        Float distSq;
        if(_src == null){return;}//_src boid might have been eaten
        for(Boid prey : preyflock){
            distSq = myPointf._SqrDist(_src.getCoords(), prey.getCoords());
            if(distSq>fv.predRadSq){continue;}
            //what if same dist as another? nudge distance a tiny amount. Introduces bias, FIFO        
            distSq = chkPutDistInMap(_src.preyFlkLoc,prey.predFlkLoc,distSq,_src.getCoords(), prey.getCoords());
            _src.preyFlk.put(distSq, prey);
        }    
    }//srchForPrey

}// class InitPredPreyMaps
