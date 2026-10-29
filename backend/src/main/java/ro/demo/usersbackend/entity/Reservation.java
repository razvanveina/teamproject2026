package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "stockItem_id")
	private StockItem stockItem;

	@ManyToOne
	@JoinColumn(name = "retrievalOrderLine_id")
	private RetrievalOrderLine retrievalOrderLine;

	public Reservation() {
		super();
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
