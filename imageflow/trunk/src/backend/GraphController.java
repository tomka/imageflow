package backend;
import graph.Node;
import helper.Tools;
import ij.IJ;
import ij.ImageJ;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import macro.MacroGenerator;
import models.Connection;
import models.ConnectionList;
import models.Input;
import models.Output;
import models.unit.UnitDescription;
import models.unit.UnitElement;
import models.unit.UnitFactory;
import models.unit.UnitList;
import models.unit.UnitElement.Type;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import application.ApplicationController;



/**
 * @author danielsenff
 *
 */
public class GraphController extends ApplicationController {

	private ApplicationController controller;

	private UnitList unitElements;
	private final ConnectionList connectionMap;
	private ImageJ imagej;
	/**
	 * List which stores copied Nodes.
	 */
	protected ArrayList<Node> copyNodesList;



	/**
	 * 
	 */
	public GraphController() {

		this.unitElements = new UnitList();
		this.connectionMap = new ConnectionList();
		this.copyNodesList = new ArrayList<Node>();
	}


	/**
	 * verification and generation of the ImageJ macro
	 */
	public void generateMacro() {
		////////////////////////////////////////////////////////
		// analysis and 
		// verification of the connection network
		////////////////////////////////////////////////////////

		if (!checkNetwork()) {
			System.out.println("Error in node network.");
			return;
		}

		// unitElements has to be ordered according to the correct processing sequence
		unitElements = sortList(unitElements);

		////////////////////////////////////////////////////////
		// generation of the ImageJ macro
		////////////////////////////////////////////////////////


		final String macro = MacroGenerator.generateMacrofromUnitList(unitElements);

		if(imagej == null)
			imagej = new ImageJ(null, ImageJ.EMBEDDED);
					IJ.log(macro);
		// !!!
		IJ.runMacro(macro, "");
	}


	/**
	 * check if all connections have in and output
	 * @param connectionMap
	 * @return
	 */
	public boolean checkNetwork() {

		if(!unitElements.hasUnitAsDisplay()) {
			System.err.println("The flow has no displayable units, running it doesn't do anything.");
			return false; 
		}


		if(connectionMap.size() > 0) {
			System.out.println("Number of connections: "+ connectionMap.size());
			for (Iterator iterator = connectionMap.iterator(); iterator.hasNext();) {
				Connection connection = (Connection) iterator.next();

				/*if (!connection.areImageBitDepthCompatible())
					return false;*/

				switch(connection.checkConnection()) {
				case MISSING_BOTH:
				case MISSING_FROM_UNIT:
				case MISSING_TO_UNIT:
					System.err.println("Faulty connection, no input or output unit found.");
					System.err.println(connection.toString() 
							+ " with status " + connection.checkConnection());
					return false;				
				}
			}
		} else if (unitElements.hasSourcesAsDisplay()) {
			// ok, we got no connections, but we have Source-units, 
			// which are set to display.

			//do nothing
		} else {
			System.err.println("no existing connections");
			return false;
		}


		//FIXME check if units got all the inputs they need
		if (!unitElements.areAllInputsConnected()) {
			System.err.println("not all required inputs are connected");
			return false;
		}


		//TODO check parameters
		return true;
	}


	/**
	 * @return the unitElements
	 */
	public UnitList getUnitElements() {
		return this.unitElements;
	}

	// !!!
	public static void main(final String[] args) {
		final GraphController controller = new GraphController();
		controller.setupExample1();
		controller.generateMacro();
	}

	/**
	 * @return
	 */
	public ConnectionList getConnections() {
		return this.connectionMap;
	}


	public static UnitList sortList(UnitList unitElements) {

		// temporary list, discarded after this method call
		UnitList orderedList = new UnitList();

		// reset all marks
		unmarkUnits(unitElements);

		int mark = 0;	// nth element, that has been sorted
		int i = 0; 		// nth lap in the loop
		int index = 0; 	// index 0 < i < unitElements.size()

		try {
			//loop over all units, selection sort, levelorder
			while(!unitElements.isEmpty()) {
				index = i % unitElements.size();
				UnitElement unit = (UnitElement) unitElements.get(index); 

				// check if all inputs of this node are marked
				// if so, this unit is moved from the old list to the new one
				if(unit.hasMarkedOutput()) throw new Exception("Unit has Output marked, " +
				"although the unit itself is not marked. This suggests an infinited loop.");
				if(unit.hasAllInputsMarked()) {
					mark++;	

					// increment mark
					// mark outputs
					unit.setMark(mark);

					// remove from the old list and
					// move this to the new ordered list
					Node remove = unitElements.remove(index);
					orderedList.add(remove);

				} else if (!unit.hasInputsConnected() 
						&& unit.getType() != Type.SOURCE) {
					// if unit has no connections actually, it can be discarded right away
					unitElements.remove(index);
					// if there is a branch with two units connected, the first one will be discarded, 
					// the second will still exist, but as the input is now missing, it will 
					// be deleted in the next lap
				} else if (!unit.hasOutputsConnected() 
						&& unit.getType() == Type.SOURCE 
						&& !unit.isDisplayUnit()) {
					// if source has no connected outputs and is not visible
					unitElements.remove(index);
				}
				// Selection Sort
				// each time an element whose previous nodes have already been registered
				// is found the next loop over the element list is one element shorter.
				// thereby having O(n^2) maybe this can be done better later
				i++;
			}

			for (Node node : orderedList) {
				unitElements.add(node);
			}
		} catch(Exception ex) {
			// restore list, without damaging it
		}

		return unitElements;
	}

	/**
	 * Resets all marks to zero.
	 * @param units
	 */
	public static void unmarkUnits(UnitList units) {
		for (Node node : units) {
			UnitElement unit = (UnitElement) node;
			unit.setMark(0);
		}
	}

	/**
	 * Get the List of copied {@link Node};
	 * @return
	 */
	public ArrayList<Node> getCopyNodesList() {
		return copyNodesList;
	}

	/**
	 * Removes the {@link UnitElement} from the unitList and its Connections.
	 * @param unit
	 * @return
	 */
	public boolean removeUnit(final UnitElement unit) {

		// new connection between the nodes that this deleted node was inbetween

		// get the outputs of the currently connected inputs
		int numberConnectedInputs = unit.getInputsCount();
		ArrayList<Output> connectedOutputs = new ArrayList<Output>(numberConnectedInputs);
		// we use a vector, add stuff to the end and iterate over this without indecies
		// check if pins are connected and ignor, if not
		for (int i = 0; i < numberConnectedInputs; i++) {
			Input input = unit.getInput(i);
			if(input.isConnected()) {
				connectedOutputs.add(input.getFromOutput());
			}
		}
		
		
		// same for inputs
		int numberConnectedOutputs = unit.getOutputsCount();
		ArrayList<Input> connectedInputs = new ArrayList<Input>(numberConnectedOutputs);
		for (int i = 0; i < numberConnectedOutputs; i++) {
			Output output = unit.getOutput(i);
			if(output.isConnected()) 
				connectedInputs.add(output.getToInput()); 
		}
		                                      
		// now we create new connections based on the lists of 
		// formerly connected outputs and inputs.
		// if it doesn't match, discard
		
		// get the longer list, inputs or outputs
		int numPins = Math.min(connectedInputs.size(), connectedOutputs.size());
		for (int i = 0; i < numPins; i++) {
			connectionMap.add(connectedInputs.get(i), connectedOutputs.get(i));
		}
		
		
		
		
		// delete old connections
		unbindUnit(unit);

		// delete Unit
		return unitElements.remove(unit);
	}

	/**
	 * Removes all connections to this {@link UnitElement}.
	 * @param unit
	 */
	public void unbindUnit(final UnitElement unit) {
		// find connections which are attached to this unit
		for (int i = 0; i < connectionMap.size(); i++) {
			Connection connection = (Connection) connectionMap.get(i);
			if(connection.isConnectedToUnit(unit)) {
				// delete connections
				connectionMap.remove(connection);
				i--;
			}
		}
	}

	public void setupExample1() {

		/*
		 * Wurzelbaum
		 */

		////////////////////////////////////////////////////////
		// setup of units
		////////////////////////////////////////////////////////


		//		unitElements.add(null);

		//		final UnitElement sourceUnit = UnitFactory.createSourceUnit("/Users/barthel/Applications/ImageJ/_images/zange1.png");
		//		
		//		UnitDescription blurUnitDescription = new UnitDescription();
		//		blurUnitDescription.setBlurValues();
		//		blurUnitDescription.parseUnitValuesFromXmlFile("GaussianBlur_Unit.xml");
		//		//final UnitElement blurUnit = UnitFactory.createGaussianBlurUnit(new Point(180, 50));
		//		final UnitElement blurUnit = UnitFactory.createProcessingUnit(blurUnitDescription, new Point(180, 50));
		//
		//		UnitDescription mergeUnitDescription = new UnitDescription();
		//		mergeUnitDescription.setImageCalculatorValues();
		//		//final UnitElement mergeUnit = UnitFactory.createImageCalculatorUnit(new Point(320, 100));
		//		final UnitElement mergeUnit = UnitFactory.createProcessingUnit(mergeUnitDescription,new Point(320, 100));
		//		final UnitElement noiseUnit = UnitFactory.createAddNoiseUnit(new Point(450, 100));
		//		noiseUnit.setDisplayUnit(true);


		UnitDescription sourceUnitDescription = new UnitDescription(Tools.getRoot(new File("xml_units/ImageSource_Unit.xml")));
		final UnitElement sourceUnit = UnitFactory.createProcessingUnit(sourceUnitDescription, new Point(30,100));

		UnitDescription blurUnitDescription = new UnitDescription(Tools.getRoot(new File("xml_units/GaussianBlur_Unit.xml")));
		final UnitElement blurUnit = UnitFactory.createProcessingUnit(blurUnitDescription, new Point(180, 50));

		UnitDescription mergeUnitDescription = new UnitDescription(Tools.getRoot(new File("xml_units/ImageCalculator_Unit.xml")));
		final UnitElement mergeUnit = UnitFactory.createProcessingUnit(mergeUnitDescription,new Point(320, 100));

		UnitDescription noiseUnitDescription = new UnitDescription(Tools.getRoot(new File("xml_units/AddNoise_Unit.xml")));
		final UnitElement noiseUnit = UnitFactory.createProcessingUnit(noiseUnitDescription,new Point(450, 100));
		noiseUnit.setDisplayUnit(true);

		// some mixing, so they are not in order
		unitElements.add(noiseUnit);
		unitElements.add(blurUnit);
		unitElements.add(sourceUnit);
		unitElements.add(mergeUnit);


		////////////////////////////////////////////////////////
		// setup the connections
		////////////////////////////////////////////////////////



		// add six connections
		// the conn is established on adding
		// fromUnit, fromOutputNumber, toUnit, toInputNumber
		Connection con;
		con = new Connection(sourceUnit,1,blurUnit,1);
		connectionMap.add(con);
		con = new Connection(blurUnit,1,mergeUnit,1);
		connectionMap.add(con);
		con = new Connection(sourceUnit,1,mergeUnit,2);
		connectionMap.add(con);
		con = new Connection(mergeUnit,1,noiseUnit,1);
		connectionMap.add(con);

		// remove one connection
		//connectionMap.remove( Connection.getID(2,1,5,1) );


	}

	public void setupExample0_XML() {

		UnitElement[] unitElement = null;
		int numUnits = 0;

		// setup of units
		try {
			System.out.println("Reading xml-description");
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(new File("xml_flows/Example0_flow.xml"));

			Element root = doc.getRootElement();

			// read units
			Element unitsElement = root.getChild("Units");

			if (unitsElement != null) {  
				List<Element> unitsList = unitsElement.getChildren();
				Iterator<Element> unitsIterator = unitsList.iterator();
				numUnits = unitsList.size() + 1;
				unitElement = new UnitElement[numUnits];

				// loop �ber alle Units
				while (unitsIterator.hasNext()) { 
					Element actualUnitElement = (Element) unitsIterator.next();
					int unitID = Integer.parseInt(actualUnitElement.getChild("UnitID").getValue());
					int xPos = Integer.parseInt(actualUnitElement.getChild("XPos").getValue());
					int yPos = Integer.parseInt(actualUnitElement.getChild("YPos").getValue());
					UnitDescription unitDescription = new UnitDescription(actualUnitElement.getChild("UnitDescription"));

					// create unit
					unitElement[unitID] = UnitFactory.createProcessingUnit(unitDescription, new Point(xPos, yPos));
					unitElement[unitID].setDisplayUnit(unitDescription.getIsDisplayUnit());
					unitElements.add(unitElement[unitID]);
				}
			}

			// read connections
			Element connectionsElement = root.getChild("Connections");

			if (connectionsElement != null) {  
				List<Element> connectionsList = connectionsElement.getChildren();
				Iterator<Element> connectionsIterator = connectionsList.iterator();

				// loop �ber alle connections
				while (connectionsIterator.hasNext()) { 
					Element actualConnectionElement = (Element) connectionsIterator.next();
					int fromUnitID = Integer.parseInt(actualConnectionElement.getChild("FromUnitID").getValue());
					int fromOutputNumber = Integer.parseInt(actualConnectionElement.getChild("FromOutputNumber").getValue());
					int toUnitID = Integer.parseInt(actualConnectionElement.getChild("ToUnitID").getValue());
					int toInputNumber = Integer.parseInt(actualConnectionElement.getChild("ToInputNumber").getValue());
					Connection con = new Connection(unitElement[fromUnitID], fromOutputNumber, unitElement[toUnitID], toInputNumber);
					connectionMap.add(con);
				}
			}
		}
		catch (Exception e) {
			System.err.println("Invalid XML-File!");
			e.printStackTrace();
		}	
	}

	public void setupExample2() {


		////////////////////////////////////////////////////////
		// setup of units
		////////////////////////////////////////////////////////


		UnitDescription sourceUnitDescription = new UnitDescription(Tools.getRoot(new File("xml_units/ImageSource_Unit.xml")));
		final UnitElement sourceUnit = UnitFactory.createProcessingUnit(sourceUnitDescription, new Point(30,100));

		final UnitElement to8BitUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/8Bit_Unit.xml"))), new Point(150, 100));
		final UnitElement to32BitUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/32Bit_Unit.xml"))), new Point(260, 100));

		final UnitElement convUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Convolver_Unit.xml"))), new Point(400, 50));
		final UnitElement convUnit2 = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Convolver_Unit.xml"))), new Point(400, 160));

		final UnitElement squareUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Square_Unit.xml"))), new Point(510, 50));
		final UnitElement squareUnit2 = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Square_Unit.xml"))), new Point(510, 160));

		final UnitElement addUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Add_Unit.xml"))), new Point(650, 100));
		final UnitElement fireUnit = UnitFactory.createProcessingUnit(new UnitDescription(Tools.getRoot(new File("xml_units/Fire_Unit.xml"))), new Point(770, 100));

		// some mixing, so they are not in order
		unitElements.add(sourceUnit);
		unitElements.add(to8BitUnit);
		unitElements.add(to32BitUnit);
		unitElements.add(convUnit);
		unitElements.add(squareUnit);
		unitElements.add(convUnit2);
		unitElements.add(squareUnit2);
		unitElements.add(addUnit);
		unitElements.add(fireUnit);
		fireUnit.setDisplayUnit(true);

		////////////////////////////////////////////////////////
		// setup the connections
		////////////////////////////////////////////////////////

		// add six connections
		// the conn is established on adding
		// fromUnit, fromOutputNumber, toUnit, toInputNumber

		connectionMap.add(new Connection(sourceUnit,1,to8BitUnit,1));
		connectionMap.add(new Connection(to8BitUnit,1,to32BitUnit,1));
		connectionMap.add(new Connection(to32BitUnit,1,convUnit,1));
		connectionMap.add(new Connection(to32BitUnit,1,convUnit2,1));
		connectionMap.add(new Connection(convUnit,1,squareUnit,1));
		connectionMap.add(new Connection(convUnit2,1,squareUnit2,1));
		connectionMap.add(new Connection(squareUnit,1,addUnit,1));
		connectionMap.add(new Connection(squareUnit2,1,addUnit,2));
		connectionMap.add(new Connection(addUnit,1,fireUnit,1));

	}


}

