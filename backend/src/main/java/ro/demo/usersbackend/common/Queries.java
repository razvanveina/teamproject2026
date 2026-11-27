package ro.demo.usersbackend.common;

public class Queries {
	public static final String FIND_FREE_PALLET_IDS_IN_DECANTING = "LoadUnit.findFreePalletIdsInDecanting";
	public static final String FIND_RESERVED_IDS_IN_STORAGE = "LoadUnit.findReservedIdsInStorage";
	public static final String FIND_FREE_STORAGE_LOCATIONS = "StorageLocation.findFreeStorageLocations";
	public static final String FIND_FREE_BY_ARTICLE = "StockItem.findFreeByArticle";
	public static final String CLEAN_WORKING_DATA = "Database.cleanWorkingData";
	public static final String COUNT_AUTOMATIC_BY_LOAD_UNIT_BARCODE = "StockItem.countAutomaticByLoadUnitBarcode";
	public static final String COUNT_OPEN_TO_TARGET_FOR_ORDER = "TransportOrder.countOpenToTargetForOrder";
	public static final String RETRIEVAL_ORDER_FIND_AUTOMATIC_BY_STATUS_AND_AREA = "RetrievalOrder.findAutomaticByStatusAndArea";
	public static final String FIND_FREE_PICKING_LOCATIONS = "StorageLocation.findFreePickingLocations";
}
