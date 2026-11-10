package ro.demo.usersbackend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.LoadUnitResponse;
import ro.demo.usersbackend.service.LoadUnitService;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
	public static final Logger log = LogManager.getLogger("application");

	private final LoadUnitService loadUnitService;

	public ShippingController(LoadUnitService loadUnitService) {
		super();
		this.loadUnitService = loadUnitService;
	}

	@GetMapping("/{barcode}")
	public LoadUnitResponse findByOrderId(@PathVariable String barcode) {
		log.info("Shipping: scaned load unit barcode {}", barcode);

		try { // TODO check if order FINISHED
			LoadUnitResponse response = loadUnitService.findByBarcode(barcode);
			System.out.println(response);
			return response;
		} catch (ResponseStatusException e) {
			log.error("Shipping: scan error: ", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Shipping: exception: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/{barcode}/finish")
	public void confirmLoadUnit(@PathVariable String barcode) {
		log.info("Shipping: confirmed load unit barcode {}", barcode);

		try {
			loadUnitService.finishLoadUnit(barcode);
		} catch (ResponseStatusException e) {
			log.error("Shipping: scan error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Shipping: exception: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
