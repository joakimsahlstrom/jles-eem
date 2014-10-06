package se.jsa.jles.eh.events;

public class ChangeAccountNameEvent {
	private final Long accountId;
	private final String newAccountName;

	public ChangeAccountNameEvent(Long accountId, String newAccountName) {
		this.accountId = accountId;
		this.newAccountName = newAccountName;
	}

	public Long getAccountId() {
		return accountId;
	}

	public String getNewAccountName() {
		return newAccountName;
	}

	public SerializableV1 asSerializable() {
		return new SerializableV1(this);
	}

	@Override
	public String toString() {
		return "ChangeAccountNameEvent [accountId=" + accountId
				+ ", newAccountName=" + newAccountName + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ChangeAccountNameEvent)) {
			return false;
		}
		ChangeAccountNameEvent other = (ChangeAccountNameEvent)obj;
		return accountId.equals(other.accountId)
				&& newAccountName.equals(other.newAccountName);
	}

	@Override
	public int hashCode() {
		return accountId.hashCode() * 37 + newAccountName.hashCode();
	}

	public static class SerializableV1 {
		private Long accountId;
		private String newAccountName;

		public SerializableV1() {
		}

		public SerializableV1(ChangeAccountNameEvent event) {
			this.accountId = event.getAccountId();
			this.newAccountName = event.getNewAccountName();
		}

		public Long getAccountId() {
			return accountId;
		}
		public void setAccountId(Long accountId) {
			this.accountId = accountId;
		}
		public String getNewAccountName() {
			return newAccountName;
		}
		public void setNewAccountName(String newAccountName) {
			this.newAccountName = newAccountName;
		}

		public ChangeAccountNameEvent asEvent() {
			return new ChangeAccountNameEvent(accountId, newAccountName);
		}

	}

}
