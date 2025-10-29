package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import org.springframework.transaction.annotation.Transactional;

public interface BeerOrderService {

    @Transactional
    BeerOrderResponse create(BeerOrderCreateRequest request);

    @Transactional(readOnly = true)
    BeerOrderResponse getById(Integer id);
}
