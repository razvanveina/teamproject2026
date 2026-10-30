package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ReceivingOrderLine {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "article_id")
	private Article article;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private int receivedQuantity;

	@ManyToOne
	@JoinColumn(name = "order_id")
	private ReceivingOrder order;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ReceivingOrderLineStatus status;

	public ReceivingOrderLine() {
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

	public ReceivingOrder getOrder() {
		return order;
	}

	public void setOrder(ReceivingOrder order) {
		this.order = order;
	}

	public ReceivingOrderLineStatus getStatus() {
		return status;
	}

	public void setStatus(ReceivingOrderLineStatus status) {
		this.status = status;
	}
}
