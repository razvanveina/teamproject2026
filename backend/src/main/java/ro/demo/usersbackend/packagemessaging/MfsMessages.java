package ro.demo.usersbackend.packagemessaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class MfsMessages {
	// server -> simulator
	public static final String REQUEST_QUEUE = "MFS_REQUEST";
	// simulator -> server
	public static final String RESPONSE_QUEUE = "MFS_RESPONSE";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private MfsMessages() {
	}

	// de ex {"type":"GET","transportOrderId":1,"loadUnit":"00001","location":"HBW010010101"}
	public record Get(String type, Long transportOrderId, String loadUnit, String location) {
	}

	// de ex {"type":"ACK_GET","transportOrderId":1,"loadUnit":"00001"}
	public record AckGet(String type, Long transportOrderId, String loadUnit) {
	}
	
	// de ex {"type":"ARRIVED","point":"IP1","loadUnit":"00001"}
	public record Arrived(String type, String point, String loadUnit) {
	}

	public static String toJson(Object message) {
		try {
			return MAPPER.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("cannot write the message: " + e.getMessage(), e);
		}
	}

	public static <T> T fromJson(String json, Class<T> type) {
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("cannot read the message " + json + ": " + e.getMessage(), e);
		}
	}
}