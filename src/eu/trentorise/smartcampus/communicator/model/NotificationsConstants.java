package eu.trentorise.smartcampus.communicator.model;

public class NotificationsConstants {
	public enum ORDERING {
		ORDER_BY_ARRIVAL("arrival"), ORDER_BY_REL_TIME("relevance now"), ORDER_BY_REL_PLACE("relevance here"), ORDER_BY_PRIORITY(
				"priority"), ORDER_BY_TITLE("title");

		public String text;

		private ORDERING(String text) {
			this.text = text;
		}
	}

	public static final int DEF_SYNC_PERIOD = 5;
}
