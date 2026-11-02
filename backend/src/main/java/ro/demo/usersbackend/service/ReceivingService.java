package ro.demo.usersbackend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.repository.ReceivingRepository;

@Service
@Transactional
public class ReceivingService {

	private final ReceivingRepository receivingRepository;

	public ReceivingService(ReceivingRepository receivingRepository) {
		this.receivingRepository = receivingRepository;
	}

	@Transactional(readOnly = true)
	public ReceivingOrder findByOrderId(String orderId) {
		return receivingRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Cannot find receiving order with the id: " + orderId));
	}// TODO add logs
}
