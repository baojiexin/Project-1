package strategies;
/** New: This class is used to handle different mode on robot.
 * Currently only handle the Caution mode*/
public class RobotMode {
    private static boolean caution;

    public static void changeCaution(boolean status){
        caution = status;
    }
    public static boolean isCautionOn(){
        if(caution == true){
            return true;
        }
        else return false;
    }
}
