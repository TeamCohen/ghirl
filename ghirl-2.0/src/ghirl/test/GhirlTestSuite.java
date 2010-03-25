package ghirl.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({CompactTextGraphTest.class,
	TestCachingTextGraph.class,
	TestCommandLineUtil.class,
	TestPersistantGraphSleepycat.class,
	TestTextGraph.class,
	TestTextGraphTermKidnapping.class})
public class GhirlTestSuite {

}