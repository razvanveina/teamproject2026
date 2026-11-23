package ro.demo.usersbackend.common;

import java.awt.Color;
import java.util.List;

import ro.demo.usersbackend.entity.TransportOrderStatus;

public final class Constants {

	private Constants() {
	}

	// transit locations
	public static final String IN = "IN";
	public static final String OUT = "OUT";
	public static final String DECANTING = "DECANTING";
	public static final String CONVEYOR = "CONVEYOR";
	public static final String PICKING = "PICKING";
	public static final String EMPTY_TOTES = "EMPTY_TOTES";

	// punctele de pe conveyor
	public static final String TPS1 = "TPS1";
	public static final String TPT1 = "TPT1";
	public static final String IP1 = "IP1";
	public static final String IP2 = "IP2";
	public static final String DP1 = "DP1";
	public static final String DP2 = "DP2";
	public static final String DP3 = "DP3";
	public static final String EMPTY = "EMPTY";
	public static final String PTTS1 = "PTTS1";
	public static final String PTTT1 = "PTTT1";
	public static final String TPS2 = "TPS2"; // intrarea pe banda de la decanting
	public static final String IP3 = "IP3"; // pe C03, anunta ca tote-ul de la decanting a ajuns pe banda

	public static final String IP4 = "IP4";
	public static final String IP5 = "IP5";
	public static final String IP6 = "IP6";
	public static final String IP7 = "IP7";
	public static final String IP8 = "IP8";
	public static final String DP4 = "DP4";
	public static final String DP5 = "DP5";
	public static final String DP6 = "DP6";
	public static final String DP7 = "DP7";
	public static final String DP8 = "DP8";
	public static final String PTTS2 = "PTTS2";
	public static final String PTTS3 = "PTTS3";
	public static final String PTTT2 = "PTTT2";
	public static final String PTTT3 = "PTTT3";

	public static final List<String> PTTS_POINTS = List.of(PTTS1, PTTS2, PTTS3);
	public static final List<String> PTTT_POINTS = List.of(PTTT1, PTTT2, PTTT3);

	public static final List<String> CONVEYOR_POINTS = List.of(TPS1, TPS2, TPT1, IP1, IP2, IP3, IP4, IP5, IP6, IP7, IP8,
			DP1, DP2, DP3, DP4, DP5, DP6, DP7, DP8, EMPTY, PTTS1, PTTS2, PTTS3, PTTT1, PTTT2, PTTT3);

	// location names
	public static final String PICK_PREFIX = "PICK";
	public static final String PICK_NAME = "PICK%03d%02d"; // x, y
	public static final String HBW_NAME = "HBW%02d%03d%02d%02d"; // aisle, x, y, z
	public static final String GATE_NAME = "GATE%02d";

	// to care nu sunt finalizate (new sau active)
	public static final List<TransportOrderStatus> OPEN_TRANSPORT_STATUSES = List.of(TransportOrderStatus.NEW,
			TransportOrderStatus.ACTIVE);

	// barcode-uri
	public static final String TOTE_SEQUENCE = "TOTE_SEQUENCE";
	public static final String TOTE_BARCODE = "%06d";
	public static final String LOAD_UNIT_SEQUENCE = "LOADUNIT_SEQUENCE";
	public static final String LOAD_UNIT_BARCODE = "%05d";

	// mesaje dintre sv si sim
	public static final String RELEASE = "release";
	public static final String GET_ENTITIES = "getEntities";
	public static final String CREATE_LU = "createLU";

	// timings
	public static final long PROCESS_INTERVAL = 10000;
	public static final long DECISION_TIMEOUT = 30000;
	public static final long INFO_POINT_DWELL = 1000; // cat sta un tote pe un info point
	public static final long TPS1_ACK_DELAY = 2000; // TPS1 confirma GET dupa asta
	public static final long ACK_GET_WAIT = 1000; // cat asteapta sv pentru urmatorul reply
	public static final long GATE_DWELL = 2000; // cat sta un carton pe gate inainte sa iasa din sim

	// load unit types din sim
	public static final String TOTE_TYPE = "TOTE";
	public static final String CARTON_TYPE = "CARTON";

	// sim window
	public static final Color TOTE_COLOR = new Color(120, 165, 215);
	public static final Color CARTON_COLOR = new Color(185, 140, 100);
	public static final String PICKING_ZONE_LABEL = "Picking Zone";
	public static final String DECANTING_ZONE_LABEL = "Decanting Zone";
	public static final String ADD_LOAD_UNIT = "Add Load Unit";

	// printing
	public static final String PDF_OUTPUT_FOLDER = "prints";
	public static final String PREFIX = "Shipping_Document_";
	public static final String STATUS_MISSING = "MISSING";
	public static final String PDF_EXTENSION = ".pdf";
	public static final String SHIPPING_DOCUMENT_TITLE = "Shipping Document";
	public static final String ORDER_ID_TITLE = "Order ID: ";
	public static final String DESTINATION_TITLE = "Destination: ";
	public static final String ARTICLE_HEADER = "Article";
	public static final String QUANTITY_HEADER = "Quantity";

	// campurile din json-ul etichetei
	public static final String ORDER_ID = "orderId";
	public static final String DESTINATION = "destination";
	public static final String LINES = "lines";
	public static final String ARTICLE = "article";
	public static final String QUANTITY = "quantity";
}