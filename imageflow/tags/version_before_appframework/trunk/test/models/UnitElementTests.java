/**
 * 
 */
package models;

import java.awt.Dimension;
import java.awt.Point;

import junit.framework.TestCase;
import models.unit.UnitElement;
import models.unit.UnitFactory;

/**
 * @author danielsenff
 *
 */
public class UnitElementTests extends TestCase {


	public void testInputsActualCount() {

		UnitElement unit = UnitFactory.createAddNoiseUnit();

		assertEquals("number outputs",1, unit.getOutputsCount());
		assertEquals("number inputs",1, unit.getInputsCount());

		UnitElement source = UnitFactory.createBackgroundUnit(new Dimension(100,100));
		assertEquals("number outputs",1, source.getOutputsCount());
		assertEquals("number inputs",0, source.getInputsCount());

		UnitElement merge = UnitFactory.createImageCalculatorUnit();
		assertEquals("number outputs",1, merge.getOutputsCount());
		assertEquals("number inputs",2, merge.getInputsCount());

		UnitElement sink = UnitFactory.createHistogramUnit(new Point(0,0));
		assertEquals("number outputs",0, sink.getOutputsCount());
		assertEquals("number inputs",1, sink.getInputsCount());
	}


	/**
	 *  
	 */
	public void testAddParameter() {
		//unit with one allowed parameter
		UnitElement unit = new UnitElement("name", "");

		//test usual adding
		boolean addFirst = unit.addParameter(ParameterFactory.createParameter("integer", 1, "ein int"));
		assertTrue("add first", addFirst);


		//add one more than actually allowed
		boolean addSecond = unit.addParameter(ParameterFactory.createParameter("integer", 1, "ein int"));
		assertTrue("add second", addSecond);
	}

	public void testHasInputs() {

		// Sources have no inputs
		UnitElement sourceUnit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));
		assertFalse("source", sourceUnit.hasInputs());

		UnitElement filter = UnitFactory.createAddNoiseUnit();
		assertTrue("filter", filter.hasInputs());

	}


	public void testAddInput() {
		//unit with one allowed parameter
		UnitElement unit = new UnitElement("name", "");

		//test usual adding
		boolean addFirst = unit.addInput("input", "i", 4, false);
		assertTrue("add first", addFirst);


		//add one more than actually allowed
		boolean addSecond = unit.addInput("input", "i", 4, false);
		assertTrue("add second", addSecond);
	}



	public void testHasAllInputsMarked() {

		// test output-only
		UnitElement sourceUnit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));

		// test input/output case
		UnitElement filterUnit1 = UnitFactory.createAddNoiseUnit();
		UnitElement filterUnit2 = UnitFactory.createAddNoiseUnit();

		Connection conn = new Connection(sourceUnit, 1, filterUnit1, 1);
		ConnectionList connList = new ConnectionList();
		connList.add(conn);

		//assertion

		assertTrue("source has inputs marked", sourceUnit.hasAllInputsMarked());
		// the source is not yet marked, so the first filter should give false
		assertFalse("filter1 has no inputs marked yet", filterUnit1.hasAllInputsMarked());
		assertFalse("filter2 has inputs marked", filterUnit2.hasAllInputsMarked());

		//set mark on the source, now the filter next connected should find this mark
		sourceUnit.setMark(1);

		assertEquals("filter1 has inputs marked", true, filterUnit1.hasAllInputsMarked());

	}


	public void testSetMark() {

		// test output-only
		UnitElement sourceUnit = UnitFactory.createBackgroundUnit(new Dimension(12, 12));

		// test input/output case
		UnitElement filterUnit1 = UnitFactory.createAddNoiseUnit();

		//		assertEquals("mark on source before setting", false, sourceUnit.hasAllInputsMarked());
		//		assertEquals("mark on filter before setting", false, filterUnit1.hasAllInputsMarked());

		assertUnitMarks(sourceUnit, 0);
		assertUnitMarks(filterUnit1, 0);

		//set marks
		sourceUnit.setMark(1);
		filterUnit1.setMark(1);

		assertUnitMarks(sourceUnit, 1);
	}

	/**
	 * @param sourceUnit
	 */
	private void assertUnitMarks(UnitElement sourceUnit, int expected) {
		for (Input input : sourceUnit.getInputs()) {
			assertEquals("mark set in "+sourceUnit+" for "+input, expected, input.getMark());
		}
		for (Output output : sourceUnit.getOutputs()) {
			assertEquals("mark set in "+sourceUnit+" for "+output, expected, output.getMark());
		}
	}

	public void testClone() {
		UnitElement mergeUnit = UnitFactory.createImageCalculatorUnit();


		UnitElement clone = mergeUnit.clone();

		//assertions

		assertFalse("object the same", mergeUnit.equals(clone));
		assertEquals("number of max inputs", 
				mergeUnit.getInputsCount(), clone.getInputsCount());
		assertEquals("number of max outputs", 
				mergeUnit.getOutputsCount(), clone.getOutputsCount());
		assertEquals("number of max parameters", 
				mergeUnit.getParametersCount(), clone.getParametersCount());
		assertEquals("unit name", mergeUnit.getUnitName(), clone.getUnitName());
		assertNotSame("unit id", mergeUnit.getUnitID(), clone.getUnitID());
		assertFalse("Unitid not the same",(mergeUnit.getUnitID() == clone.getUnitID()));

		// assert Object
		MacroElement mergeObject = (MacroElement) mergeUnit.getObject(); 
		MacroElement cloneObject = (MacroElement) clone.getObject();
		assertFalse("contained object", mergeObject.equals(cloneObject));
		assertEquals("object imagej syntax", 
				mergeObject.getImageJSyntax() , cloneObject.getImageJSyntax());

		// assert Inputs
		assertEquals("inputs actual", mergeUnit.getInputsCount(), clone.getInputsCount());
		for (int i = 0; i < clone.getInputsCount(); i++) {
			Input cloneInput = clone.getInput(i);
			Input mergeInput = mergeUnit.getInput(i);
			assertFalse("input equals", cloneInput.equals(mergeInput));
			assertFalse("input parents equals", 
					cloneInput.getParent().equals(mergeInput.getParent()));

		}


		// assert Outputs
		//			assertEquals("outputs actual", mergeUnit.getOutputsActualCount(), clone.getOutputsActualCount());
		for (int i = 0; i < clone.getOutputsCount(); i++) {
			Output cloneOutput = clone.getOutput(i);
			Output mergeOutput = mergeUnit.getOutput(i);
			assertFalse("output equals", cloneOutput.equals(mergeOutput));
			assertFalse("output parents equals", 
					cloneOutput.getParent().equals(mergeOutput.getParent()));

		}

	}


}