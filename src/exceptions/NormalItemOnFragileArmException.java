package exceptions;

public class NormalItemOnFragileArmException extends Exception{
    public NormalItemOnFragileArmException(){
        super("Normal Item is carried on the Fragile Arm!");
    }
}
