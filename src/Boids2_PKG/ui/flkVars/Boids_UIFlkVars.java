package Boids2_PKG.ui.flkVars;

import java.util.Arrays;
import java.util.LinkedHashMap;

import Boids2_PKG.ui.base.Base_BoidsWindow;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.UIObjectManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.base.IUIManagerOwner;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;

/**
 * struct-type class to hold flocking variables for a specific flock. Displayed on sidebar under main window variables
 * @author John Turner
 *
 */
public class Boids_UIFlkVars implements IUIManagerOwner {
    public final int ID;
    //Counter of how many Boids_UIFlkVars are built in the application. 
    //Used to specify unique ID for each new Boids_UIFlkVars
    private static int flkVarCnt = 0;
    /**
     * Owning window
     */
    public final Base_BoidsWindow owner;
    /**
     * Gui-based application manager
     */
    public static GUI_AppManager AppMgr;
    /**
     * class name of instancing class
     */
    protected final String className;    
    /**
     * Manager of all UI objects that make up this construct
     */
    protected UIObjectManager uiMgr;
    
    /**
     * Base_GUIObj that was clicked on for modification
     */
    protected boolean msClickInUIObj;
    
    /**
     * Whether objects have been modified or not
     */
    protected boolean objsModified;
    
    private final float neighborMult = .5f;                   //multiplier for neighborhood consideration against zone size - all rads built off this
    
    /**
     * TODO modify this via UI
     * multiplier for damping force, to slow boats down if nothing else acting on them
     */
    public float dampConst = .01f;                            
    
    public float nghbrRad,                                    //radius of the creatures considered to be neighbors
                nghbrRadSq,
                colRad,                                        //radius of creatures to be considered for collision avoidance
                colRadSq,
                velRad,                                        //radius of creatures to be considered for velocity matching
                velRadSq,
                predRad,                                    //radius for creature to be considered for pred/prey (to start avoiding or to chase)
                predRadSq,
                spawnPct,                                    //% chance to reproduce given the boids breech the required radius
                spawnRad,                                    //distance to spawn * mass
                spawnRadSq,
                killPct,                                    //% chance to kill prey creature
                killRad,                                    //distance to kill * mass (distance required to make kill attempt)
                killRadSq;
    
    public int spawnFreq,                                     //# of cycles that must pass before can spawn again
                eatFreq,                                     //# cycles w/out food until starve to death                
                canSprintCycles;                            //cycles after eating where sufficiently full that boid can sprint
    
    /**
     * Minimum allowed radius of any kind
     */
    private final float minRad;
    /**
     * max allowed neighborhood radius - min dim of cube
     */
    private final float maxNeighborRad; 
    
    /**
     * min allowed neighborhood radius - 10% of max
     */
    private final float minNeighborRad;
    /**
     * radius modifier
     */
    private final float radMod;
    
    public float totMaxRad;                        //max search distance for neighbors
    public int nearCount;                        //# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
    public final int nearMinCnt = 5;            //smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
    
    public double[] massForType;        //
    public float[] maxFrcs;                                    //max forces for this flock, for each force type
    public float[] wts;                                        //weights for flock calculations for this flock
    
    public final float maxVelMag = 80,        //max velocity for flock member 
                minVelMag = 0.0f;                                        //min velocity for flock member
    
    // default min and max values
    private static final float[] 
            defWtAra = new float[]{.5f, .75f, .5f, .5f, .5f, .1f},              //default array of weights for different forces
            defOrigWtAra = new float[]{5.0f, 12.0f, 7.0f, 3.5f, .5f, .1f},      //default array of weights for different forces using original calcs
            MaxWtAra = new float[]{15, 15, 15, 15, 15, 15},                                
            MinWtAra = new float[]{.01f, .01f, .01f, .01f, .001f, .001f};
      
    private static final float
            minPct = 0.001f,
            maxPct = 100.0f;
    
    private static final int
            minCycles = 10,
            maxCycles = 10000;

    private final int[] _flkColor;
    
    /**
     * Name of flock this UI construct services/represents
     */
    public final String typeName;
    
    /**
     * Idxs for the UI objects this construct holds
     */
    public final static int 
        //These first 6 weights' idxs also match indices in weights array
        gIDX_FlkFrcWt              = 0,
        gIDX_ColAvoidWt            = 1,
        gIDX_VelMatchWt            = 2,
        gIDX_WanderFrcWt           = 3,
        gIDX_PredAvoidWt           = 4,
        gIDX_PreyChaseWt           = 5,
        // Other ui objects
        gIDX_FlkRad                = 6,
        gIDX_ColAvoidRad           = 7,
        gIDX_VelMatchRad           = 8,
        gIDX_SpawnRad              = 9,
        gIDX_SpawnSuccessPct       = 10,
        gIDX_SpawnFrequency        = 11,
        gIDX_HuntingRad            = 12,
        gIDX_HuntingSuccessPct     = 13,
        gIDX_HuntingFrequency      = 14;
    public final static int numFlockUIObjs = 15;
    // offset value to use to specify a label of a section
    private final static int lblOffset = 100000;
    public final static int
        disp_FlockName          = 1000,
        disp_PredFlockName      = 1001,
        disp_PreyFlockName      = 1002,
        disp_FlockCount         = 1003,
        disp_MassRange          = 1004,
        disp_VelRange           = 1005;
    
    /**
     * Index in owner's flock vars array
     */
    public final int flockIdx;
    
    /**
     * Index of predator flock
     */
    public int predIdx;
    /**
     * Index of prey flock
     */
    public int preyIdx;
    /**
     * 
     * @param _owner
     * @param _flockIdx
     * @param _flockName
     * @param _nRadMult
     * @param _predRad
     */
    public Boids_UIFlkVars(Base_BoidsWindow _owner, int _flockIdx, String _flockName, float _nRadMult, float _predRad, int[] _flkClr) {
        ID = flkVarCnt++;
        flockIdx = _flockIdx;
        predIdx = _flockIdx;
        preyIdx = _flockIdx;
        owner = _owner;
        AppMgr = Base_BoidsWindow.AppMgr;
        className = this.getClass().getSimpleName();
        typeName = _flockName;
        msClickInUIObj = false;
        // max neighborhood radius
        maxNeighborRad = _predRad*neighborMult;
        minNeighborRad = 0.01f * maxNeighborRad;
        minRad = 1.0f;
        radMod = minRad;
        _flkColor = new int[_flkClr.length];
        System.arraycopy(_flkClr, 0, _flkColor, 0, _flkClr.length);
        uiMgr = new UIObjectManager(Base_DispWindow.ri, this, AppMgr, _owner.getMsgObj());
        _initFlockVals(_nRadMult, .05f, _predRad);
    }//ctor
    
    public float getInitMass(){return (float)(massForType[0] + (massForType[1] - massForType[0])*MyMathUtils.randomFloat());}
    
    /**
     * Set the predator and prey idx of the flock this vars represents
     * @param _predIdx
     * @param _preyIdx
     */
    public void setPredPreyIDX(int _predIdx, int _preyIdx) {
        predIdx = _predIdx;        preyIdx = _preyIdx;        
        uiMgr.setNewUIValue(disp_PredFlockName, predIdx);
        uiMgr.setNewUIValue(disp_PreyFlockName, preyIdx);
        uiMgr.setAllUIWinVals();
    }
    
    /**
     * 
     * @param _initRadMult
     * @param _initSpnPct
     * @param _initPredRad
     */
    private void _initFlockVals(float _initRadMult, float _initSpnPct, float _initPredRad){
        //radius to avoid pred/find prey
        predRad = _initPredRad;        
        predRadSq = predRad * predRad;
        
        nghbrRad = maxNeighborRad*_initRadMult;
        nghbrRadSq = nghbrRad *nghbrRad;
        colRad  = nghbrRad*.5f;
        colRadSq = colRad*colRad;
        velRad  = nghbrRad*.5f;     
        velRadSq = velRad * velRad;
        //weight multiplier for forces - centering, avoidance, velocity matching and wander
        spawnPct = _initSpnPct;        //% chance to reproduce given the boids breech the required radius
        spawnRad = 1.1f*colRad;            //distance to spawn 
        spawnRadSq = spawnRad * spawnRad;
        spawnFreq = 500;         //# of cycles that must pass before can spawn again
        //required meal time
        eatFreq = 500;             //# cycles w/out food until starve to death
        setCanSprintCycles();
        killRad = minRad;                        //radius to kill * mass
        killRadSq = killRad * killRad;
        killPct = .01f;                //% chance to kill prey creature

        wts = new float[defWtAra.length]; 
        System.arraycopy( defWtAra, 0, wts, 0, defWtAra.length );
        massForType = new double[]{2.0f*_initRadMult,4.0f*_initRadMult, 0.01f}; //_initRadMult should vary from 1.0 to .25
        maxFrcs = new float[]{100,200,100,10,400,20};        //maybe scale forces?
    }
    
    /**
     * Build the UI for this flock vars
     */
    public final void initFlkVarsUI() {
        // build all ui objects
        uiMgr.initAllGUIObjects();     
    }
            
    /**
     * UIObjectManager will call this if there are any privState flags to modify
     */
    @Override
    public final void initOwnerStateDispFlags() {}
    
    /**
     * UI Manager access to this function to retrieve appropriate initial uiClkCoords.
     * @return
     */
    @Override
    public final float[] getOwnerParentWindowUIClkCoords() {
        float[] clickCoords = owner.getPrevFlkVarsUIClckCoords(flockIdx);
        return clickCoords;
    }

    /**
     * if set default weight mults based on whether using force calcs based on original inverted distance functions or linear distance functions
     * @param useOrig
     */
    public final void setDefaultWtVals(boolean useOrig){    
        float[] srcAra = (useOrig ? defOrigWtAra: defWtAra);
        for(int i=0;i<srcAra.length;++i) {            uiMgr.setNewUIValue(i, srcAra[i]);       }
        uiMgr.setAllUIWinVals();        
    }
    
    /**
     * If true, clip all radii to have a maximum of current neighbor radius, otherwise use default max radius possible for neighborhood
     * @param clipToNRad
     */
    public final void setClipToNeighborRad(boolean clipToNRad) {
        float maxRadiusToUse = (float) (clipToNRad ? uiMgr.getUIValue(gIDX_FlkRad) : maxNeighborRad);
        // set max radii based on specification
        uiMgr.setNewUIMaxVal(gIDX_ColAvoidRad, maxRadiusToUse);
        uiMgr.setNewUIMaxVal(gIDX_VelMatchRad, maxRadiusToUse);
        uiMgr.setNewUIMaxVal(gIDX_SpawnRad, maxRadiusToUse);
        uiMgr.setNewUIMaxVal(gIDX_HuntingRad, maxRadiusToUse); 
        uiMgr.setAllUIWinVals(); 
    }//setClipToNeighborRad
    
    /**
     * Draw the UI this flkVars manages
     * @param animTimeMod
     */
    public final void drawMe(float animTimeMod) {        uiMgr.drawGUIObjs(AppMgr.isDebugMode(), animTimeMod);   }
    
    @Override
    public final void moveToUIRegion() {uiMgr.moveToUIRegion();}

    @Override
    public final int getID() {return ID;}
    @Override
    public String getClassName() {return className;    }
    /**
     * This function is called on ui value update, to pass new ui values on to window-owned consumers
     */
    @Override
    public final void updateOwnerCalcObjUIVals() {   
        //uiMgr._dispErrMsg("updateOwnerCalcObjUIVals : "+getName(),"Called to update UI vals");
    }
    
    /**
     * Called if int-handling guiObjs[UIidx] (int or list) has new data which updated UI adapter. 
     * Intended to support custom per-object handling by owning window.
     * Only called if data changed!
     * @param UIidx Index of gui obj with new data
     * @param ival integer value of new data
     * @param oldVal integer value of old data in UIUpdater
     */
    @Override
    public final void setUI_OwnerIntValsCustom(int UIidx, int ival, int oldVal) { 
        switch(UIidx) {                  
            case gIDX_SpawnFrequency      :{
                spawnFreq = ival;
                break;}                    
            case gIDX_HuntingFrequency      :{
                eatFreq = ival;
                setCanSprintCycles();
                break;}   
            default:{
                uiMgr._dispErrMsg("setUI_OwnerIntValsCustom : "+getName(), "Unknown/unsupported UI Object index : "+ UIidx+" attempting to be set to "+ival);
                               
                break;}
        } 
    }//setUI_OwnerIntValsCustom
    
    /**
     * Set the number of cycles the boid can sprint to be 1/4 the current eat frequency requirement
     */
    private void setCanSprintCycles() {canSprintCycles = (int) (.25f * eatFreq);}
    
    /**
     * Called if float-handling guiObjs[UIidx] has new data which updated UI adapter.  
     * Intended to support custom per-object handling by owning window.
     * Only called if data changed!
     * @param UIidx Index of gui obj with new data
     * @param val float value of new data
     * @param oldVal float value of old data in UIUpdater
     */
    @Override
    public final void setUI_OwnerFloatValsCustom(int UIidx, float val, float oldVal) {    
        switch(UIidx) {
            case gIDX_FlkFrcWt          :{     wts[gIDX_FlkFrcWt] = val;     break;}       
            case gIDX_ColAvoidWt        :{     wts[gIDX_ColAvoidWt] = val;   break;}           
            case gIDX_VelMatchWt        :{     wts[gIDX_VelMatchWt] = val;   break;}            
            case gIDX_WanderFrcWt       :{     wts[gIDX_WanderFrcWt] = val;  break;}           
            case gIDX_PredAvoidWt       :{     wts[gIDX_PredAvoidWt] = val;  break;}           
            case gIDX_PreyChaseWt       :{     wts[gIDX_PreyChaseWt] = val;  break;}           
            case gIDX_FlkRad            :{
                uiMgr._dispWarnMsg("setUI_OwnerFloatValsCustom", "Setting new nghbrRad from " + nghbrRad + " to "+val);
                nghbrRad = val;
                //specify max radius to use for other radii 
                setClipToNeighborRad(owner.getClipMaxRadToNeighbor());
                nghbrRadSq = nghbrRad * nghbrRad; 
                break;}
            case gIDX_ColAvoidRad       :{
                uiMgr._dispWarnMsg("setUI_OwnerFloatValsCustom", "Setting new colRad from " + colRad + " to "+val);
                colRad = val;
                colRadSq = colRad * colRad;
                break;}            
            case gIDX_VelMatchRad       :{
                uiMgr._dispWarnMsg("setUI_OwnerFloatValsCustom", "Setting new velRad from " + velRad + " to "+val);
                velRad = val;
                velRadSq = velRad * velRad;
                break;}            
            case gIDX_SpawnRad          :{
                uiMgr._dispWarnMsg("setUI_OwnerFloatValsCustom", "Setting new spawnRad from " + spawnRad + " to "+val);
                spawnRad = val;
                spawnRadSq = spawnRad*spawnRad;                 
                break;}              
            case gIDX_SpawnSuccessPct   :{
                spawnPct = val;
                break;}           
            case gIDX_HuntingRad        :{
                uiMgr._dispWarnMsg("setUI_OwnerFloatValsCustom", "Setting new predRad from " + predRad + " to "+val);
                predRad = val;
                predRadSq = predRad * predRad;
                break;}             
            case gIDX_HuntingSuccessPct :{
                killPct = val;
                break;}  
            default:{
                uiMgr._dispErrMsg("setUI_OwnerFloatValsCustom : "+getName(), "Unknown/unsupported UI Object index : "+ UIidx+" attempting to be set to "+val);                
                break;}
        }
    }//setUI_OwnerFloatValsCustom
        
    /**
     * This will set the flock size display label to have the correct value
     * @param flockSize
     */
    public final void setFlockSize(int flockSize) {
        uiMgr.setNewUIValue(disp_FlockCount, flockSize);
    }
    /**
     * Build flkVars UIDataUpdater instance for application
     * @return
     */    
    @Override
    public UIDataUpdater buildOwnerUIDataUpdateObject() {
        return new Boids_FlkVars(this);
    }
    /**
     * Retrieve the Owner's UIDataUpdater. Use this to update local UI variables.
     * @return
     */
    @Override
    public UIDataUpdater getUIDataUpdater() {return uiMgr.getUIDataUpdater();}    
    
    /**
     * Build all UI objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
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
    public final void setupOwnerGUIObjsAras(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap) {
        
        //build list select box values
        //keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
        // For entire group
        LinkedHashMap<String, GUIObj_Params> tmpUIGrpBuilderMap = new LinkedHashMap<String, GUIObj_Params>(); 
        // zero row labels for count, name, velocity and mass limits
        int grpIdx = 0;
        tmpUIObjMap.put("disp_FirstSpacer", uiMgr.uiObjectInitAra_Spacer(new float[] {0, 10}));
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_space", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();               
        
        //badge spacer
        tmpUIObjMap.put("disp_BadgeSpacer", uiMgr.uiObjectInitAra_Spacer(owner.getFlockBadgeDims(flockIdx)));
        tmpUIObjMap.put("disp_FlockName", uiMgr.uiObjInitAra_DispString(disp_FlockName, flockIdx, "  Flock Name", owner.getFlknNames(), false, false));
        tmpUIObjMap.put("disp_FlockCount",uiMgr.uiObjInitAra_DispInt(disp_FlockCount, 0, "Pop", true, false));
        tmpUIObjMap.put("disp_MassRange", uiMgr.uiObjInitAra_DispFloatRange(disp_MassRange, massForType, 0, "Mass", true, false));         
        tmpUIObjMap.put("disp_VelRange", uiMgr.uiObjInitAra_DispFloatRange(disp_VelRange, new double[]{minVelMag, maxVelMag, 0.1}, 0, "Velocity", true, false));        
        //assign colors
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}
        tmpUIObjMap.get("disp_BadgeSpacer").setReadOnlyColors(new int[] {255,255,255,0});
        var lblGroup = uiMgr.buildUIObjGroupParams(tmpUIObjMap);
        lblGroup.setNumObjsPerLine(50);
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_header", lblGroup);
        tmpUIObjMap.clear();
        
        // first row - prey and predators
        tmpUIObjMap.put("disp_PredFlockName", uiMgr.uiObjInitAra_DispString(disp_PredFlockName, predIdx, "  Predator Name", owner.getFlknNames(), false, false));
        tmpUIObjMap.put("disp_PreyFlockName", uiMgr.uiObjInitAra_DispString(disp_PreyFlockName, preyIdx, "  Prey Name", owner.getFlknNames(), false, false));
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_preyPred", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();        
        
        
        //second row - radii
        tmpUIObjMap.put("label_radius", uiMgr.uiObjInitAra_Label(gIDX_FlkRad+lblOffset,"Radii : "));
        var rndrConfig = uiMgr.buildGUIObjRendererFlags(false, false, false, false);
        rndrConfig.setHasOrnament(false);rndrConfig.setHasOutline(true);
        tmpUIObjMap.put("gIDX_FlkRad", uiMgr.uiObjInitAra_Float(gIDX_FlkRad, new double[]{minNeighborRad,maxNeighborRad,radMod}, nghbrRad, "  Flocking", uiMgr.buildDefaultGUIObjConfigFlags(), rndrConfig));
        tmpUIObjMap.put("gIDX_ColAvoidRad", uiMgr.uiObjInitAra_Float(gIDX_ColAvoidRad, new double[]{minRad,maxNeighborRad,radMod}, colRad, "  Col Avoid", uiMgr.buildDefaultGUIObjConfigFlags(), rndrConfig));
        tmpUIObjMap.put("gIDX_VelMatchRad", uiMgr.uiObjInitAra_Float(gIDX_VelMatchRad, new double[]{minRad,maxNeighborRad,radMod}, velRad, "  Vel Match", uiMgr.buildDefaultGUIObjConfigFlags(), rndrConfig));
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}

        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_radii", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();
        
        //third row - weights
        tmpUIObjMap.put("label_weights", uiMgr.uiObjInitAra_Label(gIDX_FlkFrcWt+lblOffset,"Wts : "));
        int wIdx = gIDX_FlkFrcWt;
        tmpUIObjMap.put("gIDX_FlkFrcWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Flock", true, false));
        wIdx = gIDX_ColAvoidWt;
        tmpUIObjMap.put("gIDX_ColAvoidWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Col Avoid", true, false));
        wIdx = gIDX_VelMatchWt;
        tmpUIObjMap.put("gIDX_VelMatchWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Vel Match", true, false));
        wIdx = gIDX_WanderFrcWt;
        tmpUIObjMap.put("gIDX_WanderFrcWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Wander", true, false));
        wIdx = gIDX_PredAvoidWt;
        tmpUIObjMap.put("gIDX_PredAvoidWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Pred Avoid", true, false));
        wIdx = gIDX_PreyChaseWt;
        tmpUIObjMap.put("gIDX_PreyChaseWt", uiMgr.uiObjInitAra_Float(wIdx, new double[]{MinWtAra[wIdx],MaxWtAra[wIdx],MinWtAra[wIdx]}, wts[wIdx], "Chase Prey", true, false));
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}

        var wtsObjParams = uiMgr.buildUIObjGroupParams(tmpUIObjMap);
        wtsObjParams.setNumObjsPerLine(50);
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_weights", wtsObjParams);
        tmpUIObjMap.clear();
        // fourth row labels
        tmpUIObjMap.put("label_activity", uiMgr.uiObjInitAra_Label(gIDX_SpawnRad+10000,"Activity"));
        tmpUIObjMap.put("label_actRadius", uiMgr.uiObjInitAra_Label(gIDX_SpawnRad+10001,"Radius"));
        tmpUIObjMap.put("label_actSuccess", uiMgr.uiObjInitAra_Label(gIDX_SpawnRad+10002,"% Success"));
        tmpUIObjMap.put("label_actCycles", uiMgr.uiObjInitAra_Label(gIDX_SpawnRad+10003,"# Cycles")); 
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}
        
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_activityLabels", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();
        
        // fifth row spawning
        tmpUIObjMap.put("label_spawning", uiMgr.uiObjInitAra_Label(gIDX_SpawnRad+lblOffset,"Spawning : "));
        tmpUIObjMap.put("gIDX_SpawnRad", uiMgr.uiObjInitAra_Float(gIDX_SpawnRad, new double[]{minRad,maxNeighborRad,radMod}, spawnRad, "", false, false));
        tmpUIObjMap.put("gIDX_SpawnSuccessPct", uiMgr.uiObjInitAra_Float(gIDX_SpawnSuccessPct, new double[]{minPct,maxPct, minPct}, spawnPct, "", false, false));
        tmpUIObjMap.put("gIDX_SpawnFrequency", uiMgr.uiObjInitAra_Int(gIDX_SpawnFrequency, new double[]{minCycles, maxCycles, 1.0f}, spawnFreq, "", false, false));
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}
      
        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_spawning", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();
        
        // sixth row hunting
        tmpUIObjMap.put("label_spawning", uiMgr.uiObjInitAra_Label(gIDX_HuntingRad+lblOffset,"Hunting : "));
        tmpUIObjMap.put("gIDX_HuntingRad", uiMgr.uiObjInitAra_Float(gIDX_HuntingRad, new double[]{minRad, maxNeighborRad,radMod}, killRad, "", false, false));
        tmpUIObjMap.put("gIDX_HuntingSuccessPct", uiMgr.uiObjInitAra_Float(gIDX_HuntingSuccessPct, new double[]{minPct, maxPct, minPct}, killPct, "", false, false));
        tmpUIObjMap.put("gIDX_HuntingFrequency", uiMgr.uiObjInitAra_Int(gIDX_HuntingFrequency, new double[]{minCycles, maxCycles, 1.0f}, eatFreq, "", false, false));
        for (var entry : tmpUIObjMap.entrySet()) {entry.getValue().setReadOnlyColors(_flkColor);}

        tmpUIGrpBuilderMap.put("row_"+(grpIdx++)+"_hunting", uiMgr.buildUIObjGroupParams(tmpUIObjMap));
        tmpUIObjMap.clear();
        
        //copy all elements into map
        tmpUIObjMap.putAll(tmpUIGrpBuilderMap);
        
    }//setupOwnerGUIObjsAras
    /**
     * Build UI button objects to be shown in left side bar menu for this window. This is the first child class function called by initThisWin
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    @Override
    public final void setupOwnerGUIBoolSwitchAras(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {    }
    /**
     * Retrieve the total number of defined privFlags booleans (application-specific state bools and interactive buttons, to be linked with UI Switches)
     */
    @Override
    public final int getTotalNumOfPrivBools() {        return 0;    }

    @Override
    public final int[] getOwnerFlagIDXsToInitToTrue() {    return new int[0];}

    /**
     * Called by privFlags bool struct, to update uiUpdateData when boolean flags have changed
     * @param idx
     * @param val
     */
    @Override
    public final void checkSetBoolAndUpdate(int idx, boolean val) {uiMgr.checkSetBoolAndUpdate(idx, val);    }
    
    /**
     * These are called externally from execution code object to synchronize ui values that might change during execution
     * @param idx of particular type of object
     * @param value value to set
     */
    @Override
    public final void updateBoolValFromExecCode(int idx, boolean value) {uiMgr.updateBoolValFromExecCode(idx, value);}
    /**
     * These are called externally from execution code object to synchronize ui values that might change during execution
     * @param idx of particular type of object
     * @param value value to set
     */
    @Override
    public final void updateIntValFromExecCode(int idx, int value) {uiMgr.updateIntValFromExecCode(idx, value);}
    /**
     * These are called externally from execution code object to synchronize ui values that might change during execution
     * @param idx of particular type of object
     * @param value value to set
     */
    @Override
    public final void updateFloatValFromExecCode(int idx, float value) {uiMgr.updateFloatValFromExecCode(idx, value);}
    /**
     * Return the coordinates of the clickable region for this construct's UI
     * @return
     */
    @Override
    public final float[] getUIClkCoords() {return uiMgr.getUIClkCoords();}

    @Override
    public final void handleOwnerPrivFlags(int idx, boolean val, boolean oldVal) {}

    @Override
    public final void handleOwnerPrivFlagsDebugMode(boolean val) {}
    
    ///////////////////////////////////////////////////////
    /// Start mouse interaction
    
    /**
     * Handle mouse interaction via a mouse click
     * @param mouseX current mouse x on screen
     * @param mouseY current mouse y on screen
     * @param mseBtn which button is pressed : 0 is left, 1 is right
     * @return whether a UI object was clicked in
     */
    @Override
    public final boolean handleMouseClick(int mouseX, int mouseY, int mseBtn){
        boolean[] retVals = new boolean[] {false,false};
        msClickInUIObj = uiMgr.handleMouseClick(mouseX, mouseY, mseBtn, AppMgr.isClickModUIVal(), retVals);
        if (retVals[1]){objsModified = true;}
        if (retVals[0]){return true;}
        return false;
    }//handleMouseClick
        
    /**
     * Handle mouse interaction via the clicked mouse drag
     * @param mouseX current mouse x on screen
     * @param mouseY current mouse y on screen
     * @param pmouseX previous mouse x on screen
     * @param pmouseY previous mouse y on screen
     * @param mseDragInWorld vector of mouse drag in the world, for interacting with trajectories
     * @param mseBtn what mouse btn is pressed
     * @return whether a UI object has been modified via a drag action
     */
    @Override
    public final boolean handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myVector mseDragInWorld, int mseBtn){
        if (msClickInUIObj) {
            int delX = (mouseX-pmouseX), delY = (mouseY-pmouseY);
            boolean shiftPressed = AppMgr.shiftIsPressed();
            boolean retVals[] = uiMgr.handleMouseDrag(delX, delY, mseBtn, shiftPressed);
            if (retVals[1]){objsModified = true;}
            if (retVals[0]){return true;}
        }
        return false;
    }//handleMouseDrag
    
    /**
     * Handle mouse interaction via the mouse moving over a UI object
     * @param mouseX current mouse x on screen
     * @param mouseY current mouse y on screen
     * @return whether a UI object has the mouse pointer moved over it
     */
    @Override
    public final boolean handleMouseMove(int mouseX, int mouseY) {
        boolean uiObjMseOver = uiMgr.handleMouseMove(mouseX, mouseY);
        if (uiObjMseOver){return true;}
        return false;
    }
    
    /**
     * Handle mouse interaction via the mouse wheel
     * @param ticks
     * @param mult amount to modify view based on sensitivity and whether shift is pressed or not
     * @return whether a UI object has been modified via the mouse wheel
     */
    @Override
    public final boolean handleMouseWheel(int ticks, float mult) {
        if (msClickInUIObj) {
            //modify object that was clicked in by mouse motion
            boolean retVals[] = uiMgr.handleMouseWheel(ticks, mult);
            if (retVals[1]){objsModified = true;}
            if (retVals[0]){return true;}        
        }
        return false;
    }//handleMouseWheel
    
    /**
     * Handle mouse interactive when the mouse button is released - in general consider this the end of a mouse-driven interaction
     */
    @Override
    public final void handleMouseRelease(){
        //TODO no buttons currently built to be cleared on future draw
        msClickInUIObj = false;
        objsModified = false;
        if(uiMgr.handleMouseRelease(objsModified)) {}
    }//handleMouseRelease
    
    ///////////////////////////////////////////////////////
    /// End mouse interaction
   
    /**
     * Return the Owner's assigned name, for messages
     * @return
     */
    @Override
    public final String getName() {return "FlkVars for "+typeName;   }
    
    @Override
    public String toString(){
        String[] resAra = new String[]{
                "Flock Vars for " + typeName + " Lim: V: ["+String.format("%.2f", (minVelMag))+"," + String.format("%.2f", (maxVelMag))+"] M ["+ String.format("%.2f", (massForType[0])) + "," + String.format("%.2f", (massForType[1]))+"]" ,
                "Radius : Fl : "+String.format("%.2f",nghbrRad)+ " |  Avd : "+String.format("%.2f",colRad)+" |  VelMatch : "+ String.format("%.2f",velRad),
               // "           "+(nghbrRad > 10 ?(nghbrRad > 100 ? "":" "):"  ")+String.format("%.2f",nghbrRad)+" | "+(colRad > 10 ?(colRad > 100 ? "":" "):"  ")+String.format("%.2f",colRad)+" | "+(velRad > 10 ?(velRad > 100 ? "":" "):"  ")+ String.format("%.2f",velRad),
                "Wts: Ctr |  Avoid | VelM | Wndr | AvPrd | Chase" ,
                "     "+String.format("%.2f", wts[gIDX_FlkFrcWt])
                       +"  |  "+String.format("%.2f", wts[gIDX_ColAvoidWt])
                       +"  |  "+String.format("%.2f", wts[gIDX_VelMatchRad])
                       +"  |  "+String.format("%.2f", wts[gIDX_WanderFrcWt])
                       +"  |  "+String.format("%.2f", wts[gIDX_PredAvoidWt])
                       +"  |  "+String.format("%.2f", wts[gIDX_PreyChaseWt]),
               "                  % success |  radius  |  # cycles.",
               "Spawning : "+(spawnPct > .1f ? "" : " ")+String.format("%.2f", (spawnPct*100))+" | "+(spawnRad > 10 ?(spawnRad > 100 ? "":" "):"  ")+String.format("%.2f", spawnRad)+" | "+spawnFreq,
               "Hunting   :  "+(killPct > .1f ? "" : " ")+String.format("%.2f", (killPct*100))+" | "+(predRad > 10 ?(predRad > 100 ? "":" "):"  ")+String.format("%.2f", predRad)+" | "+eatFreq,    
               " "};    
        return Arrays.toString(resAra);
    }
   
}//class myFlkVars 
