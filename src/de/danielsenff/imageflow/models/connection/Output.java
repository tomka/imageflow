package de.danielsenff.imageflow.models.connection;

import java.awt.Point;
import java.util.Collection;
import java.util.Vector;

import visualap.Node;
import visualap.Pin;
import de.danielsenff.imageflow.models.datatype.DataType;
import de.danielsenff.imageflow.models.datatype.DataTypeFactory;
import de.danielsenff.imageflow.models.datatype.DataTypeFactory.Image;
import de.danielsenff.imageflow.models.unit.UnitElement;


/**
 * Outputs are Pins that pass data to other units.
 * An Output can be connected to an arbitrary number of inputs.
 * @author danielsenff
 *
 */
public class Output extends Pin {
	
	/**
	 * the title of the image generated from this output
	 */
	protected String outputTitle; 
	/**
	 * the id of the image generated from this output
	 */
	protected String outputID; 
	/**
	 * indicates that this output should be shown
	 */
	protected boolean doDisplay;
	
	Vector<Connection> connections;
	
	/**
	 * write protection
	 */
	protected boolean locked = false;
	
	/**
	 * @param name
	 * @param shortName
	 * @param dataType
	 * @param nodeParent
	 * @param outputNumber
	 */
	public Output(
			final String name,
			final String shortName,
			final DataType dataType, 
			final UnitElement nodeParent, 
			final int outputNumber) {
		this(dataType, nodeParent, outputNumber);
		this.setupOutput(name, shortName);
	}
	
	/**
	 * @param dataType 
	 * @param nodeParent
	 * @param outputNumber
	 */
	public Output(final DataType dataType, 
			final UnitElement nodeParent, 
			final int outputNumber) {
		super(dataType, outputNumber, nodeParent);
		this.connections = new Vector<Connection>();
		if(getDataType() instanceof DataTypeFactory.Image) {
			((Image)getDataType()).setParentUnitElement((UnitElement) getParent());
			((Image)getDataType()).setParentPin(this);
		}
		generateID(((UnitElement)this.parent).getUnitID(), getIndex());
	}

	/**
	 * Sets the connection between this input and an output.
	 * @param fromUnitNumber
	 * @param fromOutputNumber
	 */
	private void generateID(final int unitNumber, final int outputNumber) {
		this.outputTitle = "Unit_" + unitNumber + "_Output_" + outputNumber;
		this.outputID = "ID_Unit_" + unitNumber + "_Output_" + outputNumber;
	}
	
	/**
	 * Adds a new {@link Connection} to this Output. 
	 * @param conn 
	 */
	public void addConnection(final Connection conn) {
		if(!isLocked())
			this.connections.add(conn);
	}

	/**
	 * Returns the {@link Connection} of this output.
	 * @return
	 */
	public final Collection<Connection> getConnections() {
		return this.connections;
	}
	
	/**
	 * @param name
	 * @param shortname
	 * @param outputBitDepth
	 */
	public void setupOutput(final String name, final String shortname, final int outputBitDepth) {
		this.displayName = name;
		this.shortDisplayName = shortname;
	}

	/**
	 * @param name
	 * @param shortname
	 */
	public void setupOutput(final String name, final String shortname) {
		this.displayName = name;
		this.shortDisplayName = shortname;
	}
	

	/**
	 * Activates to display the image at this output.
	 * @param doDisplay
	 */
	public void setDoDisplay(final boolean doDisplay) {
		this.doDisplay = doDisplay;
	}


	/**
	 * Returns whether or not this output displays it's image. 
	 * @return
	 */
	public boolean isDoDisplay() {
		return doDisplay;
	}

	/**
	 * Title with which the ouput is referred to in the macro. Like a variable-name.
	 * @return
	 */
	public String getOutputTitle() {
		return this.outputTitle;
	}
	

	/**
	 * 
	 * @return the imageID
	 */
	public String getOutputID() {
		return this.outputID;
	}

	@Override
	public String toString() {
		return super.toString()+" Unit:"+getParent()+"."+index;
	}
	
	/* (non-Javadoc)
	 * @see graph.Pin#getLocation()
	 */
	@Override
	public Point getLocation() {
		int height = parent.getDimension().height;
		int nump = ((UnitElement) parent).getOutputsCount();
		int y =  (index*height / nump ) - (height/(2*nump)) + parent.getOrigin().y;
		Point point = new Point(parent.getOrigin().x+parent.getDimension().width, y);
		return point;
	}

	@Override
	public UnitElement getParent() {
		return (UnitElement)super.getParent();
	}
	
	/**
	 * Traverses through {@link Input}s to see if the {@link Node} is connected somewhere.
	 * @param goal
	 * @return
	 */
	public boolean existsInInputSubgraph(final Node goal) {
		// self reference is true
		return traverseInput(getParent(), goal);
		// if nothing helps, it's false
	}

	
	private boolean traverseInput(UnitElement element, Node goal) {
		if(element.equals(goal)) {
			return true;
		} else if (element.hasInputsConnected()) {
			for (Input input : element.getInputs()) {
				if(input.isConnected()) {
					return traverseInput(input.getFromUnit(), goal);
				}
			}
		} 
		
		return false;
	}
	
	/**
	 * Resets the this Output, so that it is unconnected.
	 */
	public void disconnectAll() {
		if(!isLocked())
			this.connections.clear();
	}
	
	public Connection getConnectionTo(Pin toInput) {
		for (Connection connection : getConnections()) {
			if(connection.getInput().equals(toInput)) 
				return connection;
		}
		return null;
	}
	
	public void disconnectFrom(Pin input) {
		if(!isLocked()) {
			Connection oldConnection = getConnectionTo(input);
			this.connections.remove(oldConnection);
		}
		
	}
	

	/**
	 * Returns whether or not this Input is connected.
	 * @return 
	 */
	public boolean isConnected() {
		return !this.connections.isEmpty();
	}
	
	/**
	 * Checks if this Input is connected with the given {@link Output}
	 * @param input 
	 * @return
	 */
	public boolean isConnectedWith(Pin input) {
		for (Connection connection : getConnections()) {
			if(connection.getInput().equals(input))
				return true;
		}
		return false;
	}
	
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean isLocked) {
		this.locked = isLocked;
	}
	
}
