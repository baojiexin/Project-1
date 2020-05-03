package automail;

import java.util.HashMap;

public class Building {
	
	
    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;

    private static HashMap<Integer,Boolean> lockingStatus = new HashMap<>();

    public static void initialFloorStatus(int floors){
        for(int i = 1; i <= floors; i++){
            lockingStatus.put(i, false);
        }
    }
    public static boolean isFloorLocked(int floor){
        return lockingStatus.get(floor);
    }
    public static void lockFloor(int floor){
        lockingStatus.put(floor, true);
    }
    public static void releaseFloor(int floor){
        lockingStatus.put(floor, false);
    }

}
