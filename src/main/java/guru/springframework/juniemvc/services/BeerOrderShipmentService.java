package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BeerOrderShipmentService {

    BeerOrderShipmentResponse create(Integer beerOrderId, BeerOrderShipmentRequest request);

    BeerOrderShipmentResponse getById(Integer beerOrderId, Integer id);

    Page<BeerOrderShipmentResponse> list(Integer beerOrderId, Pageable pageable);

    BeerOrderShipmentResponse update(Integer beerOrderId, Integer id, BeerOrderShipmentRequest request);

    void delete(Integer beerOrderId, Integer id);
}
