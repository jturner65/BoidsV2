package Boids2_PKG.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.threadedSolvers.updaters.BoidHuntUpdater;
import Boids2_PKG.threadedSolvers.updaters.BoidUpdate_Type;
import Boids2_PKG.ui.flkVars.Boids_UIFlkVars;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.renderedObjs.Boat_RenderObj;
import base_UI_Objects.renderedObjs.JFish_RenderObj;
import base_UI_Objects.renderedObjs.Sphere_RenderObj;
import base_UI_Objects.renderedObjs.base.Base_RenderObj;
import base_UI_Objects.renderedObjs.base.RenderObj_ClrPalette;
import base_UI_Objects.renderer.ProcessingRenderer;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_UI_Objects.windowUI.drawnTrajectories.DrawnSimpleTraj;
import base_UI_Objects.windowUI.uiData.UIDataUpdater;
import base_UI_Objects.windowUI.uiObjs.base.GUIObj_Params;
import base_Utils_Objects.io.messaging.MsgCodes;
import processing.core.PImage;

public abstract class Base_BoidsWindow extends Base_DispWindow {
    
    /**
     * idxs - need one per ui object
     */
    public final static int
        gIDX_TimeStep       = 0,
        gIDX_NumFlocks      = 1,
        gIDX_BoidType       = 2,
        gIDX_FlockToObs     = 3,
        gIDX_ModNumBoids    = 4,
        gIDX_BoidToObs      = 5;

    public static final int numBaseGUIObjs = 6;                                            //# of gui objects for ui
    
    /**
     * private child-class flags - window specific
     */
    public static final int 
            //debug is 0
            showBoidFrame        = 1,            //Show the boids RGB axes, with red being in direction of velocity
            drawBoids            = 2,            //whether to draw boids or draw spheres (renders faster)
            drawScaledBoids      = 3,            //whether to draw boids scaled by their mass
            clearPath            = 4,            //whether to clear each drawn boid, or to show path by keeping past drawn boids
            showVel              = 5,            //display vel values
            showFlkMbrs          = 6,            //whether or not to show actual subflock members (i.e. neigbhors,colliders, preds, etc) when debugging
            attractMode          = 7,            // whether we are in mouse attractor mode or repel mode
            //must stay within first 32 positions(single int) to make flocking control flag int easier (just sent first int)
            //flocking control flags
            flkCenter            = 8,            // on/off : flock-centering
            flkVelMatch          = 9,            // on/off : flock velocity matching
            flkAvoidCol          = 10,            // on/off : flock collision avoidance    
            flkWander            = 11,            // on/off : flock wandering
            flkAvoidPred         = 12,            //turn on/off avoiding predators force and chasing prey force
            flkHunt              = 13,            //whether hunting is enabled
            flkHunger            = 14,            //can get hungry    
            flkSpawn             = 15,            //allow breeding
            flkClipToNeighbors   = 16,           // on/off clip all radii to be no greater than neighbor radii
            useOrigDistFuncs     = 17,
            useTorroid           = 18,    
            flkCyclesFrc         = 19,            //the force these boids exert cycles with motion
            //end must stay within first 32
            modDelT              = 20,            //whether to modify delT based on frame rate or keep it fixed (to fight lag)
            viewFromBoid         = 21,            //whether viewpoint is from a boid's perspective or global
            isMTCapableIDX       = 22;            //whether this machine supports multiple threads
    
    protected static final int numBasePrivFlags = 23;

    public final int MaxNumBoids = 15000;        //max # of boids per flock
    protected final int initNumBoids = 500;        //initial # of boids per flock
    
    /**
     * All flocks of boids
     */
    public BoidFlock[] flocks;
    
    /**
     * Array of each flock's UI variables/values
     */
    protected Boids_UIFlkVars[] flockVars;
    
    // Up to 6 different flocks will display nicely on side menu
    protected String[] flkNames = new String[]{"Privateers", "Pirates", "Corsairs", "Marauders", "Freebooters", "Picaroons"};
    protected float[] flkRadMults = {1.0f, 0.5f, 0.25f, 0.75f, 0.66f, 0.33f, 0.85f};
    
    ///////////
    //graphical constructs for boids 
    ///////////
    private static final int
        sphereClrIDX = 0,
        boatClrIDX = 1,
        jFishClrIDX = 2;
    private static final int numBoidTypes = 3;
    /**
     * Specular color
     */
    private static final int[][] specClr = new int[][]{
        {255,255,255,255},        //sphere
        {255,255,255,255},        //boat
        {255,255,255,255}};        //jellyfish
    /**
     * Divide fill color for each type by these values for stroke
     */
    private static final float[][] strokeScaleFact = new float[][]{
        {1.25f,0.42f,1.33f,0.95f,3.3f,2.7f},                //sphere    
        {1.25f,0.42f,1.33f,0.95f,3.3f,2.7f},                //boat      
        {1.25f,1.25f,1.25f,1.25f,1.25f,1.25f}};               //jellyfish 
        
    /**
     * scale all fill colors by this value for emissive value
     */
    private static final float[] emitScaleFact = new float[] {0.7f, 0.9f, 0.8f};
    /**
     * stroke weight for sphere, boat, jellyfish
     */
    private static final float[] strkWt = new float[] {1.0f, 1.0f,.1f};
    /**
     * shininess for sphere, boat, jellyfish
     */
    private static final float[] shn = new float[] {5.0f,5.0f,5.0f};
    
    /**
     * per type, per flock fill colors
     */
    private static final int[][][] objFillColors = new int[][][]{
        {{110, 65, 30, 255}, {30, 30, 30,255}, {130, 22, 10,255}, {22, 188, 110, 255}, {22, 10, 130,255}, {0,180,120,255}},        //sphere
        {{110, 65, 30, 255}, {20, 20, 20,255}, {130, 22, 10,255}, {22, 128, 50, 255}, {22, 10, 150,255}, {0,180,120,255}},         //boats
        {{180, 125, 100,255}, {90, 130, 80, 255}, {180, 82, 90,255}, {190, 175, 60,255}, {50, 90, 240,255}, {30,210,160,255}}    //jellyfish
    };

    /**
     * image sigils for sails
     */
    protected PImage[] flkSails;                
    /**
     * global badge sizes
     */
    protected final float bdgSizeX_base, bdgSizeY;
    /**
     * badge size in x per flock
     */
    protected float[] bdgSizeX;
    
    /**
     * Menu badge for each type that preserves aspect ratio of pirate flags
     */
    protected myPointf[][] mnBdgBox;
    /**
     * UV coordinates
     */
    protected static final myPointf[] mnUVBox = new myPointf[]{
            new myPointf(0,0,0),
            new myPointf(1,0,0),
            new myPointf(1,1,0),
            new myPointf(0,1,0)};
    
    /**
     * Types of boids
     */
    protected String[] boidTypeNames = new String[]{"Pirate Boats", "Jellyfish"};
    
    /**
     * # of animation frames per animation cycle for animating boids
     */    
    protected int[] numAnimFramesPerType = new int[] {90,120};
    
    /**
     * whether this boid exhibits cyclic motion/animation
     */
    protected boolean[] boidCyclesFrc = new boolean[]{false, true};
    
    protected final int maxNumFlocks = flkNames.length;            //max # of flocks we'll support
    //array of template objects to render
    //need individual array for each type of object, sphere (simplified) render object
    protected Base_RenderObj[] currRndrTmplPerFlockAra,//set depending on UI choice for complex rndr obj 
        boatRndrTmplPerFlockAra,
        jellyFishRndrTmplPerFlockAra,
        //add more render obj arrays here for new boid constructs
        sphrRndrTmplPerFlockAra;//simplified rndr obj (sphere)//always last
    
    protected ConcurrentSkipListMap<String, Base_RenderObj[]> cmplxRndrTmpls;
    
    /**
     * multiplier to modify timestep to make up for lag
     */
    protected float timeStepMult = 1.0f;
    
    //current/initial values
    protected double curTimeStep = .1;
    protected int numFlocks = 1;

    /**
     * idxs of flock and boid to assign camera to if we are watching from "on deck"
     */
    protected int flockToWatch, boidToWatch;
   
    /**
     * threading constructions - allow map manager to own its own threading executor
     */
    protected ExecutorService th_exec;    //to access multithreading - instance from calling program
    protected int numUsableThreads;        //# of threads usable by the application
    
    /**
     * Callers and futures for hunt functionality - each thread gets members from all flocks
     */
    protected List<Future<Boolean>> callHuntFutures;
    protected List<Callable<Boolean>> callHuntBoidCalcs;
    
    
    public Base_BoidsWindow(IRenderInterface _p, GUI_AppManager _AppMgr, int _winIdx) {
        super(_p, _AppMgr, _winIdx);
        bdgSizeX_base = AppMgr.getSwitchTextHeightOffset();
        bdgSizeY = bdgSizeX_base;
    }
    
    public ExecutorService getTh_Exec() {return th_exec;}
        
    /**
     * Initialize any UI control flags appropriate for all boids window applications
     */
    @Override
    protected final void initDispFlags() {
        //this window is runnable
        dispFlags.setIsRunnable(true);
        //this window uses a customizable camera
        dispFlags.setUseCustCam(true);        
        initDispFlags_Indiv();
    }
    /**
     * Initialize any UI control flags appropriate for specific instanced boids window
     */
    protected abstract void initDispFlags_Indiv();
    
    @Override
    protected void initMe() {
        //called once
        // initialize all rendered objects
        initBoidRndrObjs();
        // build all flockvars/ui interfaces 1 time.
        initFlockVars();
        
        //want # of usable background threads.  Leave 2 for primary process (and potential draw loop)
        numUsableThreads = getNumThreadsAvailable() - 2;
        //set if this is multi-threaded capable - need more than 1 outside of 2 primary threads (i.e. only perform multithreaded calculations if 4 or more threads are available on host)
        uiMgr.setPrivFlag(isMTCapableIDX, numUsableThreads>1);
        
        //th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
        if(uiMgr.getPrivFlag(isMTCapableIDX)) {
            //th_exec = Executors.newFixedThreadPool(numUsableThreads+1);//fixed is better in that it will not block on the draw - this seems really slow on the prospect mapping
            th_exec = Executors.newCachedThreadPool();// this is performing much better even though it is using all available threads
        } else {//setting this just so that it doesn't fail somewhere - won't actually be exec'ed
            th_exec = Executors.newCachedThreadPool();// Executors.newFixedThreadPool(numUsableThreads);
        }
            
        initFlocks();    
        //flkMenuOffset = uiClkCoords[1] + uiClkCoords[3] - y45Off;    //495
        //custMenuOffset = uiClkCoords[3] + AppMgr.getClkBoxDim();    
        initMe_IndivPost();
    }//initMe
    
    /**
     * Instance specific init after base class init is finished
     */
    protected abstract void initMe_IndivPost();
    

    @Override
    protected int[] getFlagIDXsToInitToTrue() {
        return getFlagIDXsToInitToTrue_Indiv(new int[] {drawBoids, attractMode, useTorroid});
    }
    
    /**
     * Add instance-specific private flags to init to true to those from base class
     * @param baseFlags base class flags to init to true - add to this array
     * @return array of base and instance class flags to init to true
     */
    protected abstract int[] getFlagIDXsToInitToTrue_Indiv(int[] baseFlags);
    
    /**
     * This function would provide an instance of the override class for base_UpdateFromUIData, which would
     * be used to communicate changes in UI settings directly to the value consumers.
     */
    @Override
    protected UIDataUpdater buildUIDataUpdateObject() {
        return new Boids_UIDataUpdater(this);
    }
    /**
     * This function is called on ui value update, to pass new ui values on to window-owned consumers
     */
    protected final void updateCalcObjUIVals() {}
    
    /**
     * initialize all instances of boat boid models/templates - called 1 time
     */
    protected void initBoidRndrObjs(){        
        flkSails = new PImage[maxNumFlocks];
        bdgSizeX = new float[maxNumFlocks];
        mnBdgBox = new myPointf[maxNumFlocks][];
        for(int i=0; i<maxNumFlocks; ++i){    
            flkSails[i] = ((ProcessingRenderer) ri).loadImage(flkNames[i]+".jpg");
            float scale = flkSails[i].width / (1.0f*flkSails[i].height);
            bdgSizeX[i] = bdgSizeX_base * scale; 

            mnBdgBox[i] = new myPointf[]{
                    new myPointf(),
                    new myPointf(0,bdgSizeY,0),
                    new myPointf(bdgSizeX[i],bdgSizeY,0),
                    new myPointf(bdgSizeX[i],0,0)
            };
        }
        
        cmplxRndrTmpls = new ConcurrentSkipListMap<String, Base_RenderObj[]> (); 
        boatRndrTmplPerFlockAra = new Boat_RenderObj[maxNumFlocks];
        jellyFishRndrTmplPerFlockAra = new JFish_RenderObj[maxNumFlocks];
        sphrRndrTmplPerFlockAra = new Sphere_RenderObj[maxNumFlocks];
        RenderObj_ClrPalette[] palettes = new RenderObj_ClrPalette[numBoidTypes];
        for (int i=0;i<numBoidTypes;++i) {palettes[i] = buildRenderObjPalette(i);}
        
        for(int i=0; i<maxNumFlocks; ++i){                
            sphrRndrTmplPerFlockAra[i] = new Sphere_RenderObj(ri, i, maxNumFlocks, palettes[sphereClrIDX]);    
            //build boat render object for each individual flock type
            boatRndrTmplPerFlockAra[i] = new Boat_RenderObj(ri, i, maxNumFlocks,numAnimFramesPerType[0], palettes[boatClrIDX], new PImage[] {flkSails[i]});
            //build "jellyfish" render object for each flock
            jellyFishRndrTmplPerFlockAra[i] = new JFish_RenderObj(ri, i, maxNumFlocks, numAnimFramesPerType[1], palettes[jFishClrIDX]);
        }

        cmplxRndrTmpls.put(boidTypeNames[0], boatRndrTmplPerFlockAra);
        cmplxRndrTmpls.put(boidTypeNames[1], jellyFishRndrTmplPerFlockAra);
        currRndrTmplPerFlockAra = cmplxRndrTmpls.get(boidTypeNames[0]);//start by rendering boats
    }
    
    /**
     * Returns the dimensions of the flag badge, used by flkVars UI to fit the text nicely
     * @param idx
     * @return
     */
    public final float[] getFlockBadgeDims(int idx) {      return new float[] {bdgSizeX[idx], bdgSizeY};  }
    
    /**
     * Initialize all individual flock variable UI groups. Called 1 time
     */
    protected void initFlockVars() {
        // Get dimensions of 3d box region
        float[] gridDims = AppMgr.get3dGridDims();
        float initPredRad = MyMathUtils.min(gridDims);
        flockVars = new Boids_UIFlkVars[maxNumFlocks];
        for(int i = 0; i<flockVars.length; ++i){         
            flockVars[i] = new Boids_UIFlkVars(this, i, flkNames[i], flkRadMults[i], initPredRad, objFillColors[1][i]);    
        }
        // Now build flock vars - each will use the UIclickara of the previous
        for(int i = 0; i<flockVars.length; ++i){  
            flockVars[i].initFlkVarsUI();
        }
    }//initFlockVars
    
    /**
     * Return the previous flock vars entry's click coordinates, to know where to initialize next 
     * flock vars coords. If idx is 0 return base window uiMgr click coords
     * @param idx index of current flock vars
     * @return previous flock vars' click coordinates
     */
    public final float[] getPrevFlkVarsUIClckCoords(int idx) {
        float[] flkVars = (idx == 0) ? uiMgr.getUIClkCoords() : flockVars[idx-1].getUIClkCoords();    
        //make old ending y coord new beginning y coord
        flkVars[1] = flkVars[3];
        return flkVars;
    }

    
    /**
     * Build render object color palette for passed type of flock
     * @param _type
     * @return
     */
    private final RenderObj_ClrPalette buildRenderObjPalette(int _type) {
        RenderObj_ClrPalette palette = new RenderObj_ClrPalette(ri, maxNumFlocks);
        //set main color
        palette.setColor(-1, objFillColors[_type][0], objFillColors[_type][0], objFillColors[_type][0], specClr[_type], new int[]{0,0,0,0}, strkWt[_type], shn[_type]);
        //scale stroke color from fill color
        palette.scaleMainStrokeColor(strokeScaleFact[_type][0]);
        //set alpha after scaling
        palette.setMainStrokeColorAlpha(objFillColors[_type][0][3]);
        //set per-flock colors
        for(int i=0; i<maxNumFlocks; ++i){    
            palette.setColor(i, objFillColors[_type][i], objFillColors[_type][i], objFillColors[_type][i], specClr[_type], new int[]{0,0,0,0}, strkWt[_type], shn[_type]);
            //scale stroke colors
            palette.scaleInstanceStrokeColor(i, strokeScaleFact[_type][i]);
            //set alpha after scaling
            palette.setInstanceStrokeColorAlpha(i, objFillColors[_type][i][3]);
        }
        //scale all emissive values - scaled from fill color
        palette.scaleAllEmissiveColors(emitScaleFact[_type]);
        //disable ambient
        palette.disableAmbient();
        return palette;
    }
    
    /**
     * turn on/off all flocking control boolean variables
     * @param val
     */
    public final void setFlocking(boolean val){
        uiMgr.setPrivFlag(flkCenter, val);
        uiMgr.setPrivFlag(flkVelMatch, val);
        uiMgr.setPrivFlag(flkAvoidCol, val);
        uiMgr.setPrivFlag(flkWander, val);
    }
    /**
     * turn on/off all hunting control boolean variables
     * @param val
     */
    public final void setHunting(boolean val){
        //should generally only be enabled if multiple flocks present
        //TODO set up to enable single flock to cannibalize
        uiMgr.setPrivFlag(flkAvoidPred, val);
        uiMgr.setPrivFlag(flkHunt, val);
        uiMgr.setPrivFlag(flkHunger, val);
        uiMgr.setPrivFlag(flkSpawn, val);        
    }//setHunting

    public final boolean getDoHunt() {return uiMgr.getPrivFlag(flkHunt);}
    public final boolean getDoSpawn() {return uiMgr.getPrivFlag(flkSpawn);}
    public final boolean getDoCheckHunger() {return uiMgr.getPrivFlag(flkHunger);}
    
    public final boolean getIsTorroidal() {return uiMgr.getPrivFlag(useTorroid);}
    
    /**
     * set up current flock configuration, based on ui selections
     */
    private void initFlocks(){
        // Always start with flocking controls enabled
        setFlocking(true);
        // Only enable hunting if more than 1 flock exist
        setHunting(numFlocks > 1);
        flockToWatch = 0;
        boidToWatch = 0;
        setMaxUIFlockToWatch();
        flocks = new BoidFlock[numFlocks];
        float[] UIFlkVarClkCoords = getUIClkCoords();
        // make new ui flk var clk coords start at end of old
        UIFlkVarClkCoords[1] = UIFlkVarClkCoords[3];
        for(int i =0; i<flocks.length; ++i){
            flocks[i] = new BoidFlock(this,flockVars[i], initNumBoids, numUsableThreads);
        }
        
        int predIDX, preyIDX;
        for(int i =0; i<flocks.length; ++i){
            predIDX = ((i+1)%flocks.length);
            preyIDX = (((i+flocks.length)-1)%flocks.length);
            flocks[i].setPredPreySphereTmpl(flocks[predIDX], flocks[preyIDX], currRndrTmplPerFlockAra[i], sphrRndrTmplPerFlockAra[i]);
        }    
        for(int i =0; i<flocks.length; ++i){            flocks[i].initFlock();        }
        
        callHuntBoidCalcs = new ArrayList<Callable<Boolean>>();
        callHuntFutures = new ArrayList<Future<Boolean>>();
        //Build current per-thread hunt callables
        buildBoidHuntCallables();

    }//initFlocks
    
    
    /**
     * This function will build the callables for the boid hunt. This will have boids across all flocks, so that threads
     * do not execute on a per-flock basis and a hunt bias persists.
     */
    @SuppressWarnings("unchecked")
    private void buildBoidHuntCallables() {
        if(uiMgr.getPrivFlag(flkHunt)) {
            callHuntBoidCalcs.clear();
            //Get each flock's boidThreadFrame, and merge them
            List<Boid>[][] boidThreadFramePerFlock = new List[flocks.length][];
            int maxNumFrames = -1;
            for(int i =0; i<flocks.length; ++i){
                boidThreadFramePerFlock[i] = flocks[i].getBoidThrdFrames();    
                maxNumFrames = MyMathUtils.max(boidThreadFramePerFlock[i].length,maxNumFrames);
            }
            //Just merge each flock's thread frame.
            List<Boid>[] boidThrdFrames = new List[maxNumFrames];
            for (int frame = 0; frame < maxNumFrames; ++frame) {
                boidThrdFrames[frame] = new ArrayList<Boid>();
                for(int i =0; i<flocks.length; ++i){
                    if(boidThreadFramePerFlock[i].length>frame) {
                        boidThrdFrames[frame].addAll(boidThreadFramePerFlock[i][frame]);
                    }                
                }
            }
            //Build callables
            for(List<Boid> subL : boidThrdFrames){callHuntBoidCalcs.add(new BoidHuntUpdater(subL));}
        }
    }//buildBoidHuntCallables()
    
    
    private void setCurrPerFlockRenderTemplate(){
        for(int i =0; i<flocks.length; ++i){
            flocks[i].setCurrTemplate(currRndrTmplPerFlockAra[i]);
        }
    }
    
    /**
     * Retrieve the first 32 flag bits from the privFlags structure, used to hold all the flocking menu flags
     * @return
     */
    public int getFlkFlagsInt(){        return uiMgr.getPrivFlagAsInt(0);} //get first 32 flag settings
    
    //set camera to be on a boid in one of the flocks
    public void setBoidCam(float rx, float ry, float dz){
        flocks[flockToWatch].boidFlock.get(boidToWatch).setBoatCam(ri, rx,ry,dz, winInitVals.sceneOriginVal);
    }
    
    /**
     * Handle application-specific flag setting
     */
    @Override
    public void handlePrivFlags_Indiv(int idx, boolean val, boolean oldVal){
        switch(idx){
            case showBoidFrame           : {break;}
            case drawBoids               : {break;}
            case drawScaledBoids         : {break;}        
            case clearPath               : {
                //TODO this needs to change how it works so that initialization doesn't call ProcessingRenderer before it is ready
                //ri.setClearBackgroundEveryStep( !val);//turn on or off background clearing in main window
                break;}
            case showVel                 : {break;}
            case attractMode             : {break;}
            case showFlkMbrs             : {break;}
            case flkCenter               : {break;}
            case flkVelMatch             : {break;}
            case flkAvoidCol             : {break;}
            case flkWander               : {break;}
            case flkAvoidPred            : {break;}
            case flkHunt                 : {break;}
            case flkHunger               : {break;}
            case flkSpawn                : {break;}
            case flkClipToNeighbors      : {
                if(flocks == null){break;}
                for(int i=0; i<flocks.length; ++i){flockVars[i].setClipToNeighborRad(val);}
                break;}
            case modDelT                 : {
                timeStepMult = val ?  60.0f/ ri.getFrameRate() : 1.0f;                
                break;}
            case flkCyclesFrc            : {break;}
            case viewFromBoid            : {
                dispFlags.setDrawMseEdge(!val);//if viewing from boid, then don't show mse edge, and vice versa
                break;}    //whether viewpoint is from a boid's perspective or global
            case useOrigDistFuncs         : {
                if(flocks == null){break;}
                for(int i=0; i<flocks.length; ++i){flockVars[i].setDefaultWtVals(val);}
                break;
            }
            case useTorroid                : { break;}        
            case isMTCapableIDX            : {break;}
            default : {
                if (!handlePrivBoidFlags_Indiv(idx, val, oldVal)){
                    msgObj.dispErrorMessage(className, "handlePrivFlags_Indiv", "Unknown/unhandled flag idx :"+idx+" attempting to be set to "+val+" from "+oldVal+". Aborting.");
                }
            }
        }        
    }//setPrivFlags
    
    /**
     * Instance-specific boolean flags to handle
     * @param idx
     * @param val
     * @param oldVal
     * @return
     */
    protected abstract boolean handlePrivBoidFlags_Indiv(int idx, boolean val, boolean oldVal);
    
    /**
     * Whether we're showing the flock members 
     * @return
     */
    public final boolean getShowFlkMbrs() {return uiMgr.getPrivFlag(showFlkMbrs);}
    
    /**
     * Whether we are clipping max radii to current neighborhood radius or pre-set max radius
     * @return
     */
    public final boolean getClipMaxRadToNeighbor() {return uiMgr.getPrivFlag(flkClipToNeighbors);}
    
    /**
     * Return the current flock vars for the flock specified by flockIDX
     * @param flockIDX
     * @return
     */    
    public Boids_UIFlkVars getFlkVars(int flockIDX) {return flockVars[flockIDX];}
    public String getFlkName(int flockIDX) {return flkNames[flockIDX];}
    
    public String[] getFlknNames() {return flkNames.clone();}
    
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
    protected final void setupGUIObjsAras(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap){        //keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects            
        //keyed by object idx (uiXXXIDX), entries are lists of values to use for list select ui objects
                    
        tmpUIObjMap.put("gIDX_TimeStep", uiMgr.uiObjInitAra_Float(gIDX_TimeStep, new double[]{0,1.0f,.0001f}, curTimeStep, "Time Step"));                                                                           
        tmpUIObjMap.put("gIDX_NumFlocks", uiMgr.uiObjInitAra_Int(gIDX_NumFlocks, new double[]{1,maxNumFlocks,1.0f}, 1.0, "# of Flocks"));                                                                           
        tmpUIObjMap.put("gIDX_BoidType", uiMgr.uiObjInitAra_List(gIDX_BoidType, 0.0, "Flock Species", boidTypeNames));                                                                           
        tmpUIObjMap.put("gIDX_FlockToObs", uiMgr.uiObjInitAra_List(gIDX_FlockToObs, 0.0, "Flock To Watch", flkNames));                                                                           
        tmpUIObjMap.put("gIDX_ModNumBoids", uiMgr.uiObjInitAra_Int(gIDX_ModNumBoids, new double[]{-50,50,1.0f}, 0.0, "Modify Flock Pop"));                                                                           
        tmpUIObjMap.put("gIDX_BoidToObs", uiMgr.uiObjInitAra_Int(gIDX_BoidToObs, new double[]{0,initNumBoids-1,1.0f}, 0.0, "Boid To Board"));               
    
        setupGUIObjsAras_Indiv(tmpUIObjMap);
    }//setupGUIObjsAras
    
    /**
     * Build UI button objects to be shown in left side bar menu for this window.  This is the first child class function called by initThisWin
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    @Override
    protected final void setupGUIBoolSwitchAras(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap) {        
        //add an entry for each button, in the order they are wished to be displayed
        int idx=firstIdx;
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.buildDebugButton(idx++,"Debugging", "Enable Debug"));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ShowBoidFrame","Showing Frame", "Show Frame", showBoidFrame));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "DrawBoids","Drawing Boids", "Drawing Spheres", drawBoids));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "DrawScaledBoids","Scale Boids' Sizes", "Boids Same Size", drawScaledBoids));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ClearPath","Showing Boid Path", "Hiding Boid Path", clearPath));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ShowVel","Showing Vel Vectors", "Hiding Vel Vectors", showVel));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ShowFlockMembers","DBG : List Flk Mmbrs", "DBG : Hide Flk Mmbrs", showFlkMbrs));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "AttractMode","Mouse Click Attracts", "Mouse Click Repels", attractMode));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkCenter","Ctr Force ON", "Ctr Force OFF", flkCenter));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkVelMatch","Vel Match ON", "Vel Match OFF", flkVelMatch));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkAvoidCol","Col Avoid ON", "Col Avoid OFF", flkAvoidCol));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkWander","Wander ON", "Wander OFF", flkWander));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkAvoidPred","Pred Avoid ON", "Pred Avoid OFF", flkAvoidPred));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkHunt","Hunting ON", "Hunting OFF", flkHunt));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkHunger","Hunger ON", "Hunger OFF", flkHunger));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkSpawn","Spawning ON", "Spawning OFF", flkSpawn));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "FlkClipRad","Max Rad is Neighbor Rad", "Max Rad is Global Rad",flkClipToNeighbors));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "UseOrigDistFuncs","Orig Funcs ON", "Orig Funcs OFF", useOrigDistFuncs));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "UseTorroid","Tor Bnds ON", "Tor Bnds OFF", useTorroid));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ModDelT","Mod DelT By FRate", "Fixed DelT", modDelT));
        tmpUIBoolSwitchObjMap.put("Button_"+idx, uiMgr.uiObjInitAra_Switch(idx++, "ViewFromBoid","Boid-eye View", "Global View", viewFromBoid));

        //populate instancing application objects, including instancing-class specific buttons
        setupGUIBoolSwitchAras_Indiv(idx, tmpUIBoolSwitchObjMap);        
    }//setupGUIBoolSwitchAras

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
    protected abstract void setupGUIObjsAras_Indiv(LinkedHashMap<String, GUIObj_Params> tmpUIObjMap);

    /**
     * Build all UI buttons to be shown in left side bar menu for this window. This is for instancing windows to add to button region
     * @param firstIdx : the first index to use in the map/as the objIdx
     * @param tmpUIBoolSwitchObjMap : map of GUIObj_Params to be built containing all flag-backed boolean switch definitions, keyed by sequential value == objId
     *                 the first element is the object index
     *                 the second element is true label
     *                 the third element is false label
     *                 the final element is integer flag idx 
     */
    protected abstract void setupGUIBoolSwitchAras_Indiv(int firstIdx, LinkedHashMap<String, GUIObj_Params> tmpUIBoolSwitchObjMap);

    
    //when flockToWatch changes, reset maxBoidToWatch value ((Base_NumericGUIObj)guiObjs_Numeric[gIDX_BoidToObs])
    private void setMaxUIBoidToWatch(int flkIdx){uiMgr.setNewUIMaxVal(gIDX_BoidToObs,flocks[flkIdx].boidFlock.size()-1);uiMgr.setUIWinVals(gIDX_BoidToObs);}    
    private void setMaxUIFlockToWatch(){uiMgr.setNewUIMaxVal(gIDX_FlockToObs, numFlocks - 1);    uiMgr.setUIWinVals(gIDX_FlockToObs);}        
    
    /**
     * Called if int-handling guiObjs_Numeric[UIidx] (int or list) has new data which updated UI adapter. 
     * Intended to support custom per-object handling by owning window.
     * Only called if data changed!
     * @param UIidx Index of gui obj with new data
     * @param ival integer value of new data
     * @param oldVal integer value of old data in UIUpdater
     */
    @Override
    protected final void setUI_IntValsCustom(int UIidx, int ival, int oldVal) {
        switch(UIidx){    
            case gIDX_NumFlocks        :{
                numFlocks = ival; 
                initFlocks(); 
                break;}
            case gIDX_BoidType        :{
                currRndrTmplPerFlockAra = cmplxRndrTmpls.get(boidTypeNames[ival]);
                uiMgr.setPrivFlag(flkCyclesFrc, boidCyclesFrc[ival]);//set whether this flock cycles animation/force output
                setCurrPerFlockRenderTemplate(); 
                break;}
            case gIDX_FlockToObs     :{
                flockToWatch = ival; 
                setMaxUIBoidToWatch(flockToWatch);
                break;}
            case gIDX_ModNumBoids      :{    
                flocks[flockToWatch].modNumBoids(ival);
                break;}
            case gIDX_BoidToObs     :{
                boidToWatch = ival;
                break;}        
            default : {
                if (!setUI_IntValsCustom_Indiv(UIidx, ival, oldVal)) {
                    msgObj.dispWarningMessage(className, "setUI_IntValsCustom", "No int-defined gui object mapped to idx :"+UIidx);
                }
                break;}                
        }
    }//setUI_IntValsCustom
    
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
    protected abstract boolean setUI_IntValsCustom_Indiv(int UIidx, int ival, int oldVal);
    

    /**
     * Called if float-handling guiObjs_Numeric[UIidx] has new data which updated UI adapter.  
     * Intended to support custom per-object handling by owning window.
     * Only called if data changed!
     * @param UIidx Index of gui obj with new data
     * @param val float value of new data
     * @param oldVal float value of old data in UIUpdater
     */
    @Override
    protected final void setUI_FloatValsCustom(int UIidx, float val, float oldVal) {
        switch(UIidx){        
        case gIDX_TimeStep             :{curTimeStep = val;break;}
        default : {
            if (!setUI_FloatValsCustom_Indiv(UIidx, val, oldVal)) {
                msgObj.dispWarningMessage(className, "setUI_FloatValsCustom", "No float-defined gui object mapped to idx :"+UIidx);
            }
            break;}
        }                
    }//setUI_FloatValsCustom
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
    protected abstract boolean setUI_FloatValsCustom_Indiv(int UIidx, float val, float oldVal);
    
    /**
     * 
     * @return
     */
    public double getTimeStep(){        return curTimeStep * timeStepMult;    }
    /**
     * Once boid count has been modified, reset mod UI obj
     */
    public void clearModNumBoids() {    uiMgr.resetUIObj(gIDX_ModNumBoids);}
    
    @Override
    public void initDrwnTraj_Indiv(){}    
    //set camera to either be global or from pov of one of the boids
    @Override
    protected void setCamera_Indiv(float[] camVals){
        if (uiMgr.getPrivFlag(viewFromBoid)){    setBoidCam(getCamRotX(),getCamRotY(),getCamDist());        }
        else {                        setCameraBase(camVals);    }
    }
    
    protected abstract void initTransform();
    
    private void drawMenuBadge(myPointf[] uvAra, int type) {
        ri.pushMatState(); 
            flockVars[type].moveToUIRegion();
            ri.gl_beginShape(); 
            ((ProcessingRenderer)ri).texture(flkSails[type]);
            //for(int i=0;i<mnBdgBox[type].length;++i){    ((ProcessingRenderer)ri).vTextured(mnBdgBox[type][i], uvAra[i].y, uvAra[i].x);} 
            for(int i=0;i<mnBdgBox[type].length;++i){    ((ProcessingRenderer)ri).vTextured(mnBdgBox[type][i], uvAra[i].y, uvAra[i].x);} 
            ri.gl_endShape(true);
        ri.popMatState();
    }//       
    @Override
    public void drawCustMenuObjs(float animTimeMod){
        ri.pushMatState();    
        for(int i =0; i<flocks.length; ++i){
            currRndrTmplPerFlockAra[i].setMenuColor();
            drawMenuBadge(mnUVBox,i);
            flockVars[i].setFlockSize(flocks[i].boidFlock.size());
            flockVars[i].drawMe(animTimeMod);
        }
        ri.popMatState();
    }
    
    @Override
    protected void drawMe(float animTimeMod) {
        ri.pushMatState();
        initTransform();
        
        if (uiMgr.getPrivFlag(showBoidFrame)){            for(int i =0; i<flocks.length; ++i){flocks[i].drawBoidFrames(ri);}}
        if (uiMgr.getPrivFlag(showVel)){                  for(int i =0; i<flocks.length; ++i){flocks[i].drawBoidVels(ri);}}

        if(uiMgr.getPrivFlag(drawBoids)){//broken apart to minimize if checks - only potentially 2 per flock per frame instead of thousands
            if (uiMgr.getPrivFlag(drawScaledBoids)) {    for(int i =0; i<flocks.length; ++i){flocks[i].drawBoidsScaled(ri);}}                
            else {                                       for(int i =0; i<flocks.length; ++i){flocks[i].drawBoids(ri);}}
        } else {
            for(int i =0; i<flocks.length; ++i){flocks[i].drawBoidsAsBall(ri);}
            if(uiMgr.getPrivFlag(showFlkMbrs)){          for(int i =0; i<flocks.length; ++i){flocks[i].drawBoidsFlkMmbrs(ri);}}
        }    
        ri.popMatState();
    }//drawMe
    

    @Override
    protected void drawRightSideInfoBarPriv(float modAmtMillis) {}

    @Override
    protected void drawOnScreenStuffPriv(float modAmtMillis) {}
    
    @Override
    protected boolean simMe(float modAmtSec) {//run simulation
        //clear out all boid maps from last cycle
        for(int i =0; i<flocks.length; ++i){flocks[i].clearOutBoids();} 
        //initialize maps of each boid's nighborhoods, predators and prey
        for(int i =0; i<flocks.length; ++i){flocks[i].initAllMaps();}
        //TODO verify not clicking in menu
        boolean checkForce = (AppMgr.mouseIsClicked()) && (!AppMgr.shiftIsPressed());
        //Derive forces
        if(uiMgr.getPrivFlag(useOrigDistFuncs)){for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsOrigMultTH(checkForce);}} 
        else {                    for(int i =0; i<flocks.length; ++i){flocks[i].moveBoidsLinMultTH(checkForce);}}
        //Apply forces and derive velocity and location updates
        for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidState(BoidUpdate_Type.Move);}
        //Solve for spawning    
        if(uiMgr.getPrivFlag(flkSpawn)) {for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidState(BoidUpdate_Type.Spawn);}}
        //solve for hunt
        //TODO this needs to be organized across all flocks so that one flock does not get first dibs on hunting
        if(uiMgr.getPrivFlag(flkHunt)) {
            //invoke callables        
            try {callHuntFutures = th_exec.invokeAll(callHuntBoidCalcs);for(Future<Boolean> f: callHuntFutures) {f.get();}} catch (Exception e) {            e.printStackTrace();}            
        }
        //solve for hunger
        if(uiMgr.getPrivFlag(flkHunger)) {for(int i =0; i<flocks.length; ++i){flocks[i].updateBoidState(BoidUpdate_Type.Hunger);}}
        
        //Finish up - remove dead boids, add newborns
        for(int i =0; i<flocks.length; ++i){flocks[i].finalizeBoids();setMaxUIBoidToWatch(i);}
        //Build hunt callables, with boids from every flock in every thread
        buildBoidHuntCallables();
        return false;
    }
    @Override
    protected void stopMe() {    }        

    
    ///////////////////////////////////////////////////////
    /// Start mouse interaction

    @Override
    protected boolean hndlMouseClick_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
        //not in ui buttons, check if in flk vars region
        if((mouseX < uiMgr.getUIClkCoords()[2]) && (mouseY >= uiMgr.getUIClkCoords()[3])){
            for(int i = 0; i<flockVars.length; ++i){if(flockVars[i].handleMouseClick(mouseX, mouseY, mseBtn)) {
                setMsClickInUIObj(true);
                return true;}}
        }            
        return false;
    }//hndlMouseClickIndiv

    @Override
    protected boolean handleMouseWheel_Indiv(int ticks, float mult) {
        for(int i = 0; i<flockVars.length; ++i){if(flockVars[i].handleMouseWheel(ticks, mult)) {return true;}}
        return false;
    }

    @Override
    protected boolean hndlMouseMove_Indiv(int mouseX, int mouseY, myPoint mseClckInWorld){
        for(int i = 0; i<flockVars.length; ++i){if(flockVars[i].handleMouseMove(mouseX, mouseY)) {return true;}}
        return false;
    }
    
    @Override
    protected boolean hndlMouseDrag_Indiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
        for(int i = 0; i<flockVars.length; ++i){
            if(flockVars[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY, mseDragInWorld, mseBtn)) {return true;}
        }
        return false;
    }
    
    // pass release to all active flock vars 
    @Override
    protected void hndlMouseRel_Indiv() {
        for(int i = 0; i<flockVars.length; ++i){flockVars[i].handleMouseRelease();}
    }

    ///////////////////////////////////////////////////////
    /// End mouse interaction    
    
    @Override
    protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}    
    @Override
    protected void endShiftKey_Indiv() {}
    @Override
    protected void endAltKey_Indiv() {}
    @Override
    protected void endCntlKey_Indiv() {}
    @Override
    protected void addSScrToWin_Indiv(int newWinKey){}
    @Override
    protected void addTrajToScr_Indiv(int subScrKey, String newTrajKey){}
    @Override
    protected void delSScrToWin_Indiv(int idx) {}    
    @Override
    protected void delTrajToScr_Indiv(int subScrKey, String newTrajKey) {}
    //resize drawn all trajectories
    @Override
    protected void resizeMe(float scale) {}
    @Override
    protected void closeMe() {}
    @Override
    protected void showMe() {}

    /**
     * type is row of buttons (1st idx in curCustBtn array) 2nd idx is btn
     * @param funcRow idx for button row
     * @param btn idx for button within row (column)
     * @param label label for this button (for display purposes)
     */
    @Override
    protected final void launchMenuBtnHndlr(int funcRow, int btn, String label){
        switch(funcRow) {
        case 0 : {
            msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 1 | Btn : " + btn, MsgCodes.info4);
            switch(btn){
                case 0 : {                        
                    resetButtonState();
                    break;}
                case 1 : {    
                    resetButtonState();
                    break;}
                case 2 : {    
                    resetButtonState();
                    break;}
                default : {
                    break;}
            }    
            break;}//row 1 of menu side bar buttons
        case 1 : {
            msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 2 | Btn : " + btn, MsgCodes.info4);
            switch(btn){
                case 0 : {    
                    resetButtonState();
                    break;}
                case 1 : {    
                    resetButtonState();
                    break;}
                case 2 : {    
                    resetButtonState();
                    break;}
                case 3 : {    
                    //test cosine function
                    resetButtonState();
                    break;}
                default : {
                    break;}    
            }
            break;}//row 2 of menu side bar buttons
        case 2 : {
            msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 3 | Btn : " + btn, MsgCodes.info4);
            switch(btn){
                case 0 : {    
                    resetButtonState();
                    break;}
                case 1 : {    
                    resetButtonState();
                    break;}
                case 2 : {    
                    resetButtonState();
                    break;}
                case 3 : {    
                    //test cosine function
                    resetButtonState();
                    break;}
                default : {
                    break;}    
            }
            break;}//row 2 of menu side bar buttons
        case 3 : {
            msgObj.dispMessage(className,"launchMenuBtnHndlr","Clicked Btn row : Aux Func 4 | Btn : " + btn, MsgCodes.info4);
            switch(btn){
                case 0 : {    
                    resetButtonState();
                    break;}
                case 1 : {    
                    resetButtonState();
                    break;}
                case 2 : {    
                    resetButtonState();
                    break;}
                case 3 : {    
                    //test cosine function
                    resetButtonState();
                    break;}
                default : {
                    break;}    
            }
            break;}//row 2 of menu side bar buttons
        default : {
            msgObj.dispWarningMessage(className,"launchMenuBtnHndlr","Clicked Unknown Btn row : " + funcRow +" | Btn : " + btn);
            break;
        }
        }            
    }
    
    @Override
    public final void handleSideMenuMseOvrDispSel(int btn, boolean val) {}
    
    @Override
    protected final void handleSideMenuDebugSelEnable(int btn) {
        switch (btn) {
            case 0: {                break;            }
            case 1: {                break;            }
            case 2: {                break;            }
            case 3: {                break;            }
            case 4: {                break;            }
            case 5: {                break;            }
            default: {
                msgObj.dispMessage(className, "handleSideMenuDebugSelEnable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
                break;
            }
        }
    }
    
    @Override
    protected final void handleSideMenuDebugSelDisable(int btn) {
        switch (btn) {
            case 0: {                break;            }
            case 1: {                break;            }
            case 2: {                break;            }
            case 3: {                break;            }
            case 4: {                break;            }
            case 5: {                break;            }
        default: {
            msgObj.dispMessage(className, "handleSideMenuDebugSelDisable", "Unknown Debug btn : " + btn,MsgCodes.warning2);
            break;
            }
        }
    }


    @Override
    protected String[] getSaveFileDirNamesPriv() {
        return null;
    }

    @Override
    protected myPoint getMsePtAs3DPt(myPoint mseLoc){return new myPoint(mseLoc.x,mseLoc.y,mseLoc.z);}

    @Override
    protected void setVisScreenDimsPriv() {}

    @Override
    protected void setCustMenuBtnLabels() {}

    @Override
    public void hndlFileLoad(File file, String[] vals, int[] stIdx) {}

    @Override
    public ArrayList<String> hndlFileSave(File file) {        
        return null;
    }

    @Override
    public void processTraj_Indiv(DrawnSimpleTraj drawnTraj) {    }
}//class Base_BoidsWindow