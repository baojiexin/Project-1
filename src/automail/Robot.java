package automail;

import exceptions.BreakingFragileItemException;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import exceptions.NormalItemOnFragileArmException;
import strategies.IMailPool;
import strategies.RobotMode;

import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot implements IMailHandling{
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING, WRAPPING, UNWRAPPING }
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;

    /** New: MailItems on robot*/
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    private MailItem fragileItem = null;
    private MailItem normalItem = null;

    private int deliveryCounter;

    /** New: Time for wrapping and unwrapping*/
    private int wrapTime = 0;
    private int unwrapTime = 0;



    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool){
    	id = "R" + hashCode();
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
    }


    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException {    	
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d >  +addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch){
                    /** New: Wrapping Fragile item cost 2 units of time*/
                    if(deliveryItem.isFragile()){
                        if(wrapTime >= 2){
                            receivedDispatch = false;
                            deliveryCounter = 0; // reset delivery counter
                            setRoute();
                            wrapTime = 0;
                            changeState(RobotState.DELIVERING);
                        }
                        else {
                            /** New: Wrapping Fragile item before delivery*/
                            wrap();
                            System.out.println(this.id + " is wrapping fragile item: " + deliveryItem.toString());
                        }

                    }
                    else {
                        receivedDispatch = false;
                        deliveryCounter = 0; // reset delivery counter
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }

                }
                break;
    		case DELIVERING:
    			if(current_floor == destination_floor){ // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    /** New: Wrapping Fragile item before delivery*/
                    if(deliveryItem.isFragile()){
                        assert (RobotMode.isCautionOn());
                        /** New: Unwrapping Fragile item before delivery costs 1 unit of time*/
                        if(unwrapTime >= 1){
                            delivery.deliver(deliveryItem);

                            deliveryItem = null;
                            unwrapTime = 0;
                            deliveryCounter++;
                            if(deliveryCounter > 3){  // Implies a simulation bug
                                throw new ExcessiveDeliveryException();
                            }
                            /** Check if want to return, i.e. if there is no item in the tube*/
                            if(tube == null && fragileItem == null && normalItem == null){
                                changeState(RobotState.RETURNING);
                            }
                            else{
                                /** If there is another item, set the robot's route to the location to deliver the item */
                                updateDeliveryItem();
                                setRoute();
                                changeState(RobotState.DELIVERING);
                            }
                            /** New: Release the floor*/
                            Building.releaseFloor(current_floor);
                            System.out.println(this.id + "released floor " + current_floor);
                        }
                        else {
                            /** New: Unwrapping fragile items*/
                            unwrap();
                            /** New: Locking the floor*/
                            Building.lockFloor(current_floor);
                            System.out.println(this.id + " is unwrapping fragile item: " + deliveryItem.toString());
                        }
                    }
                    else {
                        /** New: Deliver normal item*/
                        delivery.deliver(deliveryItem);
                        deliveryItem = null;
                        deliveryCounter++;
                        if(RobotMode.isCautionOn()){

                            if(deliveryCounter > 3){  // Implies a simulation bug
                                throw new ExcessiveDeliveryException();
                            }
                        }
                        else {
                            if(deliveryCounter > 2){  // Implies a simulation bug
                                throw new ExcessiveDeliveryException();
                            }
                        }

                        /** Check if want to return, i.e. if there is no item in the tube*/
                        if(tube == null && fragileItem == null && normalItem == null){
                            changeState(RobotState.RETURNING);
                        }
                        else{
                            /** If there is another item, set the robot's route to the location to deliver the item */
                            updateDeliveryItem();
                            setRoute();
                            changeState(RobotState.DELIVERING);
                        }
                    }



    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
    	}
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute() {
        /** Set the destination floor */
        //System.out.println("Tube空吗: " + tubeEmpty() + " Normal空吗: " + normalHandEmpty() + " Fragile空吗: " + specialHandEmpty());
        //System.out.println("发送物品的ID是 ：" + deliveryItem.id + "是否为脆弱： " + deliveryItem.fragile);
        destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     * New: Check locking status to decide whether to wait or move
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if(current_floor < destination){
            if(!Building.isFloorLocked(current_floor + 1)){
                current_floor++;
            }
            else {
                System.out.println(this.id + " is Waiting for Floor " + (current_floor + 1) + " to be released");
            }
        } else {
            if(!Building.isFloorLocked(current_floor - 1)){
                current_floor--;
            }
            else {
                System.out.println(this.id + " is Waiting for Floor " + (current_floor - 1) + " to be released");
            }
        }
    }

    public String getId(){return this.id;}
    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
            System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
    	}
    }

	public MailItem getTube() {
		return tube;
	}
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null);
	}

    public void addDeliveryItem(MailItem mailItem){
        this.deliveryItem = mailItem;
    }

    /** New: Update items on robot at the start or after a delivery*/
    public void updateDeliveryItem(){
	    assert (deliveryItem ==null);
	    if(normalItem !=null && fragileItem != null){
            if(Math.abs(normalItem.destination_floor - current_floor) >=Math.abs(fragileItem.destination_floor - current_floor)){
                deliveryItem = normalItem;
                normalItem = null;
            }
            else {
                deliveryItem = fragileItem;
                fragileItem = null;
            }
        }
	    else if(normalItem == null && fragileItem !=null){
	        deliveryItem = fragileItem;
	        fragileItem = null;
        }
	    else if(normalItem != null && fragileItem == null){
	        deliveryItem = normalItem;
	        normalItem = null;
        }
	    if(normalItem == null){
	        if(tube != null){
	            normalItem = tube;
	            tube = null;
            }
        }
    }

    public void addToHand(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(normalItem == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
        normalItem = mailItem;
		if (normalItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException, BreakingFragileItemException {
		assert(tube == null);
		if(mailItem.fragile) throw new BreakingFragileItemException();
		tube = mailItem;
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	/** new function too add a Fragile item to the special arm*/
    public void addToFragile(MailItem mailItem) throws ItemTooHeavyException, NormalItemOnFragileArmException {
        assert(fragileItem == null);
        if(!mailItem.fragile) throw new NormalItemOnFragileArmException();
        fragileItem = mailItem;
        if (fragileItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }

    public boolean tubeEmpty(){
        if(this.tube == null){
            return true;
        }
        else return false;
    }
    public boolean specialHandEmpty(){
        if(this.fragileItem == null){
            return true;
        }
        else return false;
    }
    public boolean normalHandEmpty(){
        if(this.normalItem == null){
            return true;
        }
        else return false;
    }


    @Override
    public void wrap() {
        wrapTime++;
        Simulation.addCautionTime();

    }
    public void unwrap(){
        unwrapTime++;
        Simulation.addCautionTime();
    }


}
