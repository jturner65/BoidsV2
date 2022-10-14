package Boids2_PKG.ui;

import java.util.Map;

import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;

public class myBoidsUIDataUpdater extends UIDataUpdater {

	public myBoidsUIDataUpdater(myDispWindow _win) {
		super(_win);
	}

	public myBoidsUIDataUpdater(myBoidsUIDataUpdater _otr) {
		super(_otr);
	}

	public myBoidsUIDataUpdater(myDispWindow _win, Map<Integer, Integer> _iVals, Map<Integer, Float> _fVals,
			Map<Integer, Boolean> _bVals) {
		super(_win, _iVals, _fVals, _bVals);
	}

}//class myBoidsUIDataUpdater