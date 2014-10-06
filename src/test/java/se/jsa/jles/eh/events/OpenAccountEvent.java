package se.jsa.jles.eh.events;



public class OpenAccountEvent {
	private final Long accountId;
	private final String email;

	public OpenAccountEvent(Long accountId, String email) {
		this.accountId = accountId;
		this.email = email;
	}

	public Long getAccountId() {
		return accountId;
	}

	public String getEmail() {
		return email;
	}

	public SerializableEventV1 asSerializable() {
		return new SerializableEventV1(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof OpenAccountEvent)) {
			return false;
		}
		OpenAccountEvent other = (OpenAccountEvent)obj;
		return accountId.equals(other.accountId)
				&& email.equals(other.email);
	}

	@Override
	public int hashCode() {
		return accountId.hashCode() * 37 + email.hashCode();
	}

	@Override
	public String toString() {
		return "OpenAccountEvent [accountId=" + accountId + ", email=" + email + "]";
	}


	public static class SerializableEventV1 {
		private Long accountId;
		private String email;

		public SerializableEventV1() {
			// for eventStore
		}

		public SerializableEventV1(OpenAccountEvent event) {
			this.accountId = event.getAccountId();
			this.email = event.getEmail();
		}

		public Long getAccountId() {
			return accountId;
		}

		public void setAccountId(Long accountId) {
			this.accountId = accountId;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public OpenAccountEvent asEvent() {
			return new OpenAccountEvent(accountId, email);
		}
	}

}
