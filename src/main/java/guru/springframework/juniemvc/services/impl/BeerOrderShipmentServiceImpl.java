package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderShipment;
import guru.springframework.juniemvc.mappers.BeerOrderShipmentMapper;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import guru.springframework.juniemvc.repositories.BeerOrderRepository;
import guru.springframework.juniemvc.repositories.BeerOrderShipmentRepository;
import guru.springframework.juniemvc.services.BeerOrderShipmentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BeerOrderShipmentServiceImpl implements BeerOrderShipmentService {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderShipmentRepository beerOrderShipmentRepository;
    private final BeerOrderShipmentMapper mapper;

    BeerOrderShipmentServiceImpl(BeerOrderRepository beerOrderRepository,
                                 BeerOrderShipmentRepository beerOrderShipmentRepository,
                                 BeerOrderShipmentMapper mapper) {
        this.beerOrderRepository = beerOrderRepository;
        this.beerOrderShipmentRepository = beerOrderShipmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public BeerOrderShipmentResponse create(Integer beerOrderId, BeerOrderShipmentRequest request) {
        BeerOrder parent = beerOrderRepository.findById(beerOrderId)
                .orElseThrow(() -> new EntityNotFoundException("BeerOrder not found: " + beerOrderId));
        BeerOrderShipment entity = mapper.toEntity(request);
        entity.setBeerOrder(parent);
        BeerOrderShipment saved = beerOrderShipmentRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BeerOrderShipmentResponse getById(Integer beerOrderId, Integer id) {
        BeerOrderShipment entity = beerOrderShipmentRepository.findByIdAndBeerOrderId(id, beerOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Shipment not found: " + id + " for order: " + beerOrderId));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BeerOrderShipmentResponse> list(Integer beerOrderId, Pageable pageable) {
        return beerOrderShipmentRepository.findAllByBeerOrderId(beerOrderId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public BeerOrderShipmentResponse update(Integer beerOrderId, Integer id, BeerOrderShipmentRequest request) {
        BeerOrderShipment entity = beerOrderShipmentRepository.findByIdAndBeerOrderId(id, beerOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Shipment not found: " + id + " for order: " + beerOrderId));
        mapper.update(entity, request);
        BeerOrderShipment saved = beerOrderShipmentRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer beerOrderId, Integer id) {
        BeerOrderShipment entity = beerOrderShipmentRepository.findByIdAndBeerOrderId(id, beerOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Shipment not found: " + id + " for order: " + beerOrderId));
        beerOrderShipmentRepository.delete(entity);
    }
}
