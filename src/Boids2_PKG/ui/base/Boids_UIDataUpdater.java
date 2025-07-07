package Boids2_PKG.ui.base;

import java.util.Map;

import base_UI_Objects.windowUI.uiData.UIDataUpdater;

public class Boids_UIDataUpdater extends UIDataUpdater {

    public Boids_UIDataUpdater(Base_BoidsWindow _win) {
        super(_win);
    }

    public Boids_UIDataUpdater(Base_BoidsWindow _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
            Map<Integer, Boolean> _bVals) {
        super(_win, _iVals, _fVals, _bVals);
    }
    
    public Boids_UIDataUpdater(Boids_UIDataUpdater _otr) {
        super(_otr);
    }


}//class myBoidsUIDataUpdater
