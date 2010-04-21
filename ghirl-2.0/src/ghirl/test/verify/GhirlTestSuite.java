package ghirl.test.verify;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	// Basic functionality tests:
	TestCompactTextGraph.class,
	TestCachingTextGraph.class,
	TestTextGraph.class,
	TestTextGraphTermKidnapping.class,
	// Tests that prove you fixed a bug:
	TestGraphLoaderFlavorBug.class,
	})
public class GhirlTestSuite {}