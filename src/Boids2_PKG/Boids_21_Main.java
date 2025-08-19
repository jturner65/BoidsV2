package Boids2_PKG;

import java.util.HashMap;

import Boids2_PKG.ui.Boids_2DWin;
import Boids2_PKG.ui.Boids_3DWin;
import base_UI_Objects.GUI_AppManager;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * Flocking boids sim version 2.1
 * @author john turner
 */
public class Boids_21_Main extends GUI_AppManager {
    //project-specific variables
    private final String prjNmLong = "Boids Version 2.0";
    private final String prjNmShrt = "Boids2";
    private final String projDesc = "Multiple boid flock predator/prey simulation";
    /**
     * use sphere background for this program
     */
    private boolean useSphereBKGnd = true;
    /**
     * File name for background sphere/skybox
     */
    private final String bkSkyBox = "bkgrndTex.jpg";
    /**
     * Background color if no skybox is used
     */
    private final int[] bkgGndClr = new int[]{244,244,255,255};
    
    /**
     * size of 3d grid cube side
     */
    private final int GridDim_3D = 1500;
        
    /**
     * idx's in dispWinFrames for each window - 0 is always left side menu window
     * Side menu is dispMenuIDX == 0
     */
    private static final int 
            dispWindow1IDX = 1,
            dispWindow2IDX = 2;    
    /**
     * # of visible windows including side menu (always at least 1 for side menu)
     */
    private static final int numVisWins = 3;
    
    
    //////////////////////////////////////////////// code
    public static void main(String[] passedArgs) {
        Boids_21_Main me = new Boids_21_Main();
        Boids_21_Main.invokeProcessingMain(me, passedArgs);
     }
    
    protected Boids_21_Main(){super();}
    
    /**
     * Whether or not we should show the machine data on launch
     * @return
     */
    @Override
    protected boolean showMachineData() {return true;}
    
    /**
     * Set various relevant runtime arguments in argsMap
     * @param _passedArgs command-line arguments
     */
    @Override
    protected HashMap<String,Object> setRuntimeArgsVals(HashMap<String, Object> _passedArgsMap) {
        return  _passedArgsMap;
    }
    /**
     * Called in pre-draw initial setup, before first init
     * potentially override setup variables on per-project basis.
     * Do not use for setting background color or Skybox anymore.
     *      (Current settings in ProcessingRenderer)     
     *      strokeCap(PROJECT);
     *      textSize(txtSz);
     *      textureMode(NORMAL);            
     *      rectMode(CORNER);    
     *      sphereDetail(4);     * 
     */
    @Override
    protected void setupAppDims_Indiv() {
        //modify default grid dims to be 1500x1500x1500
        setDesired3DGridDims(GridDim_3D);        
    }
   
    /**
     * whether or not we want to restrict window size on widescreen monitors
     * 
     * @return 0 - use monitor size regardless
     *             1 - use smaller dim to be determine window 
     *             2+ - TBD
     */
    @Override
    protected int setAppWindowDimRestrictions() {    return 1;}    
    
    @Override
    protected boolean hideAppFlag_DebugMode() {             return false;}
    @Override
    protected boolean hideAppFlag_SaveAnim() {              return false;}
    @Override
    protected boolean hideAppFlag_RunSim() {                return false;}
    @Override
    protected boolean hideAppFlag_SingleStep() {            return false;}
    @Override
    protected boolean hideAppFlag_showRtSideInfoDisp() {    return false;}
    @Override
    protected boolean hideAppFlag_showStatusBar() {         return false;}
    @Override
    protected boolean hideAppFlag_showCanvas() {            return true;}
    
    /**
     * this is called to build all the Base_DispWindows in the instancing class
     */
    @Override
    protected void initAllDispWindows() {
        String[] _winTitles = new String[]{"","Boids ver2.0 3D","Boids ver2.0 2D"},
                _winDescr = new String[]{"", "Multi Flock Predator/Prey Boids 3D Simulation","Multi Flock Predator/Prey Boids 2D Simulation"};
        //instanced window dims when open and closed - only showing 1 open at a time - and init cam vals
        float[][] _floatDims  = getDefaultWinAndCameraDims();    

        //Builds sidebar menu button config
        //application-wide menu button bar titles and button names
        String[] menuBtnTitles = new String[]{"Functions 1","Functions 2","Functions 3"};
        String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar menu, or ignored
            {"Func 00", "Func 01", "Func 02"},                //row 1
            {"Func 10", "Func 11", "Func 12", "Func 13"},    //row 2
            {"Func 10", "Func 11", "Func 12", "Func 13"}    //row 3
            };
        String [] dbgBtns = {"Debug 0", "Debug 1", "Debug 2", "Debug 3","Debug 4"};
        buildSideBarMenu(_winTitles, menuBtnTitles, menuBtnNames, dbgBtns, true, false);
        
        //define windows
        /**
         *  _winIdx The index in the various window-descriptor arrays for the dispWindow being set
         *  _title string title of this window
         *  _descr string description of this window
         *  _dispFlags Essential flags describing the nature of the dispWindow for idxs : 
         *         0 : dispWinIs3d, 
         *         1 : canDrawInWin; 
         *         2 : canShow3dbox (only supported for 3D); 
         *         3 : canMoveView
         *  _floatVals an array holding float arrays for 
         *                 rectDimOpen(idx 0),
         *                 rectDimClosed(idx 1),
         *                 initCameraVals(idx 2)
         *  _intClrVals and array holding int arrays for
         *                 winFillClr (idx 0),
         *                 winStrkClr (idx 1),
         *                 winTrajFillClr(idx 2),
         *                 winTrajStrkClr(idx 3),
         *                 rtSideFillClr(idx 4),
         *                 rtSideStrkClr(idx 5)
         *  _sceneCenterVal center of scene, for drawing objects (optional)
         *  _initSceneFocusVal initial focus target for camera (optional)
         */
        int wIdx = dispWindow1IDX;
        //setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{255,255,255,255},new int[]{0,0,0,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
        setInitDispWinVals(wIdx, _winTitles[wIdx], _winDescr[wIdx], getDfltBoolAra(true), _floatDims,        
                new int[][] {new int[]{255,255,255,255},new int[]{255,255,255,255},
                    new int[]{180,180,180,255},new int[]{100,100,100,255},
                    new int[]{0,0,0,200},new int[]{255,255,255,255}});
                
        setDispWindow(wIdx, new Boids_3DWin(ri, this, wIdx));
        
        wIdx = dispWindow2IDX;
        //setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,true,false}, new int[]{50,40,20,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255});
        setInitDispWinVals(wIdx, _winTitles[wIdx], _winDescr[wIdx], getDfltBoolAra(false), _floatDims,
                new int[][] {new int[]{50,40,20,255}, new int[]{255,255,255,255},
                    new int[]{180,180,180,255}, new int[]{100,100,100,255},
                    new int[]{0,0,0,200},new int[]{255,255,255,255}});
                
        setDispWindow(wIdx, new Boids_2DWin(ri, this, wIdx));

        //specify windows that cannot be shown simultaneously here
        initXORWins(new int[]{dispWindow1IDX, dispWindow2IDX}, new int[]{dispWindow1IDX, dispWindow2IDX});    
    }//initAllDispWindows
    
    /**
     * Map indexed by window ID, holding an array of the titles (idx 0) and descriptions (idx 1) for every sub window
     * return null if none exist, and only put an entry in the map if one exists for that window
     * @return
     */
    @Override
    protected final HashMap<Integer, String[]> getSubWindowTitles(){ return null;}
    
    /**
     * Application-specific 1-time init. Specify which window to start on, whether to show status bar, etc.
     */
    @Override
    protected void initOnce_Indiv() {
        setWinVisFlag(dispWindow1IDX, true);
    }

    @Override
    protected void initProgram_Indiv() {    }

    /**
     * return a list of labels to apply to mse-over display select buttons - an empty or null list will not display option
     * @return
     */
    @Override
    public String[] getMouseOverSelBtnLabels() {return new String[0];    }
    
    @Override
    protected void handleKeyPress(char key, int keyCode) {
        switch (key){
            case '1' : {break;}
            case '2' : {break;}
            case '3' : {break;}
            case '4' : {break;}
            case '5' : {break;}                            
            case '6' : {break;}
            case '7' : {break;}
            case '8' : {break;}
            case '9' : {break;}
            case '0' : { break;}                            
            case ' ' : {toggleSimIsRunning(); break;}                            //run sim
            case 'f' : {getCurFocusDispWindow().setInitCamView();break;}//reset camera
            case 'a' :
            case 'A' : {toggleSaveAnim();break;}                        //start/stop saving every frame for making into animation
            case 's' :
            case 'S' : {break;}
//            case ';' :
//            case ':' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs_Numeric[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(-1); break;}//decrease the number of cycles between each draw, to some lower bound
//            case '\'' :
//            case '"' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs_Numeric[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(1); break;}//increase the number of cycles between each draw to some upper bound        
            default : {    }
        }//switch    
    }
    @Override
    public boolean isClickModUIVal() {
        //TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or convenient
        return altIsPressed() || shiftIsPressed();        
    }

    //////////////////////////////////////////
    /// graphics and base functionality utilities and variables
    //////////////////////////////////////////

    /**
     * Individual extending Application Manager post-drawMe functions
     * @param modAmtMillis
     * @param is3DDraw
     */
    @Override
    protected void drawMePost_Indiv(float modAmtMillis, boolean is3DDraw, boolean isGlblAppDebug) {}
    
    
    
    @Override
    public float[] getUIRectVals_Indiv(int idx, float[] menuRectVals) {
        switch(idx){
            case dispWindow1IDX         : {return menuRectVals;}
            case dispWindow2IDX         : {return menuRectVals;}
            default :  return menuRectVals;
        }
    }
    
    /**
     * Address flag-setting here for switching windows, so that if any special cases need to be addressed they can be.
     * only process visibility-related state changes here (should only be the switch statement
     * @param idx
     * @param val
     */
    @Override
    protected void setVisFlag_Indiv(int idx, boolean val ){
        switch (idx){
            case dispWindow1IDX        : {setWinFlagsXOR(dispWindow1IDX, val); break;}
            case dispWindow2IDX        : {setWinFlagsXOR(dispWindow2IDX, val); break;}
            default : {break;}
        }
    }//setFlags
    
    /**
     * return the number of visible window flags for this application
     * @return
     */
    @Override
    public int getNumVisFlags() {return numVisWins;}    
    
    /**
     * Set minimum level of message object console messages to display for this application. If null then all messages displayed
     * @return
     */
    @Override
    protected final MsgCodes getMinConsoleMsgCodes() {return null;}
    /**
     * Set minimum level of message object log messages to save to log for this application. If null then all messages saved to log.
     * @return
     */
    @Override
    protected final MsgCodes getMinLogMsgCodes() {return null;}
    @Override
    public String getPrjNmShrt() {return prjNmShrt;}
    @Override
    public String getPrjNmLong() {return prjNmLong;}
    @Override
    public String getPrjDescr() {return projDesc;}
    @Override
    protected boolean getUseSkyboxBKGnd(int winIdx) {    return useSphereBKGnd;}
    @Override
    protected String getSkyboxFilename(int winIdx) {    return bkSkyBox;}
    @Override
    protected int[] getBackgroundColor(int winIdx) {return bkgGndClr;}
    @Override
    protected int getNumDispWindows() {    return numVisWins;    }
    @Override
    public void setSmoothing() {        ri.setSmoothing(0);        }
}//class Boids_21_Main extends GUI_AppManager
