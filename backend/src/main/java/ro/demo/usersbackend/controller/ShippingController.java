package ro.demo.usersbackend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.LoadUnitResponse;
import ro.demo.usersbackend.server.mfs.StationControl;
import ro.demo.usersbackend.service.LoadUnitService;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
	public static final Logger log = LogManager.getLogger("application");

	private static final Object SHIPPING_LOCK = new Object();

	private final LoadUnitService loadUnitService;

	public ShippingController(LoadUnitService loadUnitService) {
		super();
		this.loadUnitService = loadUnitService;
	}

	@GetMapping("/{barcode}")
	public LoadUnitResponse findByOrderId(@PathVariable String barcode, Authentication authentication) {
		log.info("Shipping: {} scanned load unit {}", authentication.getName(), barcode);
		try {
			synchronized (SHIPPING_LOCK) {
				return loadUnitService.findByBarcode(barcode, authentication.getName());
			}
		} catch (ResponseStatusException e) {
			log.warn("Shipping: scan rejected {}", e.getReason());
			throw e;
		} catch (Exception e) {
			log.error("Shipping: exception: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/{barcode}/finish")
	public void confirmLoadUnit(@PathVariable String barcode, @RequestParam(required = false) String carton,
			Authentication authentication) {
		log.info("Shipping: {} confirmed load unit {}", authentication.getName(), barcode);
		boolean releaseOut = false;
		try {
			synchronized (SHIPPING_LOCK) {
				releaseOut = loadUnitService.finishLoadUnit(barcode, authentication.getName(), carton);
			}
		} catch (ResponseStatusException e) {
			log.warn("Shipping: confirm rejected {}", e.getReason());
			throw e;
		} catch (Exception e) {
			log.error("Shipping: exception: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}

		if (releaseOut) {
			try {
				StationControl.release("OUT");
				log.info("Shipping: tote {} released from OUT", barcode);
			} catch (Exception e) {
				log.error("Shipping: tote {} not released from OUT {}", barcode, e.toString());
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Order shipped, but tote " + barcode + " could not be released from OUT: " + e.getMessage());
			}
		}
	}

	@PutMapping("/{barcode}/release")
	public void releaseLoadUnit(@PathVariable String barcode, Authentication authentication) {
		log.info("Shipping: {} released load unit {}", authentication.getName(), barcode);
		synchronized (SHIPPING_LOCK) {
			loadUnitService.releaseLoadUnit(barcode, authentication.getName());
		}
	}

	@PostMapping("/{tote}/carton/{carton}")
	public LoadUnitResponse chooseCarton(@PathVariable String tote, @PathVariable String carton,
			Authentication authentication) {
		log.info("Shipping: {} chose carton {} for tote {}", authentication.getName(), carton, tote);
		try {
			synchronized (SHIPPING_LOCK) {
				return loadUnitService.chooseCarton(tote, carton, authentication.getName());
			}
		} catch (ResponseStatusException e) {
			log.warn("Shipping: carton rejected {}", e.getReason());
			throw e;
		}
	}
}