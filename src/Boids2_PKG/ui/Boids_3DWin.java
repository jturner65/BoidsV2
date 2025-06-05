package Boids2_PKG.ui;

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

	/**
	 * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
	 */
	@Override
	public int getTotalNumOfPrivBools() {		return numBasePrivFlags;	}

	@Override
	protected void initMe_IndivPost() {}

	@Override
	protected void initDispFlags_Indiv() {
		dispFlags.setDrawMseEdge(true);
	}

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
	 * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
	 * @param tmpUIObjArray : map of object data, keyed by UI object idx, with array values being :                    
	 *           the first element double array of min/max/mod values                                                   
	 *           the 2nd element is starting value                                                                      
	 *           the 3rd elem is label for object                                                                       
	 *           the 4th element is object type (GUIObj_Type enum)
	 *           the 5th element is boolean array of : (unspecified values default to false)
	 *           	idx 0: value is sent to owning window,  
	 *           	idx 1: value is sent on any modifications (while being modified, not just on release), 
	 *           	idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
	 *           the 6th element is a boolean array of format values :(unspecified values default to false)
	 *           	idx 0: whether multi-line(stacked) or not                                                  
	 *              idx 1: if true, build prefix ornament                                                      
	 *              idx 2: if true and prefix ornament is built, make it the same color as the text fill color.
	 * @param tmpListObjVals : map of string arrays, keyed by UI object idx, with array values being each element in the list
	 * @param tmpBtnNamesArray : map of Object arrays to be built containing all button definitions, keyed by sequential value == objId
	 * 				the first element is true label
	 * 				the second element is false label
	 */
	@Override
	protected void setupGUIObjsAras_Indiv(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals, int firstBtnIDX, TreeMap<Integer,Object[]> tmpBtnNamesArray) {}
	
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
