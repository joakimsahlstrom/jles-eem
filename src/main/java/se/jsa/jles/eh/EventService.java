package se.jsa.jles.eh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.jsa.jles.EventQuery;
import se.jsa.jles.EventStore;
import se.jsa.jles.NewEventNotificationListeners.NewEventNotificationListener;
import se.jsa.jles.internal.util.Objects;

/**
 * This class is designed to be used as a hub for keeper event consumers current with the events that are passed to an {@link EventStore}
 */
public class EventService implements NewEventNotificationListener {
	private final EventStore sourceStore;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final List<ListenerNotifier> subscriptions = new ArrayList<ListenerNotifier>();
	private final List<ListenerNotifier> feeds = new ArrayList<ListenerNotifier>();

	public EventService(EventStore sourceStore) {
		this.sourceStore = Objects.requireNonNull(sourceStore);
		sourceStore.registerEventListener(this);
	}

	/**
	 * The subscription is made up to date before this method returns!
	 * @param subscription
	 * @param eventQuery
	 */
	public void subscribe(EventSubscription subscription, EventQuery eventQuery) {
		subscriptions.add(new ListenerNotifier(subscription, eventQuery));
		update(subscriptions);
	}

	public void register(EventFeedReader reader, EventQuery eventQuery) {
		feeds.add(new ListenerNotifier(reader, eventQuery));
		executor.submit(new SubscriptionEmitter());
	}

	/**
	 * From EventListener
	 */
	@Override
	public void onNewEvent() {
		update(subscriptions);
		executor.submit(new SubscriptionEmitter());
	}

	// Private methods and classes

	private class SubscriptionEmitter implements Runnable {
		public SubscriptionEmitter() {
			// do nothing
		}
		@Override
		public void run() {
			update(feeds);
		}
	}

	private class ListenerNotifier {
		private final Iterator<Object> iterator;
		private final EventListener listener;

		public ListenerNotifier(EventListener listener, EventQuery eventQuery) {
			this.listener = listener;
			this.iterator = sourceStore.readEvents(eventQuery).iterator();
		}

		public boolean call() {
			if (iterator.hasNext()) {
				listener.onNewEvent(iterator.next());
				return true;
			}
			return false;
		}
	}

	private void update(Collection<ListenerNotifier> notifiers) {
		boolean changed;
		do {
			changed = false;
			for (ListenerNotifier notifier : notifiers) {
				changed = changed | notifier.call();
			}
		} while (changed);
	}

}
