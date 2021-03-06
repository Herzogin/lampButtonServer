package lampButtonServer;

import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.htw.fiw.vs.IBinder;
import org.htw.fiw.vs.team1.ButtonInterface;
import org.htw.fiw.vs.team1.ControllerInterface;
import org.htw.fiw.vs.team1.LampInterface;

public class Controller extends java.rmi.server.UnicastRemoteObject implements ControllerInterface {
	private LampInterface li = null;
	private List<LampInterface> lampGroup = new ArrayList<LampInterface>();
	private List<String> buttonGroup = new ArrayList<String>();
	boolean blink = false;
	boolean sequence = false;
	Thread t;
	Thread t2;
	private HashMap<String, String> patternHashMap = new HashMap<String, String>();

	public Controller() throws RemoteException {
		super();
		t = new Thread(new BlinkThread(lampGroup));
		t2 = new Thread(new SequenceThread(lampGroup));
		patternHashMap.put("default", "default");
	}

	public void start() {
		try {
			System.out.println("Controller started. Registry gets created...");
			
			IBinder registry = (IBinder) Naming.lookup("rmi://localhost/binder");
						
			registry.bind("controller", this);
						
			String[] list = registry.list();
			
			for (String item : list ) {

				if (item.contains("button")) {
					ButtonInterface bi = (ButtonInterface) registry.lookup(item);
					bi.register(this);
					System.out.println("button was registered: " + item);
					buttonGroup.add(item);

				} else if (item.contains("lamp")) {
					li = (LampInterface) registry.lookup(item);

					lampGroup.add(li);
					System.out.println("lamp was registered: " + item);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	class BlinkThread implements Runnable {
		List<LampInterface> lampGroup;
		
		public BlinkThread(List<LampInterface> lampGroup) {
			this.lampGroup = lampGroup;
		}
		
		public void turnLampsOff() {
			for (int i = 0; i < lampGroup.size(); i++) {
				try {
					System.out.println("lampen aus");
					lampGroup.get(i).turnOff();
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			}
		}
		
		public void changeLampStatus(int i) {
			try {
				lampGroup.get(i).changeStatus();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	    public void run() {
	    	while(true) {
	    		if (Thread.interrupted()) {
	    			turnLampsOff();
	    			return;
	    		}
	    		
	    		//EVEN
				for (int i = 0; i < lampGroup.size(); i++) {
					if (i % 2 == 0){
						changeLampStatus(i);
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("interupted"+ e);
	    			turnLampsOff();
					return;
				}
				for (int i = 0; i < lampGroup.size(); i++) {
					if (i % 2 == 0){
						changeLampStatus(i);
					}
				}
				//ODD
				for (int i = 0; i < lampGroup.size(); i++) {
					if (i % 2 != 0){
						changeLampStatus(i);
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("interupted"+ e);
					turnLampsOff();
					return;
				}
				for (int i = 0; i < lampGroup.size(); i++) {
					if (i % 2 != 0){
						changeLampStatus(i);
					}
				}
			}
	    }
	}
	
	class SequenceThread implements Runnable {
		List<LampInterface> lampGroup;
		
		public SequenceThread(List<LampInterface> lampGroup) {
			this.lampGroup = lampGroup;
		}
		
		public void turnLampsOff() {
			for (int i = 0; i < lampGroup.size(); i++) {
				try {
					System.out.println("lampen aus");
					lampGroup.get(i).turnOff();
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			}
		}
		
		public void changeLampStatus(int i) {
			try {
				lampGroup.get(i).changeStatus();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	    public void run() {
	    	while(true) {
	    		if (Thread.interrupted()) {
	    			turnLampsOff();
	    			return;
	    		}
	    		
	    		for (int i = 0; i < lampGroup.size(); i++) {
					try {
						lampGroup.get(i).changeStatus();
						Thread.sleep(1000);
						lampGroup.get(i).changeStatus();
					} catch (RemoteException re) {
						re.printStackTrace();
					} catch (InterruptedException e) {
						turnLampsOff();
		    			return;
					}
				}
	    	}
	    }
	}

	@Override
	public void update(String name) throws RemoteException {
		System.out.println(name);
		System.out.println("update called");
		try {
			if (patternHashMap.containsKey(name)){
				if (patternHashMap.get(name).equals("blink")) {
					if (!blink) { 
						blink = true; 
						System.out.println("blink is: " + blink);
						try {
							t.start();
						}
						catch (IllegalThreadStateException e) {
							t = new Thread(new BlinkThread(lampGroup));
							t.start();
						}
						
					}
					else if (blink) {
						blink = false;
						System.out.println("blink is: " + blink);
						t.interrupt();
						System.out.println("interrrupt called");
					}
				}
				else if (patternHashMap.get(name).equals("even-odd")) {
					for (int i = 0; i < lampGroup.size(); i++) {
						if (buttonGroup.indexOf(name) % 2 == 0) {
							if (i % 2 == 0){
								lampGroup.get(i).changeStatus();
								System.out.println("Changed status of lamp: " + lampGroup.get(i));
							}
						} else {
							if (i % 2 != 0) {
								lampGroup.get(i).changeStatus();
								System.out.println("Changed status of lamp: " + lampGroup.get(i));
							}
						}
					}
				}
				else if (patternHashMap.get(name).equals("all-lamps")) {
					for (LampInterface lamp: lampGroup) {
						lamp.changeStatus();
						System.out.println("Changed status of lamp: " + lamp);
					}
				}
				else if (patternHashMap.get(name).equals("switch-off")) {
					for (int i = 0; i < lampGroup.size(); i++) {
						try {
							System.out.println("lampen aus");
							lampGroup.get(i).turnOff();
						} catch (RemoteException re) {
							re.printStackTrace();
						}
					}
				}
				else if (patternHashMap.get(name).equals("sequence")) {
					if (!sequence) { 
						sequence = true; 
						try {
							t2.start();
						}
						catch (IllegalThreadStateException e) {
							t2 = new Thread(new SequenceThread(lampGroup));
							t2.start();
						}
					}
					else {
						sequence = false;
						t2.interrupt();
						System.out.println("interrrupt called");
					}
					
				}
				else {
					System.out.println("No pattern defined. Button does nothing.");
				}
			}
			else {
				System.out.println("Key didn't exist.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws RemoteException, UnknownHostException {
		Controller s = new Controller();
		
		s.start();
	
	}

	@Override
	public void changePattern(String buttonName, String pattern) throws RemoteException {
		patternHashMap.put(buttonName, pattern);
		System.out.println(patternHashMap);
	}
	
}
