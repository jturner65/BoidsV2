package Boids2_PKG.threadedSolvers.forceSolvers;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.threadedSolvers.forceSolvers.base.Base_ForceSolver;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;

public class LinearForceSolver extends Base_ForceSolver{
    public LinearForceSolver(GUI_AppManager _AppMgr, BoidFlock _f, int _flagInt, boolean _isClk, List<Boid> _bAra) {
        super(_AppMgr,_f,_flagInt, _isClk, _bAra);
    }
    
    //collect to center of local group
    @Override
    protected myVectorf frcToCenter(Boid b){
        float wtSqSum = 0, wtDist;    
        myVectorf frcVec = new myVectorf();
        for(Float bd_k : b.neighLoc.keySet()){    
            wtDist = bd_k;
            frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(bd_k), b.getCoords()), wtDist));
            wtSqSum += wtDist;    
        }
        frcVec._div(wtSqSum);                
        return frcVec;
    }//frcToCenter

    //avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
    @Override
    protected myVectorf frcAvoidCol(Boid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh){
        myVectorf frcVec = new myVectorf(), tmpVec;
        float subRes;
        for(Float bd_k : otherLoc.keySet()){    //already limited to those closer than colRadSq
            tmpVec = myVectorf._sub(b.getCoords(),otherLoc.get(bd_k));
            subRes = (frcThresh - bd_k);     //old
            frcVec._add(myVectorf._mult(tmpVec, subRes ));
        }
        return frcVec;
    }//frcAvoidCol
    
    @Override
    protected myVectorf frcVelMatch(Boid b){
        float dsq;
        myVectorf frcVec = new myVectorf();        
        for(Float bd_k : b.neighbors.keySet()){    
            if(bd_k>fv.velRadSq){continue;}
            dsq=(fv.velRadSq-bd_k);
            //frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
            frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity, b.velocity), dsq));
        }
        return frcVec;
    }
    
    
}//myLinForceStencil