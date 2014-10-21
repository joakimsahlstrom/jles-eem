package se.jsa.jles.eh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static se.jsa.jles.eh.AsyncAssert.assertEqualsEventually;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import se.jsa.jles.EventQuery;
import se.jsa.jles.EventStore;
import se.jsa.jles.EventStoreConfigurer;
import se.jsa.jles.eh.AsyncAssert.ValueRetriever;
import se.jsa.jles.eh.events.ChangeAccountNameEvent;
import se.jsa.jles.eh.events.OpenAccountEvent;

public class EventServiceTest {

	static class RecordingEventFeedReader implements EventFeedReader, EventSubscription {
		private final LinkedList<Object> eventQueue = new LinkedList<Object>();
		@Override
		public void onNewEvent(Object event) {
			this.eventQueue.offer(event);
		}

		public Object getEvent() {
			return eventQueue.isEmpty() ? null : eventQueue.poll();
		}

		public LinkedList<Object> getEvents() {
			return eventQueue;
		}

		public Integer getNumEvents() {
			return eventQueue.size();
		}
	}

	private final EventStore eventStore = EventStoreConfigurer.createMemoryOnlyConfigurer().testableEventDefinitions().configure();
	private final EventService eventService = new EventService(eventStore);
	private final RecordingEventFeedReader eventListener = new RecordingEventFeedReader();

	@Test
	public void eventFeed_existingEventIsEventuallyPassedToFeedReader() throws Exception {
		OpenAccountEvent expectedEvent = new OpenAccountEvent(1L, "test@test.com");
		eventStore.write(expectedEvent);

		final RecordingEventFeedReader subscription = new RecordingEventFeedReader();
		eventService.register(subscription, EventQuery.select(OpenAccountEvent.class));

		assertEqualsEventually(expectedEvent, new ValueRetriever() { @Override public Object get() { return subscription.getEvent(); } }, 50);
	}

	@Test
	public void eventFeed_existingEventsAreEventuallyPassedToSubscriber() throws Exception {
		OpenAccountEvent expectedEvent = new OpenAccountEvent(1L, "test@test.com");
		OpenAccountEvent expectedEvent2 = new OpenAccountEvent(2L, "test2@test.com");
		eventStore.write(expectedEvent);
		eventStore.write(expectedEvent2);

		eventService.register(eventListener, EventQuery.select(OpenAccountEvent.class));

		assertEqualsEventually(expectedEvent, new ValueRetriever() { @Override public Object get() { return eventListener.getEvent(); } }, 50);
		assertEqualsEventually(expectedEvent2, new ValueRetriever() { @Override public Object get() { return eventListener.getEvent(); } }, 50);
	}

	@Test
	public void eventFeed_newEventsArePassedToSubscriber() throws Exception {
		OpenAccountEvent expectedEvent = new OpenAccountEvent(1L, "test@test.com");

		eventService.register(eventListener, EventQuery.select(OpenAccountEvent.class));

		eventStore.write(expectedEvent);

		assertEqualsEventually(expectedEvent, new ValueRetriever() { @Override public Object get() { return eventListener.getEvent(); } }, 50);
	}

	@Test
	public void eventFeed_allEventsArePassedToSubscribersInTheOrderTheyEnteredTheSystem() throws Exception {
		final int firstBatchSize = 1000;
		final int secondBatchSize = 10;

		for (int i = 0; i < firstBatchSize; i++) {
			eventStore.write(createEvent(i));
		}

		eventService.register(eventListener, EventQuery.select(OpenAccountEvent.class));

		for (int i = firstBatchSize; i < firstBatchSize + secondBatchSize; i++) {
			eventStore.write(createEvent(i));
		}

		assertEqualsEventually(firstBatchSize + secondBatchSize, new ValueRetriever() { @Override public Object get() { return eventListener.getNumEvents(); } }, 2000);

		List<Object> events = eventListener.getEvents();
		for (int i = 0; i < firstBatchSize + secondBatchSize; i++) {
			assertEquals((long)i, (long)((OpenAccountEvent)events.get(i)).getAccountId());
		}
	}

	@Test
	public void eventFeed_canSubscribeToMultipleEventTypes() throws Exception {
		OpenAccountEvent oae1 = new OpenAccountEvent(1L, "test@test.com");
		ChangeAccountNameEvent cane1 = new ChangeAccountNameEvent(1L, "n1");
		OpenAccountEvent oae2 = new OpenAccountEvent(2L, "test@test.com");
		ChangeAccountNameEvent cane2 = new ChangeAccountNameEvent(2L, "n2");
		ChangeAccountNameEvent cane3 = new ChangeAccountNameEvent(1L, "n3");

		eventService.register(eventListener, EventQuery.select(OpenAccountEvent.class).join(ChangeAccountNameEvent.class));

		write(oae1, cane1, oae2, cane2, cane3);

		assertEventualEventsOrder(eventListener, oae1, cane1, oae2, cane2, cane3);
	}

	@Test
	public void eventFeed_receivesAlreadyWrittenEventsOnNewSubscription() throws Exception {
		OpenAccountEvent oae1 = new OpenAccountEvent(1L, "test@test.com");
		ChangeAccountNameEvent cane1 = new ChangeAccountNameEvent(1L, "n1");
		OpenAccountEvent oae2 = new OpenAccountEvent(2L, "test@test.com");
		ChangeAccountNameEvent cane2 = new ChangeAccountNameEvent(2L, "n2");
		ChangeAccountNameEvent cane3 = new ChangeAccountNameEvent(1L, "n3");
		write(oae1, cane1, oae2, cane2, cane3);

		eventService.register(eventListener, EventQuery.select(OpenAccountEvent.class).join(ChangeAccountNameEvent.class));

		assertEventualEventsOrder(eventListener, oae1, cane1, oae2, cane2, cane3);
	}

	@Test
	public void subscription_receivesNewEvents() throws Exception {
		OpenAccountEvent oae1 = new OpenAccountEvent(1L, "test@test.com");
		ChangeAccountNameEvent cane1 = new ChangeAccountNameEvent(1L, "n1");

		eventService.subscribe(eventListener, EventQuery.select(OpenAccountEvent.class));
		write(cane1, oae1);

		assertEventsOrder(eventListener, oae1);
	}

	@Test
	public void subscription_receivesOldEvents() throws Exception {
		OpenAccountEvent oae1 = new OpenAccountEvent(1L, "test@test.com");
		ChangeAccountNameEvent cane1 = new ChangeAccountNameEvent(1L, "n1");

		write(cane1, oae1);
		eventService.subscribe(eventListener, EventQuery.select(OpenAccountEvent.class));

		assertEventsOrder(eventListener, oae1);
	}

	@Test
	public void subscription_receivesMultipleEventTypes() throws Exception {
		OpenAccountEvent oae1 = new OpenAccountEvent(1L, "test@test.com");
		ChangeAccountNameEvent cane1 = new ChangeAccountNameEvent(1L, "n1");
		OpenAccountEvent oae2 = new OpenAccountEvent(2L, "test@test.com");
		ChangeAccountNameEvent cane2 = new ChangeAccountNameEvent(2L, "n2");
		ChangeAccountNameEvent cane3 = new ChangeAccountNameEvent(1L, "n3");

		write(oae1, cane1);
		eventService.subscribe(eventListener, EventQuery.select(OpenAccountEvent.class).join(ChangeAccountNameEvent.class));
		write(oae2, cane2, cane3);

		assertEventsOrder(eventListener, oae1, cane1, oae2, cane2, cane3);
	}



	// ----- Helper methods -----

	private void write(Object... events) {
		for (Object event : events) {
			eventStore.write(event);
		}
	}

	private static void assertEventualEventsOrder(final RecordingEventFeedReader subscription, Object... events) {
		assertEqualsEventually(5, new ValueRetriever() { @Override public Object get() { return subscription.getNumEvents(); } }, 50);
		assertEventsOrder(subscription, events);
	}

	private static void assertEventsOrder(final RecordingEventFeedReader subscription, Object... events) {
		Iterator<Object> actual = subscription.getEvents().iterator();
		for (Object expected : events) {
			assertEquals(expected, actual.next());
		}
		assertFalse(actual.hasNext());
	}

	private Object createEvent(int i) {
		return new OpenAccountEvent((long)i, "test" + i + "@test.com");
	}

}
