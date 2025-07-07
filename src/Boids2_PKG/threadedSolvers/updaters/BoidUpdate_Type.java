package Boids2_PKG.threadedSolvers.updaters;

import java.util.HashMap;
import java.util.Map;
/**
 * enum used to specify supported types of UI objects
 * @author john turner 
 */
public enum BoidUpdate_Type {
    Move, 
    Spawn, 
    Hunger;
    private static final String[] _typeExplanation = new String[]{
            "Integrate forces and move boid",
            "Reproduce if able",
            "Update Hunger state"};
    private static final String[] _typeName = new String[]{"Move Boid","Reproduce","Update Hunger"};
    public static String[] getListOfTypes() {return _typeName;}
    private static Map<Integer, BoidUpdate_Type> map = new HashMap<Integer, BoidUpdate_Type>(); 
    static { for (BoidUpdate_Type enumV : BoidUpdate_Type.values()) { map.put(enumV.ordinal(), enumV);}}
    public int getOrdinal() {return ordinal();}
    public static BoidUpdate_Type getEnumByIndex(int idx){return map.get(idx);}
    public static BoidUpdate_Type getEnumFromValue(int idx){return map.get(idx);}
    public static int getNumVals(){return map.size();}                        //get # of values in enum
    public String getName() {return _typeName[ordinal()];}
    @Override
    public String toString() { return ""+this.name()+":"+_typeExplanation[ordinal()]; }    
    public String toStrBrf() { return ""+_typeExplanation[ordinal()]; }    
}
