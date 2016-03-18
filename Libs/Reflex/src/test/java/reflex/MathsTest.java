package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Test;

public class MathsTest extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(MathsTest.class);

	@Test
	public void testDivision1() throws RecognitionException {
		String program = "i = 20;\n" +
			"println(i/2); \n";

		String output = runScript(program, null);
		assertEquals("10", output.trim());
	}
	
	@Test
	public void testDivision2() throws RecognitionException {
		String program = "i = 21;\n" +
			"println(i/2); \n";

		String output = runScript(program, null);
		assertEquals("10.5", output.trim());
	}
	
}