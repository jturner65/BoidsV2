package Boids2_PKG.ui;

import java.util.ArrayList;
import java.util.TreeMap;

import Boids2_PKG.ui.base.Base_BoidsWindow;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;

/**
 * Class for 3D boids
 * @author John Turner
 *
 */
public class Boids_3DWin extends Base_BoidsWindow {
	//ui objects should start numbering at Base_BoidsWindow.numBaseGUIObjs
	
	//instance-specific flag idxs should start number at Base_BoidsWindow.numBasePrivFlags
	
	public Boids_3DWin(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
		super(_p, _AppMgr, _winIdx);		
		super.initThisWin(false);
	}

	@Override
	protected int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
		//TODO : Add instance-specific boolean buttons to tmpBtnNamesArray and return new size
		return tmpBtnNamesArray.size();
	}

	@Override
	protected void initMe_IndivPost() {}

	@Override
	protected void initDispFlags_Indiv() {}

	@Override
	protected void initTransform() {
		
		ri.translate(winInitVals.sceneOriginVal);	
	}
	
	/**
	 * Add instance-specific private flags to init to true to those from base class
	 * @param baseFlags base class flags to init to true - add to this array
	 * @return array of base and instance class flags to init to true
	 */
	@Override
	protected int[] getFlagIDXsToInitToTrue_Indiv(int[] baseFlags) {
		//TODO : Add instance-specific flags to those in baseFlags and return new array
		return baseFlags;
	}

	/**
	 * Instance-specific boolean flags to handle
	 * @param idx
	 * @param val
	 * @param oldVal
	 * @return
	 */
	@Override
	protected boolean handlePrivBoidFlags_Indiv(int idx, boolean val, boolean oldVal) {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * Set up ui objects specific to instancing class.
	 * @param tmpUIObjArray
	 * @param tmpListObjVals
	 */
	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray,
			TreeMap<Integer, String[]> tmpListObjVals) {
		// TODO : Add any instance-specific UI objects here		
	}
	
	/**
	 * UI code-level Debug mode functionality. Called only from flags structure
	 * @param val
	 */
	@Override
	protected final void handleDispFlagsDebugMode_Indiv(boolean val) {}
	
	/**
	 * Application-specific Debug mode functionality (application-specific). Called only from privflags structure
	 * @param val
	 */
	@Override
	protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {	}
	
	/**
	 * Handles Instance-specific UI objects
	 * Called if int-handling guiObjs_Numeric[UIidx] (int or list) has new data which updated UI adapter. 
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed!
	 * @param UIidx Index of gui obj with new data
	 * @param ival integer value of new data
	 * @param oldVal integer value of old data in UIUpdater
	 * @return whether the UIidx was found or not
	 */
	@Override
	protected boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal) {
		// TODO :Switch to handle Instance-specific UIidxs
		return false;
	}
	/**
	 * Handles Instance-specific UI objects
	 * Called if float-handling guiObjs_Numeric[UIidx] has new data which updated UI adapter.  
	 * Intended to support custom per-object handling by owning window.
	 * Only called if data changed! 
	 * @param UIidx Index of gui obj with new data
	 * @param val float value of new data
	 * @param oldVal float value of old data in UIUpdater
	 * @return whether the UIidx was found or not
	 */
	@Override
	protected boolean setUI_FloatValsCustom_Indiv(int UIidx, float val, float oldVal) {
		// TODO :Switch to handle Instance-specific UIidxs
		return false;
	}

}//class Boids_3DWin
