/**
 * 
 */
package Boids2_PKG.ui.flkVars;

import java.util.Map;

import base_UI_Objects.windowUI.uiData.UIDataUpdater;

/**
 * structure holding UI-derived/modified data used to update execution code, specifically for flock variables
 */
public class Boids_UIFlkVarDataUpdater extends UIDataUpdater {

    /**
     * @param _flkVars owning flock vars for this data updater
     */
    public Boids_UIFlkVarDataUpdater(Boid_UIFlkVars _flkVars) {
        super(_flkVars);
    }

    /**
     * @param _win
     * @param _iVals
     * @param _fVals
     * @param _bVals
     */
    public Boids_UIFlkVarDataUpdater(Boid_UIFlkVars _flkVars, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
            Map<Integer, Boolean> _bVals) {
        super(_flkVars, _iVals, _fVals, _bVals);
    }

    /**
     * @param _otr
     */
    public Boids_UIFlkVarDataUpdater(UIDataUpdater _otr) {
        super(_otr);
    }

}//class Boids_UIFlkVarDataUpdater 
