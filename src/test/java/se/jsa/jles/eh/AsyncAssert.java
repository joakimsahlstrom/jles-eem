package se.jsa.jles.eh;

import static org.junit.Assert.fail;

public class AsyncAssert {

	public static void assertEqualsEventually(Object expectedValue, ValueRetriever retriever, long timeout) {
		long start = System.currentTimeMillis();
		Object latest = retriever.get();
		while (!equals(expectedValue, latest) && !expired(start, timeout)) {
			try {
				Thread.sleep(10);
				latest = retriever.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (!equals(expectedValue, latest)) {
			fail("Values did not match! Expected: " + expectedValue + " was: " + retriever.get());
		}
	}

	private static boolean equals(Object expectedValue, Object object) {
		return expectedValue == null ? object == null : expectedValue.equals(object);
	}

	private static boolean expired(long start, long timeout) {
		return System.currentTimeMillis() - start >= timeout;
	}

	public interface ValueRetriever {
		Object get();
	}


}
