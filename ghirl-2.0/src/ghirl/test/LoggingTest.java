package ghirl.test;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;


public class LoggingTest {
	private static final Logger logger = Logger.getLogger(LoggingTest.class);
	@Test
	public void testLogging() {
		logger.debug("Logging working? 1");
		Logger.getRootLogger().setLevel(Level.DEBUG); logger.setLevel(Level.DEBUG);
		logger.debug("Logging working? 2");
		BasicConfigurator.configure();
		logger.debug("Logging working? 3");
	}
}
