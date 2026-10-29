package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "stock_item_id")
	StockItem stockItem;

	@ManyToOne
	@JoinColumn(name = "retrieval_order_line_id")
	RetrievalOrderLine retrievalOrderLine;

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
