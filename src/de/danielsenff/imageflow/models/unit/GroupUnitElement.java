/**
 * 
 */
package de.danielsenff.imageflow.models.unit;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import visualap.GList;
import visualap.Node;
import visualap.Pin;
import de.danielsenff.imageflow.models.connection.Connection;
import de.danielsenff.imageflow.models.connection.ConnectionList;
import de.danielsenff.imageflow.models.connection.Input;
import de.danielsenff.imageflow.models.connection.Output;
import de.danielsenff.imageflow.models.connection.ProxyInput;
import de.danielsenff.imageflow.models.connection.ProxyOutput;

/**
 * @author danielsenff
 *
 */
public class GroupUnitElement extends UnitElement {

	/**
	 * List of units included in this Group
	 */
	protected UnitList units;
	private Vector<Connection> originalConnections;
	private Collection<Connection> internalConnections;
	private Collection<Connection> externalConnections;


	public GroupUnitElement(Point point, String name) {
		super(point, "name", "");
		setLabel(name);
		init();
	}

	/**
	 * @param origin
	 * @param unitName
	 * @param selections 
	 * @param allUnits 
	 * @param unitsImageJSyntax
	 */
	public GroupUnitElement(Point origin, String unitName,
			final Collection<Node> selections, final UnitList allUnits) {
		super(origin, unitName, "");
		init();
		try {
			putUnits(selections, allUnits);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void init() {
		this.units = new UnitList();
		this.internalConnections = new Vector<Connection>();
		this.externalConnections = new Vector<Connection>();
		this.originalConnections = new Vector<Connection>();
	}


	public void putUnits(final Collection<Node> unitsToAdd, final UnitList allUnits) throws Exception {

		findRelevantConnections(unitsToAdd, allUnits.getConnections());
		
		if(selectedUnitsConsistentGroup(unitsToAdd, allUnits)) {

			for (Node node : unitsToAdd) {
				this.units.add(node);
			}

			/*
			 * determine position
			 */

			int x, y;
			x = (int) getUnit(0).getOrigin().getX();
			y = (int) getUnit(0).getOrigin().getY();
			setOrigin(new Point(x, y));

			dealWithConnections(allUnits.getConnections());
		} else throw new Exception("group not albe");

		/*
		 * remove original units from workflow
		 */

		
		for (Node node : this.units) {
			// this removes not only the node from the unitList
			// but also disconnects all connections to this unit.
			// Therefore all connections are backuped before doing this.
			allUnits.remove(node);
		}

		// Reconnecting the connections within the group.
		for (Connection connection : internalConnections) {
			connection.connect();
		}

		
		// uh, reconnect the external connections
		// if the target-unit is contained in the group
		// just not sure why not connecting from-units
		for (Connection connection : externalConnections) {
			if(this.units.contains(connection.getToUnit())
					//|| this.units.contains(connection.getFromUnit())
			) {
				connection.connect();
			}
		}
		
		// proxy connections
		
		for (Input input : getInputs()) {
			((ProxyInput)input).getEmbeddedInput().setLocked(true);
		}
		for (Output output : getOutputs()) {
			((ProxyOutput)output).getEmbeddedOutput().setLocked(true);
		}
		
		
		int lowestX = 3000, lowestY = 3000;
		for (Node node : getNodes()) {
			if(node.getOrigin().x < lowestX
				&& node.getOrigin().y < lowestY) {
				lowestX = node.getOrigin().x;
				lowestY = node.getOrigin().y;
			}
		}
		
		// offset from original
		int deltaX = lowestX - 25;
		int deltaY = lowestY - 25;
		
		for (Node node : getNodes()) {
			int x = node.getOrigin().x, y = node.getOrigin().y;
			node.getOrigin().setLocation(x-deltaX, y-deltaY);
		}
	}

	/**
	 * True if the selected Units result in a consistent group.
	 * @param allUnits 
	 * @param unitsToAdd 
	 * @return
	 */
	private boolean selectedUnitsConsistentGroup(Collection<Node> unitsToAdd, UnitList allUnits) {

		/*
		 * for each input we test, if every unit in input graph is 
		 */

		for (Connection connection : this.externalConnections) {
			for (Node node : unitsToAdd) {
				if(unitsToAdd.contains(connection.getToUnit())
						&& connection.getInput().isConnectedInInputBranch(node))
					return false;
			}
		}

		return true;
	}

	private boolean isToUnitIn(final Collection<Node> unitsToAdd, final Connection connection) {

		UnitElement unit = (UnitElement)connection.getFromUnit();
		if(unitsToAdd.contains(unit)
				&& unitsToAdd.contains(connection.getToUnit())) {
			// unit is in selection
			// check if the preceding unit is also in selection
			return true;

		} else {
			for (Input input : unit.getInputs()) {
				if(input.isConnected() ) {
					return isToUnitIn(unitsToAdd, input.getConnection());
				}	
			}
		}
		return false;
	}

	public void dealWithConnections(final ConnectionList allConnections) {

		for (Connection connection : externalConnections) {
			this.originalConnections.add(connection);
		}

		/*
		 * create inputs and outputs based on external connections
		 */
		HashMap<Output, ProxyOutput> externalOutputs = new HashMap<Output, ProxyOutput>();
		for (Connection connection : externalConnections) {
			if(contains(connection.getToUnit())) 
			{
				ProxyInput pInput = new ProxyInput(connection.getInput(), this, getInputsCount()+1);
				addInput(pInput);
				Connection newconn = new Connection(connection.getOutput(), pInput);
				allConnections.add(newconn);
			}
			if(contains(connection.getFromUnit())) 
			{
				ProxyOutput pOutput;
				if(externalOutputs.containsKey(connection.getOutput())) {
					// output already used, take existing proxy
					pOutput = externalOutputs.get(connection.getOutput());
				} else {
					// output not yet used, create new
					pOutput = new ProxyOutput(connection.getOutput(), this, getOutputsCount()+1);
					addOutput(pOutput);
					externalOutputs.put(connection.getOutput(), pOutput);
				}
				Connection newconn = new Connection(pOutput, connection.getInput());
				allConnections.add(newconn);
			}
		}
	}

	private final void findRelevantConnections(Collection<Node> unitsToAdd, final ConnectionList allConnections) {
		for (Connection connection : allConnections) {
			for (Node node : unitsToAdd) {
				if (connection.isConnectedToUnit(node) 
						&& !externalConnections.contains(connection))
					externalConnections.add(connection);
			}
		}

		for (Connection connection : externalConnections) {
			for (Node node : unitsToAdd) {
				for (Node node2 : unitsToAdd) {
					if(connection.getFromUnit().equals(node)
							&& connection.getToUnit().equals(node2)) {
						internalConnections.add(connection);
					}	
				}
			}
		}
		
		/*
		 * determine which connections "leave" the group
		 */

		for (Connection connection : internalConnections) {
			externalConnections.remove(connection);
		}
		
	}


	/**
	 * @return the externalConnections
	 */
	public Collection<Connection> getExternalConnections() {
		return externalConnections;
	}

	
	/**
	 * Returns true if the node in question is contained in this GroupUnit.
	 * @param node
	 * @return
	 */
	public boolean contains(Node node) {
		return this.units.contains(node);
	}

	/**
	 * Number of elements grouped in this GroupUnit.
	 * @return
	 */
	public int getGroupSize() {
		return this.units.getSize();
	}

	/**
	 * 
	 * @param i
	 * @return
	 */
	public Node getUnit(int i) {
		return this.units.get(i);
	}

	/**
	 * Returns a Collection of Units embedded in this Group.
	 * @return
	 */
	public GList<Node> getNodes() {
		return this.units;
	}

	/**
	 * @return the originalConnections
	 */
	public Vector<Connection> getOriginalConnections() {
		return originalConnections;
	}


	/**
	 * @return the internalConnections
	 */
	public final Collection<Connection> getInternalConnections() {
		return internalConnections;
	}

	@Override
	public GroupUnitElement clone() {

		GroupUnitElement groupClone = new GroupUnitElement(getOrigin(), getLabel());
		groupClone = initClone(groupClone);
		
		
		
		return groupClone;
	}

	protected GroupUnitElement initClone(GroupUnitElement groupClone) {
		/*
		 * clone included units
		 */
		HashMap<Pin, Pin> correspondingPins = new HashMap<Pin, Pin>();

		for (Node node : getNodes()) {
			Node c;
			try {
				c = node.clone();
				groupClone.getNodes().add(c);

				if(node instanceof UnitElement) {
					UnitElement unit = (UnitElement) node;
					UnitElement unitClone = (UnitElement) c;
					for (int i = 0; i < unit.getInputsCount(); i++) {
						correspondingPins.put(unit.getInput(i), unitClone.getInput(i));
					}
					for (int i = 0; i < unit.getOutputsCount(); i++) {
						correspondingPins.put(unit.getOutput(i), unitClone.getOutput(i));
					}
				}

			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}

		/*
		 * reconnect internal connections
		 * the internal connections have the original pins
		 * we have to create new connections with the respective pins 
		 */

		for (Connection originalConnection : getInternalConnections()) {

			Input cloneInput = (Input) correspondingPins.get(originalConnection.getInput());
			Output cloneOutput = (Output) correspondingPins.get(originalConnection.getOutput());
			Connection newConnection = new Connection(cloneInput, cloneOutput);
			groupClone.getInternalConnections().add(newConnection);
		}



		/*
		 * clone pins
		 */

		for (int i = 0; i < getInputsCount(); i++) {
			if(getInput(i) instanceof ProxyInput) {
				Input input = ((ProxyInput)getInput(i)).getEmbeddedInput();
				Input embeddedInputClone = (Input) correspondingPins.get(input); 
				ProxyInput pInput = new ProxyInput(embeddedInputClone, groupClone, i+1);
				groupClone.addInput(pInput);
			} else {
				super.cloneInput(groupClone, i);
			}
		}
		for (int i = 0; i < getOutputsCount(); i++) {
			if(getOutput(i) instanceof ProxyOutput) {
				Output output = ((ProxyOutput)getOutput(i)).getEmbeddedOutput();
				Output embeddedOutputClone = (Output) correspondingPins.get(output); 
				ProxyOutput pOutput = new ProxyOutput(embeddedOutputClone, groupClone, i+1);
				groupClone.addOutput(pOutput);
			} else {
				super.cloneOutput(groupClone, i);
			}
		}

		return groupClone;
	}
	
	@Override
	public String getHelpString() {
		String string = "Grouped units: \n";
		for (Node node : getNodes()) {
			string += node.getLabel() + " \n ";
		}
		return string;
	}

	public void showGroupWindow() {
		
	}
	
}
