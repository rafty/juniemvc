package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderLine;
import guru.springframework.juniemvc.entities.LineStatus;
import guru.springframework.juniemvc.exceptions.BeerNotFoundException;
import guru.springframework.juniemvc.exceptions.InvalidOrderException;
import guru.springframework.juniemvc.mappers.BeerOrderMapper;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderLineCreateItem;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import guru.springframework.juniemvc.repositories.BeerOrderLineRepository;
import guru.springframework.juniemvc.repositories.BeerOrderRepository;
import guru.springframework.juniemvc.repositories.BeerRepository;
import guru.springframework.juniemvc.services.BeerOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
class BeerOrderServiceImpl implements BeerOrderService {

    private final BeerRepository beerRepository;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderLineRepository beerOrderLineRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public BeerOrderResponse create(BeerOrderCreateRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one line");
        }

        BeerOrder order = new BeerOrder();
        order.setCustomerRef(request.customerRef());
        order.setPaymentAmount(request.paymentAmount());

        for (BeerOrderLineCreateItem item : request.lines()) {
            Objects.requireNonNull(item, "line item must not be null");
            Integer beerId = item.beerId();
            Beer beer = beerRepository.findById(beerId)
                    .orElseThrow(() -> new BeerNotFoundException(beerId));

            BeerOrderLine line = new BeerOrderLine();
            line.setBeer(beer);
            line.setOrderQuantity(item.orderQuantity());
            line.setQuantityAllocated(0);
            line.setStatus(LineStatus.NEW);

            order.addLine(line);
        }

        BeerOrder saved = beerOrderRepository.save(order);
        if (log.isInfoEnabled()) {
            log.info("BeerOrder created id={}", saved.getId());
        }
        return beerOrderMapper.toResponse(saved);
    }

    @Override
    public BeerOrderResponse getById(Integer id) {
        BeerOrder found = beerOrderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new InvalidOrderException("Order not found: id=" + id));
        return beerOrderMapper.toResponse(found);
    }
}
