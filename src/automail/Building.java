package automail;

import java.util.HashMap;

public class Building {
	
	
    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;

    /** New: A map used to store status of floors(true = locked, false = unlocked)*/
    private static HashMap<Integer,Boolean> lockingStatus = new HashMap<>();

    public static void initialFloorStatus(int floors){
        for(int i = 1; i <= floors; i++){
            lockingStatus.put(i, false);
        }
    }
    /** New: Used to check whether a floor is locked*/
    public static boolean isFloorLocked(int floor){
        return lockingStatus.get(floor);
    }
    /** New: Used to lock a floor */
    public static void lockFloor(int floor){
        lockingStatus.put(floor, true);
    }
    /** New: Used to release a floor*/
    public static void releaseFloor(int floor){
        lockingStatus.put(floor, false);
    }

}
