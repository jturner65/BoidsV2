package Boids2_PKG.ui;

import java.util.LinkedHashMap;

import Boids2_PKG.ui.base.Base_BoidsWindow;
import base_Render_Interface.IGraphicsAppInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;

/**
 * Class for 2D boid environment
 * @author John Turner
 *
 */
public class Boids_2DWin extends Base_BoidsWindow {
    //ui objects should start numbering at Base_BoidsWindow.numBaseGUIObjs
    
    //instance-specific flag idxs should start number at Base_BoidsWindow.numBasePrivFlags
    
    public Boids_2DWin(IGraphicsAppInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
        super(_p, _AppMgr, _winIdx);        
        
    }
    
    /**
     * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons)
     */
    @Override
    public int getTotalNumOfPrivBools() {        return numBasePrivFlags;    }
    
    @Override
    protected void initDispFlags_Indiv() {        
    }
    
    @Override
    protected void initMe_IndivPost() {
    }
    

    @Override
    protected void initTransform() {
        moveTo2DRectCenter();        
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
     * Build all UI objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
     * @param tmpUIObjMap : map of GUIObj_Params, keyed by unique string, with values describing the UI object
     *             - The object IDX                   
     *          - A double array of min/max/mod values                                                   
     *          - The starting value                                                                      
     *          - The label for object                                                                       
     *          - The object type (GUIObj_Type enum)
     *          - A boolean array of behavior configuration values : (unspecified values default to false)
     *               idx 0: value is sent to owning window,  
     *               idx 1: value is sent on any modifications (while being modified, not just on release), 
     *               idx 2: changes to value must be explicitly sent to consumer (are not automatically sent),
     *          - A boolean array of renderer format values :(unspecified values default to false) - Behavior Boolean array must also be provided!
     *                 - Should be multiline
     *                 - One object per row in UI space (i.e. default for multi-line and btn objects is false, single line non-buttons is true)
     *                 - Force this object to be on a new row/line (For side-by-side layouts)
     *                 - Text should be centered (default is false)
     *                 - Object should be rendered with outline (default for btns is true, for non-buttons is false)
     *                 - Should have ornament
     *                 - Ornament color should match label color 
     */
    @Override
    protected final void setupGUIObjsAras_Indiv(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap) {}

    /**
     * Build all UI buttons to be shown in left side bar menu for this window. This is for instancing windows to add to button region
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    @Override
    protected final void setupGUIBoolSwitchAras_Indiv(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {}
    
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
    protected final void handlePrivFlagsDebugMode_Indiv(boolean val) {    }
    
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

}//class Boids_2DWin