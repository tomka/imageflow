/**
 * 
 */
package models;

/**
 * @author danielsenff
 *
 */
public class ParameterFactory {

	/**
	 * Creates the right UnitElement based on the given parameters.
	 * @param displayName 
	 * @param parameter 
	 * @param helpString 
	 * @return 
	 */
	
	// old version should be removed
	public static Parameter createParameter(final String displayName, 
			Object parameter, 
			String helpString) {
		if(parameter instanceof String) {
			return new StringParameter(displayName, (String) parameter, helpString);
		} else if (parameter instanceof Double ) {
			return new DoubleParameter(displayName, (Double) parameter, helpString);
		} else if ( parameter instanceof Integer) {
//			return new DoubleParameter(displayName, (Double) parameter, helpString);
			return new IntegerParameter(displayName, (Integer) parameter, helpString);
		} else if (parameter instanceof Boolean) {
			return new BooleanParameter(displayName, (Boolean) parameter, "", helpString);
		} else if (parameter instanceof String[]) {
			return new ChoiceParameter(displayName, (String[]) parameter, 
					((String[]) parameter)[0], helpString);
		}
		System.out.println("Parameter not recognized: " + parameter);
		return null;

	}
	
	public static Parameter createParameter(final String displayName, 
			Object parameter,
			String trueString,
			String helpString) {
		if(parameter instanceof String) {
			return new StringParameter(displayName, (String) parameter, helpString);
		} else if (parameter instanceof Double ) {
			return new DoubleParameter(displayName, (Double) parameter, helpString);
		} else if ( parameter instanceof Integer) {
//			return new DoubleParameter(displayName, (Double) parameter, helpString);
			return new IntegerParameter(displayName, (Integer) parameter, helpString);
		} else if (parameter instanceof Boolean) {
			return new BooleanParameter(displayName, (Boolean) parameter, trueString, helpString);
		} else if (parameter instanceof String[]) {
			return new ChoiceParameter(displayName, (String[]) parameter, 
					((String[]) parameter)[0], helpString);
		}
		System.out.println("Parameter not recognized: " + parameter);
		return null;

	}
	
}
