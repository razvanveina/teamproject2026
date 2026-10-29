package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class RetrievalOrderLine {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "article_id")
	private Article article;

	@Column(nullable = false)
	private int quantity;

	@ManyToOne
	@JoinColumn(name = "order_id")
	private RetrievalOrder order;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RetrievalOrderLineStatus status;

	@Column(nullable = false)
	@OneToMany(mappedBy = "retrievalOrderLine")
	private List<Reservation> reservations;

	public RetrievalOrderLine() {
		super();
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public RetrievalOrder getOrder() {
		return order;
	}

	public void setOrder(RetrievalOrder order) {
		this.order = order;
	}

	public RetrievalOrderLineStatus getStatus() {
		return status;
	}

	public void setStatus(RetrievalOrderLineStatus status) {
		this.status = status;
	}

	public List<Reservation> getReservations() {
		return reservations;
	}

	public void setReservations(List<Reservation> reservations) {
		this.reservations = reservations;
	}
}
