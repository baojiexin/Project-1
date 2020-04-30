package strategies;

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
