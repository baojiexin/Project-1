package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.Robot;
import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;
import exceptions.NormalItemOnFragileArmException;

public class MailPool implements IMailPool {

	private class Item {
		int destination;
		MailItem mailItem;
		
		public Item(MailItem mailItem) {
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.destination > i2.destination) {  // Further before closer
				order = 1;
			} else if (i1.destination < i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException, BreakingFragileItemException, NormalItemOnFragileArmException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i);
		} catch (Exception e) { 
            throw e; 
        } 
	}
	private boolean robotReady(Robot robot){
		if(!robot.normalHandEmpty() && !robot.specialHandEmpty() && !robot.tubeEmpty()){
			return true;
		}
		else return false;
	}
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException, BreakingFragileItemException, NormalItemOnFragileArmException {
		Robot robot = i.next();
		assert(robot.isEmpty());
//		System.out.printf("目前pool有多少个 " + "P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		if(RobotMode.isCautionOn()){
			/**
			 * Load a robot in caution mode
			 */

			LinkedList<Item> tempList = new LinkedList<>();
			while (pool.size() > 0 && !robotReady(robot)){
				Item currentItem = pool.pollFirst();
				if(robot.specialHandEmpty()){
					if(currentItem.mailItem.isFragile()){
						robot.addToFragile(currentItem.mailItem);

						continue;
					}
				}
				else if(!robot.specialHandEmpty() && currentItem.mailItem.isFragile()){
					tempList.add(currentItem);
					continue;
				}
				if(robot.normalHandEmpty()){
					if(!currentItem.mailItem.isFragile()){
						robot.addToHand(currentItem.mailItem);
						continue;
					}
				}
				if(robot.tubeEmpty() && (!robot.normalHandEmpty())){
					if(!currentItem.mailItem.isFragile()){
						robot.addToTube(currentItem.mailItem);
						continue;
					}
				}
				tempList.add(currentItem);

			}
			pool.addAll(tempList);
			tempList = new LinkedList<>();
			if(!(robot.specialHandEmpty() && robot.normalHandEmpty())){
				robot.dispatch();
				i.remove();
			}

		}
		/**
		 * Load a robot in a normal mode
		 */
		else {
			if (pool.size() > 0) {
				try {
					robot.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
					j.remove();
					if (pool.size() > 0) {
						robot.addToTube(j.next().mailItem);
						j.remove();
					}
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
				} catch (Exception e) {
					throw e;
				}
			}
		}
		robot.updateDeliveryItem();

	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
