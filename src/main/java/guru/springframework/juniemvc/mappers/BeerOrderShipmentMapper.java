package guru.springframework.juniemvc.mappers;

import guru.springframework.juniemvc.entities.BeerOrderShipment;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BeerOrderShipmentMapper {

    BeerOrderShipment toEntity(BeerOrderShipmentRequest dto);

    BeerOrderShipmentResponse toResponse(BeerOrderShipment entity);

    void update(@MappingTarget BeerOrderShipment target, BeerOrderShipmentRequest source);
}
