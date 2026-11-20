package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "Reservation.findByStockItemIn", //
				query = "from Reservation r where r.stockItem in :stockItems") //
})
public class Reservation {
	public static final String BY_STOCK_ITEMS = "Reservation.findByStockItemIn";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "stockItem_id", nullable = false)
	private StockItem stockItem;

	@ManyToOne
	@JoinColumn(name = "retrievalOrderLine_id", nullable = false)
	private RetrievalOrderLine retrievalOrderLine;

	public Reservation() {
		super();
	}

	public Reservation(StockItem stockItem, RetrievalOrderLine retrievalOrderLine) {
		super();
		this.stockItem = stockItem;
		this.retrievalOrderLine = retrievalOrderLine;
	}

	public StockItem getStockItem() {
		return stockItem;
	}

	public void setStockItem(StockItem stockItem) {
		this.stockItem = stockItem;
	}

	public RetrievalOrderLine getRetrievalOrderLine() {
		return retrievalOrderLine;
	}

	public void setRetrievalOrderLine(RetrievalOrderLine retrievalOrderLine) {
		this.retrievalOrderLine = retrievalOrderLine;
	}
}
